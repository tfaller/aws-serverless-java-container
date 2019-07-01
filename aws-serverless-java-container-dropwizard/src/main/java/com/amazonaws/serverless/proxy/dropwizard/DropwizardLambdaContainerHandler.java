package com.amazonaws.serverless.proxy.dropwizard;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.*;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.servlet.*;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.jersey.jackson.JacksonFeature;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Generics;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.io.File;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;

/**
 * Dropwizard specific implementation of the Lambda Container Handler. While Dropwizard uses Jersey it also wraps it in
 * a servlet. This classes leverages the <code>JerseyServletContainer</code> provided by the Dropwizard environment.
 *
 * <pre>
 *  {@code
 *    public class LambdaHandler implements RequestHandler<AwsProxyRequest, AwsProxyResponse> {
 *      private DropwizardLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse, TestConfiguration> handler;
 *      public LambdaHandler() {
 *        try {
 *          handler = DropwizardLambdaContainerHandler.getAwsProxyHandler(
 *             new TestApplication(),
 *             "test-config.yml"
 *          );
 *        } catch (ContainerInitializationException e) {
 *          e.printStackTrace();
 *          System.exit(1);
 *        }
 *      }
 *
 *      public AwsProxyResponse handleRequest(AwsProxyRequest awsProxyRequest, Context context) {
 *        return handler.proxy(awsProxyRequest, context);
 *      }
 *    }
 *  }
 * </pre>
 *
 * @param <RequestType> The type for the incoming Lambda event
 * @param <ResponseType> The type declared as the Lambda output
 * @param <DropwizardConfig> The implementation of Dropwizard's <code>Configuration</code> interface
 */
public class DropwizardLambdaContainerHandler<RequestType, ResponseType, DropwizardConfig extends Configuration>
        extends AwsLambdaServletContainerHandler<RequestType, ResponseType, AwsProxyHttpServletRequest, AwsHttpServletResponse> {

    private Application<DropwizardConfig> dropwizardApp;
    private Bootstrap<DropwizardConfig> dropwizardBootstrap;
    private DropwizardConfig dropwizardConfig;
    private Environment dropwizardEnvironment;
    private String configFileName;

    /**
     * Creates a new instance of the <code>DropwizardLambdaContainerHandler</code> for the given application. It uses the
     * default configuration factory to generate an empty config
     * @param dwApp The Dropwizard Application object
     * @param <ConfigType> The type for the Dropwizard configuration class
     * @return An initialized <code>DropwizardLambdaContainerHandler</code>
     * @throws ContainerInitializationException If the Application class could not be started with Dropwizard's bootstrap process
     */
    public static <ConfigType extends Configuration> DropwizardLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse, ConfigType> getAwsProxyHandler(Application<ConfigType> dwApp) throws ContainerInitializationException {
        return new DropwizardLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse, ConfigType>(
                AwsProxyRequest.class,
                AwsProxyResponse.class,
                new AwsProxyHttpServletRequestReader(),
                new AwsProxyHttpServletResponseWriter(),
                new AwsProxySecurityContextWriter(),
                new AwsProxyExceptionHandler(),
                dwApp
        );
    }

    /**
     * Creates a new instance of the <code>DropwizardLambdaContainerHandler</code> for the given application. The configuration
     * is loaded by the given file name. The library expects the configuration yaml file to be located in the resources
     * folder.
     * @param dwApp The Dropwizard Application object
     * @param configFile The name of the configuration file as its relative path in the resources folder
     * @param <ConfigType> The type for the Dropwizard configuration class
     * @return An initialized <code>DropwizardLambdaContainerHandler</code>
     * @throws ContainerInitializationException If the Application class could not be started with Dropwizard's bootstrap process
     */
    public static <ConfigType extends Configuration> DropwizardLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse, ConfigType> getAwsProxyHandler(Application<ConfigType> dwApp, String configFile) throws ContainerInitializationException {
        return new DropwizardLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse, ConfigType>(
                AwsProxyRequest.class,
                AwsProxyResponse.class,
                new AwsProxyHttpServletRequestReader(),
                new AwsProxyHttpServletResponseWriter(),
                new AwsProxySecurityContextWriter(),
                new AwsProxyExceptionHandler(),
                dwApp,
                configFile
        );
    }

    /**
     * Creates a new instance of <code>DropwizardLambdaContainerHandler</code>.
     *
     * @param requestTypeClass The type for the incoming request event
     * @param responseTypeClass The type for the expected response object
     * @param requestReader An implementation of the <code>RequestReader</code> abstract class
     * @param responseWriter An implementation of the <code>ResponseWriter</code> abstract class
     * @param securityContextWriter An implementation of the <code>SecurityContextWriter</code> object
     * @param exceptionHandler An exception handler
     * @param dwApp The Dropwizard application object
     * @throws ContainerInitializationException If the Application class could not be started with Dropwizard's bootstrap process
     */
    public DropwizardLambdaContainerHandler(Class<RequestType> requestTypeClass,
                                            Class<ResponseType> responseTypeClass,
                                            RequestReader<RequestType, AwsProxyHttpServletRequest> requestReader,
                                            ResponseWriter<AwsHttpServletResponse, ResponseType> responseWriter,
                                            SecurityContextWriter<RequestType> securityContextWriter,
                                            ExceptionHandler<ResponseType> exceptionHandler,
                                            Application<DropwizardConfig> dwApp) throws ContainerInitializationException {
        this(requestTypeClass, responseTypeClass, requestReader, responseWriter, securityContextWriter, exceptionHandler, dwApp, null);
    }

    /**
     * Creates a new instance of <code>DropwizardLambdaContainerHandler</code>.
     *
     * @param requestTypeClass The type for the incoming request event
     * @param responseTypeClass The type for the expected response object
     * @param requestReader An implementation of the <code>RequestReader</code> abstract class
     * @param responseWriter An implementation of the <code>ResponseWriter</code> abstract class
     * @param securityContextWriter An implementation of the <code>SecurityContextWriter</code> object
     * @param exceptionHandler An exception handler
     * @param dwApp The Dropwizard application object
     * @param configFile The relative path to the Dropwizard yaml config file from the resources folder
     * @throws ContainerInitializationException If the Application class could not be started with Dropwizard's bootstrap process
     */
    public DropwizardLambdaContainerHandler(Class<RequestType> requestTypeClass,
                                            Class<ResponseType> responseTypeClass,
                                            RequestReader<RequestType, AwsProxyHttpServletRequest> requestReader,
                                            ResponseWriter<AwsHttpServletResponse, ResponseType> responseWriter,
                                            SecurityContextWriter<RequestType> securityContextWriter,
                                            ExceptionHandler<ResponseType> exceptionHandler,
                                            Application<DropwizardConfig> dwApp,
                                            String configFile) throws ContainerInitializationException {
        super(requestTypeClass, responseTypeClass, requestReader, responseWriter, securityContextWriter, exceptionHandler);
        dropwizardApp = dwApp;
        dropwizardBootstrap = new Bootstrap<>(dwApp);
        configFileName = configFile;

        // THIS INITIALIZATION PROCESS IS SIMILAR TO WHAT THE DROPWIZARD CLI DOES TO START THE SERVER

        // initilize a Dropwizard environment
        dropwizardEnvironment = new Environment(
                dropwizardBootstrap.getApplication().getName(),
                dropwizardBootstrap.getObjectMapper(),
                dropwizardBootstrap.getValidatorFactory(),
                dropwizardBootstrap.getMetricRegistry(),
                dropwizardBootstrap.getClassLoader(),
                dropwizardBootstrap.getHealthCheckRegistry()
        );

        // attempt to load the configuration file or generated an empty one if no file was passed
        try {
            ConfigurationFactory<DropwizardConfig> configurationFactory = dropwizardBootstrap.getConfigurationFactoryFactory()
                    .create(
                            Generics.getTypeParameter(dropwizardApp.getClass(), Configuration.class),
                            dropwizardBootstrap.getValidatorFactory().getValidator(),
                            dropwizardBootstrap.getObjectMapper(),
                            "dw"
                    );
            if (configFileName == null) {
                dropwizardConfig = configurationFactory.build();
            } else {
                dropwizardConfig = configurationFactory.build(new File(getClass().getClassLoader().getResource(configFileName).getFile()));
            }

            // start the bootstrap and app processes with the configuration and environment
            dropwizardBootstrap.run(dropwizardConfig, dropwizardEnvironment);
            dropwizardApp.run(dropwizardConfig, dropwizardEnvironment);

            // set the metrics registry - this is not used right now
            dropwizardConfig.getMetricsFactory().configure(dropwizardEnvironment.lifecycle(), dropwizardBootstrap.getMetricRegistry());
            // force register Jackson, for some reason it worked in the unit tests but was not picking it up in Lambda itself.
            dropwizardEnvironment.jersey().register(new JacksonFeature(LambdaContainerHandler.getObjectMapper()));
            // initialize the jersey servlet container with a custom ServletConfig object
            dropwizardEnvironment.getJerseyServletContainer().init(new ServletConfig() {
                @Override
                public String getServletName() {
                    return "dropwizard-default-servlet";
                }

                @Override
                public ServletContext getServletContext() {
                    return DropwizardLambdaContainerHandler.super.getServletContext();
                }

                @Override
                public String getInitParameter(String s) {
                    return null;
                }

                @Override
                public Enumeration<String> getInitParameterNames() {
                    return Collections.emptyEnumeration();
                }
            });
        } catch (Exception e) {
            throw new ContainerInitializationException("Could not run dropwizard application object", e);
        }
    }

    @Override
    protected AwsHttpServletResponse getContainerResponse(AwsProxyHttpServletRequest request, CountDownLatch latch) {
        return new AwsHttpServletResponse(request, latch);
    }

    @Override
    protected void handleRequest(AwsProxyHttpServletRequest containerRequest, AwsHttpServletResponse containerResponse, Context lambdaContext) throws Exception {
        if (dropwizardEnvironment.getJerseyServletContainer() == null) {
            System.out.println("Null container");
        }
        containerRequest.setServletContext(getServletContext());
        dropwizardEnvironment.getJerseyServletContainer().service(containerRequest, containerResponse);
        if (!containerResponse.isCommitted()) {
            containerResponse.flushBuffer();
        }
    }

    @Override
    public void initialize() {

    }
}

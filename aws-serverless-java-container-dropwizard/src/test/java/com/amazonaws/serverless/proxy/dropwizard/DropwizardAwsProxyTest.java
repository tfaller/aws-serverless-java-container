package com.amazonaws.serverless.proxy.dropwizard;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.dropwizard.echoapp.DwTestApplication;
import com.amazonaws.serverless.proxy.dropwizard.echoapp.DwTestConfiguration;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.HttpHeaders;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class DropwizardAwsProxyTest {
    private static DropwizardLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse, DwTestConfiguration>  handler;
    private static Context lambdaCtx = new MockLambdaContext();

    private boolean isAlb;

    public DropwizardAwsProxyTest(boolean alb) throws ContainerInitializationException {
        isAlb = alb;
        handler = DropwizardLambdaContainerHandler.getAwsProxyHandler(
                new DwTestApplication(), "test-config.yml"
        );
    }

    @Parameterized.Parameters
    public static Collection<Object> data() {
        return Arrays.asList(new Object[] { false, true });
    }

    private AwsProxyRequestBuilder getRequestBuilder(String path, String method) {
        AwsProxyRequestBuilder builder = new AwsProxyRequestBuilder(path, method);
        if (isAlb) builder.alb();

        return builder;
    }

    @Test
    public void helloWorld_defaultName_returnsCorrectString() throws ContainerInitializationException {
        AwsProxyRequest req = getRequestBuilder("/echo/hello", "GET").header(HttpHeaders.ACCEPT, "application/json").build();
        AwsProxyResponse resp = handler.proxy(req, lambdaCtx);
        assertEquals("{\"message\":\"Hello, Stranger!\"}", resp.getBody());
    }
}

package com.amazonaws.serverless.proxy.dropwizard.echoapp;

import com.amazonaws.serverless.proxy.dropwizard.echoapp.api.EchoResource;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class DwTestApplication extends Application<DwTestConfiguration> {

    public static void main(final String[] args) throws Exception {
        new DwTestApplication().run(args);
    }

    @Override
    protected void bootstrapLogging() {
    }

    @Override
    public String getName() {
        return "DwSample";
    }

    @Override
    public void initialize(final Bootstrap<DwTestConfiguration> bootstrap) {
        // TODO: application initialization
    }

    @Override
    public void run(final DwTestConfiguration configuration,
                    final Environment environment) {
        final EchoResource resource = new EchoResource(
                configuration.getTemplate(),
                configuration.getDefaultName()
        );
        environment.jersey().register(resource);
    }

}

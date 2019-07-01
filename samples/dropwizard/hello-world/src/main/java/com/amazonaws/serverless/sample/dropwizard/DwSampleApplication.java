package com.amazonaws.serverless.sample.dropwizard;

import com.amazonaws.serverless.sample.dropwizard.api.HelloWorldResource;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class DwSampleApplication extends Application<DwSampleConfiguration> {

    public static void main(final String[] args) throws Exception {
        new DwSampleApplication().run(args);
    }

    @Override
    public String getName() {
        return "DwSample";
    }

    @Override
    public void initialize(final Bootstrap<DwSampleConfiguration> bootstrap) {
        // TODO: application initialization
    }

    @Override
    public void run(final DwSampleConfiguration configuration,
                    final Environment environment) {
        final HelloWorldResource resource = new HelloWorldResource(
                configuration.getTemplate(),
                configuration.getDefaultName()
        );
        environment.jersey().register(resource);
    }

}

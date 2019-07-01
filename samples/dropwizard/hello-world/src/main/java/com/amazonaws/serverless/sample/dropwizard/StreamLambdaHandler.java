package com.amazonaws.serverless.sample.dropwizard;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.dropwizard.DropwizardLambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.Timer;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamLambdaHandler implements RequestStreamHandler {
    private DropwizardLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse, DwSampleConfiguration> handler;

    public StreamLambdaHandler() {
        // we enable the timer for debugging. This SHOULD NOT be enabled in production.
        Timer.enable();
        try {
            handler = DropwizardLambdaContainerHandler.getAwsProxyHandler(new DwSampleApplication(), "config.yml");
        } catch (ContainerInitializationException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        handler.proxyStream(inputStream, outputStream, context);
    }
}

package com.amazonaws.serverless.proxy.dropwizard.echoapp.api;

import com.codahale.metrics.annotation.Timed;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Path("/echo")
@Produces(MediaType.APPLICATION_JSON)
public class EchoResource {
    private final String template;
    private final String defaultName;

    public EchoResource(String template, String defaultName) {
        this.template = template;
        this.defaultName = defaultName;
    }

    @Path("/hello") @GET
    @Timed
    public Response sayHello(@QueryParam("name") Optional<String> name) {
        final String value = String.format(template, name.orElse(defaultName));
        Map<String, String> output = new HashMap<>();
        output.put("message", value);
        return Response.ok(output).build();
    }
}

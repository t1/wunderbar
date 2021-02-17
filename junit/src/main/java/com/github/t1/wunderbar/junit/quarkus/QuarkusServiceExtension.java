package com.github.t1.wunderbar.junit.quarkus;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

import static javax.ws.rs.core.Response.Status.Family.OTHER;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

class QuarkusServiceExtension implements Extension, BeforeAllCallback, AfterAllCallback {
    private Process service;

    @Override public void beforeAll(ExtensionContext context) throws Exception {
        service = new ProcessBuilder().command("java", "-jar", "target/wunderbar.demo.product-runner.jar").inheritIO().start();
        waitUntilReady();
    }

    private void waitUntilReady() throws TimeoutException {
        var healthClient = RestClientBuilder.newBuilder()
            .baseUri(URI.create("http://localhost:8080"))
            .build(HealthClient.class);
        var start = Instant.now();
        Family family;
        do {
            Response status;
            try {
                status = healthClient.ready();
                family = status.getStatusInfo().getFamily();
            } catch (Exception e) {
                family = OTHER;
                continue;
            }
            if (Duration.between(start, Instant.now()).getSeconds() > 10)
                throw new TimeoutException("still getting " + status.getStatus() + ": "
                    + status.readEntity(String.class).replaceAll("\n", " "));
        } while (!family.equals(SUCCESSFUL));
    }

    @Override public void afterAll(ExtensionContext context) {
        if (service != null) service.destroy();
    }

    @Path("/q/health")
    public interface HealthClient {
        @GET @Path("/ready") Response ready();
    }
}

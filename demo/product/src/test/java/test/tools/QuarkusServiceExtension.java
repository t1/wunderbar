package test.tools;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

/** @see QuarkusService */
@Slf4j
public class QuarkusServiceExtension implements Extension, BeforeAllCallback, AfterAllCallback {
    public static final String ENDPOINT = "http://localhost:8081";
    private Process service;

    @Override public void beforeAll(ExtensionContext context) throws Exception {
        service = new ProcessBuilder().command("java", "-jar", "target/quarkus-app/quarkus-run.jar").inheritIO().start();
        waitUntilReady();
    }

    // TODO there must be a better way to wait for the readiness than a busy-waiting loop
    private void waitUntilReady() throws TimeoutException {
        var healthClient = RestClientBuilder.newBuilder()
            .baseUri(URI.create(ENDPOINT))
            .build(HealthClient.class);
        var start = Instant.now();
        Response response;
        do {
            try {
                response = healthClient.ready();
            } catch (Exception e) {
                response = Response.status(399).type(TEXT_PLAIN_TYPE).entity(e.toString()).build();
            }
            if (response.getStatusInfo().getFamily().equals(SUCCESSFUL)) return;
            log.debug("busy-wait. {}", info(response));
        } while (Duration.between(start, Instant.now()).getSeconds() < 10);
        throw new TimeoutException("while waiting for readiness. Last got: " + info(response));
    }

    private String info(Response response) {
        return response.getStatusInfo().getFamily() + "/" + response.getStatus() + "/" + response.getStatusInfo().getReasonPhrase()
               + ((response.hasEntity() ? ": " + response.readEntity(String.class) : ""));
    }

    @Override public void afterAll(ExtensionContext context) {
        if (service != null) service.destroy();
    }

    @Path("/q/health")
    public interface HealthClient {
        @Produces(APPLICATION_JSON)
        @GET @Path("/ready") Response ready();
    }
}

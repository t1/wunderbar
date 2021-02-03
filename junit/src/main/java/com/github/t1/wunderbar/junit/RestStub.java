package com.github.t1.wunderbar.junit;

import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.WebApplicationException;
import java.lang.reflect.Method;

import static io.undertow.util.Headers.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Slf4j
class RestStub extends Stub {
    private static final Jsonb JSONB = JsonbBuilder.create();

    private final HttpServer server = new HttpServer(this::handleRequest);

    RestStub(Method method, Object[] args) { super(method, args); }

    @Override Object invoke() throws Exception {
        var client = RestClientBuilder.newBuilder().baseUri(server.baseUri()).build(method.getDeclaringClass());
        return invokeOn(client);
    }

    private void handleRequest(HttpServerExchange exchange) {
        exchange.getResponseHeaders().put(CONTENT_TYPE, APPLICATION_JSON);
        if (exception == null) {
            exchange.getResponseSender().send(JSONB.toJson(response));
        } else {
            if (exception instanceof WebApplicationException)
                exchange.setStatusCode(((WebApplicationException) exception).getResponse().getStatus());
            else throw (RuntimeException) exception;
        }
    }

    @Override public void close() { server.stop(); }
}

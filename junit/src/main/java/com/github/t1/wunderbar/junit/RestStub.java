package com.github.t1.wunderbar.junit;

import com.github.t1.wunderbar.junit.http.HttpServer;
import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.WebApplicationException;
import java.lang.reflect.Method;
import java.util.Optional;

@Slf4j
class RestStub extends Stub {
    private static final Jsonb JSONB = JsonbBuilder.create();

    private final HttpServer server = new HttpServer(Bar.save(this::name, this::handleRequest));

    RestStub(Method method, Object[] args) { super(method, args); }

    @Override Object invoke() throws Exception {
        var client = RestClientBuilder.newBuilder().baseUri(server.baseUri()).build(method.getDeclaringClass());
        return invokeOn(client);
    }

    private HttpServerResponse handleRequest(HttpServerRequest request) {
        var response = HttpServerResponse.builder();
        if (exception == null) {
            response.body(Optional.of(JSONB.toJson(this.response)));
        } else {
            if (exception instanceof WebApplicationException)
                response.status(((WebApplicationException) exception).getResponse().getStatusInfo());
            else throw (RuntimeException) exception;
        }
        return response.build();
    }

    @Override public void close() { server.stop(); }
}

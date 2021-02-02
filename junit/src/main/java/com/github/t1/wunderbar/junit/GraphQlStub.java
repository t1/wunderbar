package com.github.t1.wunderbar.junit;

import io.smallrye.graphql.client.typesafe.api.GraphQlClientBuilder;
import io.undertow.server.HttpServerExchange;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static io.undertow.util.Headers.CONTENT_TYPE;
import static java.util.Locale.US;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

class GraphQlStub extends Stub {
    private static final Jsonb JSONB = JsonbBuilder.create();

    private final HttpServer server = new HttpServer(this::handleRequest);

    public GraphQlStub(Method method, Object... args) { super(method, args); }

    @Override Object invoke() throws Exception {
        var client = GraphQlClientBuilder.newBuilder().endpoint(server.baseUri()).build(method.getDeclaringClass());
        return invokeOn(client);
    }

    private void handleRequest(HttpServerExchange exchange) {
        exchange.getResponseHeaders().put(CONTENT_TYPE, APPLICATION_JSON);
        var responseBuilder = GraphQlResponse.builder();
        if (response != null)
            responseBuilder.data(Map.of("product", response)); // TODO get name from request
        if (exception != null)
            responseBuilder.error(GraphQlError.builder()
                .message(exception.getMessage())
                .extension("code", errorCode())
                .build());
        var body = JSONB.toJson(responseBuilder.build());
        exchange.getResponseSender().send(body);
    }

    private String errorCode() { return camelToKebab(exception.getClass().getSimpleName()); }

    private static String camelToKebab(String in) { return String.join("-", in.split("(?=\\p{javaUpperCase})")).toLowerCase(US); }

    @ToString @Getter @Builder
    public static class GraphQlResponse {
        Map<String, Object> data;
        @Singular List<GraphQlError> errors;
    }

    @ToString @Getter @Builder
    public static class GraphQlError {
        String message;
        @Singular Map<String, Object> extensions;
    }

    @Override public void close() { server.stop(); }
}

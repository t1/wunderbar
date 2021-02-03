package com.github.t1.wunderbar.junit;

import io.smallrye.graphql.client.typesafe.api.GraphQlClientBuilder;
import io.undertow.server.HttpServerExchange;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;
import org.eclipse.microprofile.graphql.Name;

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

    GraphQlStub(Method method, Object... args) { super(method, args); }

    @Override Object invoke() throws Exception {
        var client = GraphQlClientBuilder.newBuilder().endpoint(server.baseUri()).build(method.getDeclaringClass());
        return invokeOn(client);
    }

    private void handleRequest(HttpServerExchange exchange) {
        exchange.getResponseHeaders().put(CONTENT_TYPE, APPLICATION_JSON);
        var responseBuilder = GraphQlResponse.builder();
        if (response != null)
            responseBuilder.data(Map.of(dataName(), response));
        if (exception != null)
            responseBuilder.error(GraphQlError.builder()
                .message(exception.getMessage())
                .extension("code", errorCode())
                .build());
        var body = JSONB.toJson(responseBuilder.build());
        exchange.getResponseSender().send(body);
    }

    private String dataName() {
        if (method.isAnnotationPresent(Name.class))
            return method.getAnnotation(Name.class).value();
        String name = method.getName();
        if (isGetter(name))
            return lowerFirst(name.substring(3));
        return name;
    }

    private boolean isGetter(String name) {
        return name.startsWith("get") && name.length() > 3 && Character.isUpperCase(name.charAt(3));
    }

    private String lowerFirst(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
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

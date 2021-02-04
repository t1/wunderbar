package com.github.t1.wunderbar.junit;

import com.github.t1.wunderbar.junit.http.HttpServer;
import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientBuilder;
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
import java.util.Optional;

import static java.util.Locale.US;

class GraphQlStub extends Stub {
    private static final Jsonb JSONB = JsonbBuilder.create();

    private final HttpServer server = new HttpServer(Bar.save(this::name, this::handleRequest));

    GraphQlStub(Method method, Object... args) { super(method, args); }

    @Override Object invoke() throws Exception {
        var client = GraphQlClientBuilder.newBuilder().endpoint(server.baseUri()).build(method.getDeclaringClass());
        return invokeOn(client);
    }

    private HttpServerResponse handleRequest(HttpServerRequest request) {
        return HttpServerResponse.builder()
            .body(Optional.of(body()))
            .build();
    }

    private String body() {
        var responseBuilder = GraphQlResponseBody.builder();
        if (response != null)
            responseBuilder.data(Map.of(dataName(), response));
        if (exception != null)
            responseBuilder.error(GraphQlError.builder()
                .message(exception.getMessage())
                .extension("code", errorCode())
                .build());
        return JSONB.toJson(responseBuilder.build());
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
    public static class GraphQlResponseBody {
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

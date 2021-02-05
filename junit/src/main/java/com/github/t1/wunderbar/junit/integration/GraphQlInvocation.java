package com.github.t1.wunderbar.junit.integration;

import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientBuilder;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;
import org.eclipse.microprofile.graphql.Name;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static java.util.Locale.US;

class GraphQlInvocation extends HttpServiceInvocation {
    GraphQlInvocation(String name, Method method, Object... args) { super(name, method, args); }

    @Override protected Object service() {
        return GraphQlClientBuilder.newBuilder().endpoint(baseUri()).build(method.getDeclaringClass());
    }

    @Override protected HttpServerResponse handleRequest(HttpServerRequest request) {
        return HttpServerResponse.builder().body(buildResponseBody()).build();
    }

    private GraphQlResponseBody buildResponseBody() {
        var responseBuilder = GraphQlResponseBody.builder();
        if (getResponse() != null)
            responseBuilder.data(Map.of(dataName(), getResponse()));
        if (getException() != null)
            responseBuilder.error(GraphQlError.builder()
                .message(getException().getMessage())
                .extension("code", errorCode())
                .build());
        return responseBuilder.build();
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

    private String errorCode() { return camelToKebab(getException().getClass().getSimpleName()); }

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
}

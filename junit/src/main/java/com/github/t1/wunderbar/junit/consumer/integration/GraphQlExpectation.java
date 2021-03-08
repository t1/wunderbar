package com.github.t1.wunderbar.junit.consumer.integration;

import com.github.t1.wunderbar.junit.consumer.BarWriter;
import com.github.t1.wunderbar.junit.http.Authorization;
import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import io.smallrye.graphql.client.typesafe.api.AuthorizationHeader;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientBuilder;
import org.eclipse.microprofile.graphql.Name;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static com.github.t1.wunderbar.junit.Utils.errorCode;

class GraphQlExpectation extends HttpServiceExpectation {
    private final String configPrefix;
    private Authorization.Basic old;

    GraphQlExpectation(BarWriter bar, Method method, Object... args) {
        super(bar, method, args);
        this.configPrefix = method.getDeclaringClass().getName() + "/mp-graphql/";
    }

    @Override protected Object service() {
        if (needsAuthorizationConfig()) this.old = configureDummyAuthorization();
        return GraphQlClientBuilder.newBuilder()
            .endpoint(baseUri().resolve("/graphql"))
            .build(method.getDeclaringClass());
    }

    private boolean needsAuthorizationConfig() {
        return method.isAnnotationPresent(AuthorizationHeader.class)
            || method.getDeclaringClass().isAnnotationPresent(AuthorizationHeader.class);
    }

    private Authorization.Basic configureDummyAuthorization() {
        var oldUsername = System.setProperty(configPrefix + "username", "dummy-username");
        var oldPassword = System.setProperty(configPrefix + "password", "dummy-password");
        return new Authorization.Basic(oldUsername, oldPassword);
    }

    @Override protected HttpServerResponse handleRequest(HttpServerRequest request) {
        return HttpServerResponse.builder().body(buildResponseBody()).build();
    }

    private GraphQlResponseBody buildResponseBody() {
        var responseBuilder = GraphQlResponseBody.builder();
        if (getResponse() != null)
            responseBuilder.data(Map.of(dataName(), getResponse()));
        if (getException() != null)
            responseBuilder.errors(List.of(GraphQlError.builder()
                .message(getException().getMessage())
                .extension("code", errorCode(getException()))
                .build()));
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

    @Override public void done() {
        super.done();
        if (old != null) {
            setSystemProperty("username", old.getUsername());
            setSystemProperty("password", old.getPassword());
        }
    }

    private void setSystemProperty(String name, String value) {
        if (value == null) System.clearProperty(configPrefix + name);
        else System.setProperty(configPrefix + name, value);
    }
}

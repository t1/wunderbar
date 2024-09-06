package com.github.t1.wunderbar.junit.consumer.integration;

import com.github.t1.wunderbar.junit.http.Authorization;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import io.smallrye.graphql.client.typesafe.api.AuthorizationHeader;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.smallrye.graphql.client.typesafe.api.TypesafeGraphQLClientBuilder;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import java.lang.reflect.Method;
import java.net.URI;

import static com.github.t1.wunderbar.junit.http.HttpUtils.errorCode;
import static com.github.t1.wunderbar.junit.http.HttpUtils.readJson;

class GraphQlExpectation extends HttpServiceExpectation {
    private final String configKey;
    private Authorization.Basic oldAuth;

    GraphQlExpectation(URI baseUri, Method method, Object... args) {
        super(baseUri, method, args);
        this.configKey = configKey(method);
    }

    private String configKey(Method method) {
        var declaringClass = method.getDeclaringClass();
        var graphQLClientApi = declaringClass.getAnnotation(GraphQLClientApi.class);
        return graphQLClientApi.configKey().isEmpty() ? declaringClass.getName() : graphQLClientApi.configKey();
    }

    @Override protected Object service() {
        if (needsAuthorizationConfig()) this.oldAuth = configureDummyAuthorization();
        return TypesafeGraphQLClientBuilder.newBuilder()
                .endpoint(baseUri())
                .configKey(configKey)
                .build(method.getDeclaringClass());
    }

    private boolean needsAuthorizationConfig() {
        return method.isAnnotationPresent(AuthorizationHeader.class)
               || method.getDeclaringClass().isAnnotationPresent(AuthorizationHeader.class);
    }

    private Authorization.Basic configureDummyAuthorization() {
        var oldUsername = setConfig("username", "dummy-username");
        var oldPassword = setConfig("password", "dummy-password");
        return new Authorization.Basic(oldUsername, oldPassword);
    }

    @Override public HttpResponse handleRequest(HttpRequest request) {
        return HttpResponse.builder().body(buildResponseBody()).build();
    }

    private JsonObject buildResponseBody() {
        var responseBuilder = Json.createObjectBuilder();
        if (getException() == null)
            responseBuilder.add("data", Json.createObjectBuilder().add(dataName(), readJson(getResponse())));
        else
            responseBuilder.add("errors", Json.createArrayBuilder().add(Json.createObjectBuilder()
                    .add("message", getException().getMessage())
                    .add("extensions", Json.createObjectBuilder()
                            .add("code", errorCode(getException()))
                            .build())));
        return responseBuilder.build();
    }

    private String dataName() {
        if (method.isAnnotationPresent(Name.class))
            return method.getAnnotation(Name.class).value();
        if (method.isAnnotationPresent(Query.class) && !method.getAnnotation(Query.class).value().isEmpty())
            return method.getAnnotation(Query.class).value();
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
        if (oldAuth != null) {
            setConfig("username", oldAuth.getUsername());
            setConfig("password", oldAuth.getPassword());
        }
    }

    private String setConfig(String name, String value) {
        return (value == null)
                ? System.clearProperty(configKey + "/mp-graphql/" + name)
                : System.setProperty(configKey + "/mp-graphql/" + name, value);
    }
}

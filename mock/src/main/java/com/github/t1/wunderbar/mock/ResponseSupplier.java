package com.github.t1.wunderbar.mock;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.github.t1.wunderbar.mock.RestResponseSupplier.restResponse;

interface ResponseSupplier {
    void apply(RequestMatcher matcher, HttpServletResponse response);
}

class GraphQLResponseSupplier implements ResponseSupplier {

    public static final String APPLICATION_JSON = "application/json;charset=utf-8";

    static GraphQLResponseSupplier graphQL() {return new GraphQLResponseSupplier();}

    public static GraphQLResponseSupplier graphQlError(String code, String message) {
        return graphQL().add("errors", Json.createArrayBuilder()
            .add(Json.createObjectBuilder()
                .add("message", message)
                .add("extensions", Json.createObjectBuilder()
                    .add("code", code))
            ));
    }


    JsonObjectBuilder builder = Json.createObjectBuilder();
    JsonObject body;

    public GraphQLResponseSupplier add(String name, JsonArrayBuilder array) {
        builder.add(name, array);
        return this;
    }

    public GraphQLResponseSupplier add(String name, JsonObjectBuilder object) {
        builder.add(name, object);
        return this;
    }

    @Override
    @SneakyThrows(IOException.class)
    public void apply(RequestMatcher matcher, HttpServletResponse response) {
        response.setStatus(200);
        response.setContentType(APPLICATION_JSON);
        if (body == null) body = builder.build();
        response.getWriter().write(body.toString());
    }
}

class RestErrorSupplier implements ResponseSupplier {
    private final RestResponseSupplier response = restResponse()
        .status(400)
        .contentType("application/problem+json;charset=utf-8");

    public static RestErrorSupplier restError() {return new RestErrorSupplier();}

    RestErrorSupplier status(int status) {
        response.status(status);
        return this;
    }

    RestErrorSupplier contentType(String contentType) {
        response.contentType(contentType);
        return this;
    }

    RestErrorSupplier detail(String detail) {
        response.add("detail", detail);
        return this;
    }

    RestErrorSupplier title(String title) {
        response.add("title", title);
        return this;
    }

    RestErrorSupplier type(String type) {
        response.add("type", type);
        return this;
    }

    @Override public void apply(RequestMatcher matcher, HttpServletResponse response) {
        this.response.apply(matcher, response);
    }
}

@Setter @Accessors(fluent = true, chain = true)
class RestResponseSupplier implements ResponseSupplier {
    public static RestResponseSupplier restResponse() {return new RestResponseSupplier();}

    int status = 200;
    String contentType = "application/json;charset=utf-8";
    JsonObjectBuilder builder;
    JsonObject body;

    RestResponseSupplier add(String name, String value) {
        if (builder == null) builder = Json.createObjectBuilder();
        builder.add(name, value);
        return this;
    }

    @Override
    @SneakyThrows(IOException.class)
    public void apply(RequestMatcher matcher, HttpServletResponse response) {
        response.setStatus(status);
        if (body == null) body = builder.build();
        Json.createWriter(response.getWriter()).write(body);
    }
}

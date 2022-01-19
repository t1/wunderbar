package com.github.t1.wunderbar.common.mock;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.function.Consumer;

import static com.github.t1.wunderbar.junit.http.HttpUtils.APPLICATION_JSON_UTF8;

@RequiredArgsConstructor @ToString
public class GraphQLResponseSupplier implements ResponseSupplier {
    public static GraphQLResponseSupplierBuilder graphQL() {return new GraphQLResponseSupplierBuilder();}

    public static GraphQLResponseSupplier graphQlError(String code, String message) {
        return graphQL().add("errors", Json.createArrayBuilder()
            .add(Json.createObjectBuilder()
                .add("message", message)
                .add("extensions", Json.createObjectBuilder()
                    .add("code", code))
            )).build();
    }

    private final String body;

    public static class GraphQLResponseSupplierBuilder {
        private final JsonObjectBuilder builder = Json.createObjectBuilder();

        public GraphQLResponseSupplierBuilder with(Consumer<JsonObjectBuilder> json) {
            json.accept(builder);
            return this;
        }

        public GraphQLResponseSupplierBuilder add(String name, JsonArrayBuilder array) {
            builder.add(name, array);
            return this;
        }

        public GraphQLResponseSupplierBuilder add(String name, JsonObjectBuilder object) {
            builder.add(name, object);
            return this;
        }

        public GraphQLResponseSupplier build() {
            return new GraphQLResponseSupplier(builder.build().toString());
        }
    }

    @Override
    @SneakyThrows(IOException.class)
    public void apply(HttpServletRequest request, String requestBody, HttpServletResponse response) {
        response.setStatus(200);
        response.setContentType(APPLICATION_JSON_UTF8.toString());
        response.getWriter().write(body);
    }
}

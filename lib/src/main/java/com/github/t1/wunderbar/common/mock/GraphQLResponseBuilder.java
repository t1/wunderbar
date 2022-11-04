package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpResponse;
import lombok.ToString;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import java.util.function.Consumer;

@ToString
public class GraphQLResponseBuilder {
    public static GraphQLResponseBuilder graphQLResponse() {return new GraphQLResponseBuilder();}

    public static HttpResponse graphQlError(String code, String message) {
        return graphQLResponse().add("errors", Json.createArrayBuilder()
            .add(Json.createObjectBuilder()
                .add("message", message)
                .add("extensions", Json.createObjectBuilder()
                    .add("code", code))
            )).build();
    }

    private final JsonObjectBuilder builder = Json.createObjectBuilder();

    public GraphQLResponseBuilder with(Consumer<JsonObjectBuilder> json) {
        json.accept(builder);
        return this;
    }

    public GraphQLResponseBuilder add(String name, JsonArrayBuilder array) {
        builder.add(name, array);
        return this;
    }

    public GraphQLResponseBuilder add(String name, JsonObjectBuilder object) {
        builder.add(name, object);
        return this;
    }

    public GraphQLResponseBuilder body(JsonObject body) {
        builder.addAll(Json.createObjectBuilder(body));
        return this;
    }

    public HttpResponse build() {
        return HttpResponse.builder().body(builder.build()).build();
    }
}

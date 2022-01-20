package com.github.t1.wunderbar.common.mock;

import lombok.ToString;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.function.Consumer;

import static com.github.t1.wunderbar.common.mock.RestResponseSupplier.restResponse;

@ToString
public class GraphQLResponseBuilder {
    public static GraphQLResponseBuilder graphQL() {return new GraphQLResponseBuilder();}

    public static ResponseSupplier graphQlError(String code, String message) {
        return graphQL().add("errors", Json.createArrayBuilder()
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

    public ResponseSupplier build() {
        return restResponse().body(builder.build());
    }
}

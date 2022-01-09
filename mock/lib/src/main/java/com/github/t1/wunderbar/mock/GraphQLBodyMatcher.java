package com.github.t1.wunderbar.mock;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import java.util.Map;
import java.util.function.Predicate;

@Value @Builder(buildMethodName = "internalBuild")
public class GraphQLBodyMatcher implements Predicate<JsonValue> {
    public static GraphQLBodyMatcher.GraphQLBodyMatcherBuilder graphQlRequest() {return GraphQLBodyMatcher.builder();}

    boolean emptyBody;
    String query;
    @Singular Map<String, Object> variables;

    public static class GraphQLBodyMatcherBuilder {
        public RequestMatcher build() {
            return new RequestMatcher("POST", ".*/graphql", "application/.*json.*", internalBuild());
        }
    }

    @Override public boolean test(JsonValue jsonValue) {return matchBody(jsonValue);}

    private boolean matchBody(JsonValue jsonValue) {
        if (emptyBody) return jsonValue == JsonValue.NULL;
        return jsonValue.getValueType() == ValueType.OBJECT && matchBody(jsonValue.asJsonObject());
    }

    private boolean matchBody(JsonObject jsonObject) {
        return matchQuery(jsonObject) && matchVariables(jsonObject);
    }

    private boolean matchQuery(JsonObject jsonObject) {
        return jsonObject.getString("query").matches(query);
    }

    private boolean matchVariables(JsonObject jsonObject) {
        return variables == null || variables.entrySet().stream()
            .allMatch(entry -> matchVariable(jsonObject.getJsonObject("variables"), entry.getKey(), entry.getValue()));
    }

    private boolean matchVariable(JsonObject jsonObject, String key, Object value) {
        return jsonObject.getString(key).equals(value);
    }
}

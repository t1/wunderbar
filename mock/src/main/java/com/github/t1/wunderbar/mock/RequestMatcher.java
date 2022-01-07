package com.github.t1.wunderbar.mock;

import lombok.Setter;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.servlet.http.HttpServletRequest;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Value
class RequestMatcher {
    static GraphQLMatcherBuilder graphQlRequest() {return new GraphQLMatcherBuilder();}

    public static RestMatcherBuilder restRequest() {return new RestMatcherBuilder();}

    String method;
    String path;
    String contentType;
    Predicate<JsonValue> bodyMatcher;

    public boolean matches(HttpServletRequest request, String body) {
        return request.getMethod().matches(method)
               && (path == null || request.getRequestURI().matches(path))
               && (contentType == null || request.getContentType().matches(contentType))
               && (bodyMatcher == null || bodyMatcher.test(json(body)));
    }

    private JsonValue json(String body) {
        return body.isBlank() ? JsonValue.NULL : Json.createReader(new StringReader(body)).read();
    }

    @ToString // used in the Predicate
    @Setter @Accessors(fluent = true, chain = true)
    public static class GraphQLMatcherBuilder {
        private boolean emptyBody;
        private Pattern query;
        private Map<String, Object> variables;

        public GraphQLMatcherBuilder variable(String key, Object value) {
            if (variables == null) variables = new LinkedHashMap<>();
            variables.put(key, value);
            return this;
        }

        public RequestMatcher build() {
            return new RequestMatcher("POST", ".*/graphql", "application/.*json.*", new Predicate<>() {
                @Override public boolean test(JsonValue jsonValue) {return matchBody(jsonValue);}

                @Override public String toString() {
                    return GraphQLMatcherBuilder.this.toString()
                        .replaceAll("^.*?\\(", "("); // remove classname prefix
                }
            });
        }

        private boolean matchBody(JsonValue jsonValue) {
            if (emptyBody) return jsonValue == JsonValue.NULL;
            return jsonValue.getValueType() == ValueType.OBJECT && matchBody(jsonValue.asJsonObject());
        }

        private boolean matchBody(JsonObject jsonObject) {
            return matchQuery(jsonObject) && matchVariables(jsonObject);
        }

        private boolean matchQuery(JsonObject jsonObject) {
            return query.matcher(jsonObject.getString("query")).matches();
        }

        private boolean matchVariables(JsonObject jsonObject) {
            return variables == null || variables.entrySet().stream()
                .allMatch(entry -> matchVariable(jsonObject.getJsonObject("variables"), entry.getKey(), entry.getValue()));
        }

        private boolean matchVariable(JsonObject jsonObject, String key, Object value) {
            return jsonObject.getString(key).equals(value);
        }
    }

    @Setter @Accessors(fluent = true, chain = true)
    public static class RestMatcherBuilder {
        String method = "GET";
        String path;
        String contentType;
        Predicate<JsonValue> bodyMatcher;

        public RequestMatcher build() {
            return new RequestMatcher(method, path, contentType, bodyMatcher);
        }
    }
}

package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.json.JsonValue;
import javax.json.bind.annotation.JsonbCreator;
import java.util.function.Predicate;

import static com.github.t1.wunderbar.common.Utils.jsonNonAddDiff;
import static com.github.t1.wunderbar.common.Utils.readJson;

@Value @Builder
public class RequestMatcher {
    @Default String method = "GET";
    String path;
    String contentType;
    Predicate<JsonValue> bodyMatcher;

    public boolean matches(HttpRequest request, JsonValue body) {
        return request.getMethod().matches(method)
               && (path == null || request.getUri().toString().matches(path))
               && (contentType == null || request.getContentType().toString().matches(contentType))
               && (bodyMatcher == null || bodyMatcher.test(body));
    }

    @Getter @Setter @ToString(of = "expected") @Slf4j
    public static class BodyMatcher implements Predicate<JsonValue> {
        private final String expected;
        transient JsonValue expectedJson;

        @JsonbCreator
        public BodyMatcher(String expected) {this.expected = expected;}

        public BodyMatcher(JsonValue expected) {
            this.expected = expected.toString();
            this.expectedJson = expected;
        }

        @Override
        public boolean test(JsonValue actual) {
            return jsonNonAddDiff(expectedJson(), actual)
                .peek(action -> log.debug("does not match: {}", action))
                .findAny().isEmpty();
        }

        private JsonValue expectedJson() {
            if (expectedJson == null) expectedJson = readJson(expected);
            return expectedJson;
        }
    }
}

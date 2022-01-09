package com.github.t1.wunderbar.mock;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

import javax.json.Json;
import javax.json.JsonValue;
import javax.servlet.http.HttpServletRequest;
import java.io.StringReader;
import java.util.function.Predicate;

@Value @Builder
public class RequestMatcher {
    @Default String method = "GET";
    String path;
    String contentType;
    Predicate<JsonValue> bodyMatcher;

    public boolean matches(HttpServletRequest request, String body) {
        return request.getMethod().matches(method)
               && (path == null || request.getRequestURI().matches(path))
               && (contentType == null || request.getContentType().matches(contentType))
               && (bodyMatcher == null || bodyMatcher.test(json(body)));
    }

    public static JsonValue json(String body) {
        return body.isBlank() ? JsonValue.NULL : Json.createReader(new StringReader(body)).read();
    }

}

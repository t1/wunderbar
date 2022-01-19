package com.github.t1.wunderbar.common.mock;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

import javax.json.JsonValue;
import javax.servlet.http.HttpServletRequest;
import java.util.function.Predicate;

import static com.github.t1.wunderbar.common.Utils.readJson;

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
               && (bodyMatcher == null || bodyMatcher.test(readJson(body)));
    }
}

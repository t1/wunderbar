package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.util.function.Function;

@Data @AllArgsConstructor
public class WunderBarMockExpectation {
    public static WunderBarMockExpectation of(RequestMatcher requestMatcher, Function<HttpRequest, HttpResponse> handler) {
        return new WunderBarMockExpectation(requestMatcher, handler);
    }

    private static int nextId = 0;

    private final int id = nextId++;
    private @NonNull RequestMatcher requestMatcher;
    private @NonNull Function<HttpRequest, HttpResponse> handler;
}

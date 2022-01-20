package com.github.t1.wunderbar.common.mock;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data @AllArgsConstructor
public class WunderBarMockExpectation {
    public static WunderBarMockExpectation of(RequestMatcher requestMatcher, ResponseSupplier responseSupplier) {
        return new WunderBarMockExpectation(requestMatcher, responseSupplier);
    }

    private static int nextId = 0;

    private final int id = nextId++;
    private @NonNull RequestMatcher requestMatcher;
    private @NonNull ResponseSupplier responseSupplier;
}

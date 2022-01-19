package com.github.t1.wunderbar.common.mock;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class WunderBarMockExpectation {
    public static WunderBarMockExpectation exp(RequestMatcher requestMatcher, ResponseSupplier responseSupplier) {
        return new WunderBarMockExpectation(requestMatcher, responseSupplier);
    }

    private static int nextId = 0;

    private final int id = nextId++;
    private RequestMatcher requestMatcher;
    private ResponseSupplier responseSupplier;
}

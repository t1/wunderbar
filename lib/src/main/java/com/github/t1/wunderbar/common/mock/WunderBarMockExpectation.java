package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import lombok.Data;

@Data
public abstract class WunderBarMockExpectation {
    private static int nextId = 0;
    private final int id = nextId++;

    public abstract boolean matches(HttpRequest request);

    public abstract HttpResponse handle(HttpRequest request);
}

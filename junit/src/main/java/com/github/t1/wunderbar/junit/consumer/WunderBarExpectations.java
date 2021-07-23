package com.github.t1.wunderbar.junit.consumer;

import java.lang.reflect.Method;
import java.net.URI;

public @Internal interface WunderBarExpectations {
    URI baseUri();

    Object invoke(Method method, Object... args);

    void done();
}

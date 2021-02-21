package com.github.t1.wunderbar.junit.consumer;

import java.lang.reflect.Method;

public @Internal interface WunderBarExpectations {
    Object invoke(Method method, Object... args);

    void done();
}

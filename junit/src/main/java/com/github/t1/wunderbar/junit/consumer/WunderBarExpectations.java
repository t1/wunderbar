package com.github.t1.wunderbar.junit.consumer;

import java.lang.reflect.Method;

public interface WunderBarExpectations {
    Object invoke(Method method, Object... args);

    default void done() {}
}

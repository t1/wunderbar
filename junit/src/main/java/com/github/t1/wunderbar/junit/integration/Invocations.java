package com.github.t1.wunderbar.junit.integration;

import java.lang.reflect.Method;

public interface Invocations {
    Object invoke(Method method, Object... args) throws Exception;

    default void done() {}
}

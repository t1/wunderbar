package com.github.t1.bar.junit;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor @ToString
class Stub {
    final @NonNull Method method;
    final @NonNull Object[] args;
    Object response;
    Exception exception;

    void assertUnset(String method) {
        assert response == null : "double " + method + " (response)";
        assert exception == null : "double " + method + " (exception)";
    }

    boolean matches(Method method, Object... args) {
        return method == this.method && Arrays.deepEquals(args, this.args);
    }

    Object invoke() throws Exception {
        if (exception != null)
            throw exception;
        return response;
    }
}

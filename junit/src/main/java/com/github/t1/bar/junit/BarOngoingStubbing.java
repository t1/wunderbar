package com.github.t1.bar.junit;

public class BarOngoingStubbing<T> {
    public static <T> BarOngoingStubbing<T> given(T methodCall) {
        if (methodCall != null)
            throw new JUnitBarException("call `given` only once and only on the response object of a mock (null)");
        if (stub == null)
            throw new JUnitBarException("call `given` only on the response object of a mock (invocation)");
        return new BarOngoingStubbing<>();
    }

    static Stub stub;

    public void willReturn(T response) {
        stub.assertUnset("willReturn");
        stub.response = response;
        stub = null;
    }

    public void willThrow(Exception exception) {
        stub.assertUnset("willThrow");
        stub.exception = exception;
        stub = null;
    }
}

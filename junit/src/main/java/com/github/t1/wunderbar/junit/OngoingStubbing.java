package com.github.t1.wunderbar.junit;

public class OngoingStubbing<T> {
    public static <T> OngoingStubbing<T> given(T methodCall) {
        if (methodCall != null)
            throw new JUnitWunderBarException("call `given` only once and only on the response object of a proxy (null)");
        if (stub == null)
            throw new JUnitWunderBarException("call `given` only on the response object of a proxy (invocation)");
        return new OngoingStubbing<>();
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

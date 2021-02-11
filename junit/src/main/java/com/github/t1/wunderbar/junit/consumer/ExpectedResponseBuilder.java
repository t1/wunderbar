package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.junit.WunderBarException;

import java.util.Objects;

public class ExpectedResponseBuilder<T> {
    public static <T> ExpectedResponseBuilder<T> given(T dummyValue) {
        if (buildingInvocation == null || !Objects.equals(dummyValue, buildingInvocation.nullValue()))
            throw new ExpectedResponseBuilderException();
        return new ExpectedResponseBuilder<>();
    }

    public static Invocation buildingInvocation;

    public void willReturn(T response) {
        if (buildingInvocation == null) throw new ExpectedResponseBuilderException();
        buildingInvocation.willReturn(response);
    }

    public void willThrow(Exception exception) {
        if (buildingInvocation == null) throw new ExpectedResponseBuilderException();
        buildingInvocation.willThrow(exception);
    }

    static class ExpectedResponseBuilderException extends WunderBarException {
        ExpectedResponseBuilderException() { super("Stubbing mismatch: call `given` exactly once on the response object of a proxy call"); }
    }
}

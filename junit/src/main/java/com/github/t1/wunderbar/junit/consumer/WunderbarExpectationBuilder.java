package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.junit.WunderBarException;

import java.util.Objects;

public class WunderbarExpectationBuilder<T> {
    public static <T> WunderbarExpectationBuilder<T> given(T dummyValue) {
        if (buildingExpectation == null || !Objects.equals(dummyValue, buildingExpectation.nullValue()))
            throw new WunderbarExpectationBuilderException();
        return new WunderbarExpectationBuilder<>();
    }

    public static WunderBarExpectation buildingExpectation;

    public void willReturn(T response) {
        if (buildingExpectation == null) throw new WunderbarExpectationBuilderException();
        buildingExpectation.willReturn(response);
    }

    public void willThrow(Exception exception) {
        if (buildingExpectation == null) throw new WunderbarExpectationBuilderException();
        buildingExpectation.willThrow(exception);
    }

    static class WunderbarExpectationBuilderException extends WunderBarException {
        WunderbarExpectationBuilderException() { super("Stubbing mismatch: call `given` exactly once on the response object of a proxy call"); }
    }
}

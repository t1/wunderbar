package com.github.t1.wunderbar.junit.consumer.unit;

import com.github.t1.wunderbar.junit.consumer.WunderBarExpectation;
import com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder;
import lombok.SneakyThrows;
import org.mockito.BDDMockito;
import org.mockito.BDDMockito.BDDMyOngoingStubbing;

import java.lang.reflect.Method;
import java.util.Arrays;

class MockExpectation extends WunderBarExpectation {
    private final BDDMyOngoingStubbing<Object> mockitoStub;

    public MockExpectation(Object mock, Method method, Object... args) {
        super(method, args);
        this.mockitoStub = buildMockitoStub(mock, method, args);
    }

    boolean matches(Method method, Object... args) {
        return method == this.method && Arrays.deepEquals(args, this.args);
    }

    @SneakyThrows(ReflectiveOperationException.class)
    private BDDMyOngoingStubbing<Object> buildMockitoStub(Object mock, Method method, Object... args) {
        method.setAccessible(true);
        return BDDMockito.given(method.invoke(mock, args));
    }

    @Override public void willReturn(Object response) {
        mockitoStub.willReturn(response);
        WunderbarExpectationBuilder.buildingExpectation = null;
    }

    @Override public void willThrow(Exception exception) {
        mockitoStub.willThrow(exception);
        WunderbarExpectationBuilder.buildingExpectation = null;
    }
}

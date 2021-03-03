package com.github.t1.wunderbar.junit.consumer.unit;

import com.github.t1.wunderbar.junit.consumer.WunderBarExpectation;
import lombok.SneakyThrows;
import org.mockito.BDDMockito;
import org.mockito.BDDMockito.BDDMyOngoingStubbing;

import java.lang.reflect.Method;

class MockExpectation extends WunderBarExpectation {
    private final BDDMyOngoingStubbing<Object> mockitoStub;

    public MockExpectation(Object mock, Method method, Object... args) {
        super(method, args);
        this.mockitoStub = buildMockitoStub(mock, method, args);
    }

    @SneakyThrows(ReflectiveOperationException.class)
    private BDDMyOngoingStubbing<Object> buildMockitoStub(Object mock, Method method, Object... args) {
        method.setAccessible(true);
        return BDDMockito.given(method.invoke(mock, args));
    }

    @Override public void willReturn(Object response) {
        mockitoStub.willReturn(response);
    }

    @Override public void willThrow(Exception exception) {
        mockitoStub.willThrow(exception);
    }
}

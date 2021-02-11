package com.github.t1.wunderbar.junit.consumer.unit;

import com.github.t1.wunderbar.junit.consumer.ExpectedResponseBuilder;
import com.github.t1.wunderbar.junit.consumer.Invocation;
import lombok.SneakyThrows;
import org.mockito.BDDMockito;
import org.mockito.BDDMockito.BDDMyOngoingStubbing;

import java.lang.reflect.Method;
import java.util.Arrays;

class MockInvocation extends Invocation {
    private final BDDMyOngoingStubbing<Object> mockitoStub;

    public MockInvocation(Object mock, Method method, Object... args) {
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
        ExpectedResponseBuilder.buildingInvocation = null;
    }

    @Override public void willThrow(Exception exception) {
        mockitoStub.willThrow(exception);
        ExpectedResponseBuilder.buildingInvocation = null;
    }
}

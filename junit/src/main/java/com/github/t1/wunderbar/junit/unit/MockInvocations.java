package com.github.t1.wunderbar.junit.unit;

import com.github.t1.wunderbar.junit.ExpectedResponseBuilder;
import com.github.t1.wunderbar.junit.integration.Invocations;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MockInvocations implements Invocations {
    private final List<MockInvocation> invocations = new ArrayList<>();
    private final Object mock;

    public MockInvocations(Class<?> type) {
        this.mock = Mockito.mock(type);
    }

    @Override public Object invoke(Method method, Object... args) throws Exception {
        for (var invocation : invocations)
            if (invocation.matches(method, args))
                return invokeOnMock(method, args);

        var invocation = new MockInvocation(mock, method, args);
        invocations.add(invocation);
        ExpectedResponseBuilder.buildingInvocation = invocation;

        return invocation.nullValue();
    }

    private Object invokeOnMock(Method method, Object... args) throws Exception {
        try {
            return method.invoke(mock, args);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof RuntimeException)
                throw (RuntimeException) e.getTargetException();
            throw e;
        }
    }
}

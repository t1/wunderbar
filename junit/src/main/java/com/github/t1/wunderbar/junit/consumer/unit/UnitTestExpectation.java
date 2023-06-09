package com.github.t1.wunderbar.junit.consumer.unit;

import com.github.t1.wunderbar.junit.consumer.Depletion;
import com.github.t1.wunderbar.junit.consumer.WunderBarExpectation;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.mockito.BDDMockito;
import org.mockito.BDDMockito.BDDMyOngoingStubbing;

import java.lang.reflect.Method;
import java.net.URI;

class UnitTestExpectation extends WunderBarExpectation {
    private final BDDMyOngoingStubbing<Object> mockitoStub;
    private int callCount;

    public UnitTestExpectation(Object mock, Method method, Object... args) {
        super(method, args);
        this.mockitoStub = buildMockitoStub(mock, method, args);
    }

    @SneakyThrows(ReflectiveOperationException.class)
    private BDDMyOngoingStubbing<Object> buildMockitoStub(Object mock, Method method, Object... args) {
        method.setAccessible(true);
        return BDDMockito.given(method.invoke(mock, args));
    }

    @Override public URI baseUri() {return null;}

    @Override public void returns(@NonNull Depletion depletion, Object response) {
        mockitoStub.willAnswer(i -> {
            ++callCount;
            depletion.check(callCount);
            return response;
        });
    }

    @Override public void willThrow(@NonNull Depletion depletion, @NonNull Exception exception) {
        mockitoStub.willAnswer(i -> {
            ++callCount;
            depletion.check(callCount);
            throw exception;
        });
    }
}

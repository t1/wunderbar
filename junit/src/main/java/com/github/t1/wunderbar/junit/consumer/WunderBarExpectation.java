package com.github.t1.wunderbar.junit.consumer;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;

import static lombok.AccessLevel.PROTECTED;

@RequiredArgsConstructor(access = PROTECTED)
public abstract class WunderBarExpectation {
    protected final @NonNull Method method;
    protected final @NonNull Object[] args;

    public final Object nullValue() { return nullValue(method.getReturnType()); }

    private Object nullValue(Class<?> type) {
        if (type.equals(boolean.class)) return false;
        if (type.equals(byte.class)) return (byte) 0;
        if (type.equals(char.class)) return (char) 0;
        if (type.equals(short.class)) return (short) 0;
        if (type.equals(int.class)) return 0;
        if (type.equals(long.class)) return (long) 0;
        if (type.equals(float.class)) return 0.0f;
        if (type.equals(double.class)) return 0.0d;
        return null;
    }

    public abstract void willReturn(Object response);

    public abstract void willThrow(Exception exception);

    public void done() {}
}

package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.common.Internal;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;

import static com.github.t1.wunderbar.common.DeepEquals.deeplyEqual;
import static lombok.AccessLevel.PROTECTED;

@RequiredArgsConstructor(access = PROTECTED)
public abstract @Internal class WunderBarExpectation {
    protected final Method method;
    protected final Object[] args;

    @Override public String toString() {
        return getClass().getSimpleName() + " for " + method + Arrays.toString(args);
    }

    public final boolean matches(@NonNull Method method, @NonNull Object... args) {
        return method.equals(this.method) && deeplyEqual(args, this.args);
    }

    public final Object nullValue() {return nullValue(method.getReturnType());}

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

    public abstract URI baseUri();

    public abstract void returns(@NonNull Depletion depletion, @NonNull Object response);

    public abstract void willThrow(@NonNull Depletion depletion, @NonNull Exception exception);

    public void done() {}
}

package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.junit.Bar;
import com.github.t1.wunderbar.junit.consumer.integration.HttpServiceExpectations;
import com.github.t1.wunderbar.junit.consumer.system.SystemExpectations;
import com.github.t1.wunderbar.junit.consumer.unit.MockExpectations;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

class Proxy {
    private final Level level;
    private final Optional<Bar> bar;
    private final Class<?> type;
    final Object instance;
    private final String endpoint;
    private final WunderBarExpectations expectations;

    public Proxy(Level level, Optional<Bar> bar, Class<?> type, String endpoint) {
        this.level = level;
        this.bar = bar;
        this.type = type;
        this.instance = createProxy(type);
        this.endpoint = endpoint;
        this.expectations = createExpectations();
    }

    private <T> T createProxy(Class<T> type) {
        return type.cast(java.lang.reflect.Proxy.newProxyInstance(getClassLoader(), new Class[]{type}, this::proxyInvoked));
    }

    private static ClassLoader getClassLoader() {
        var classLoader = Thread.currentThread().getContextClassLoader();
        return (classLoader == null) ? ClassLoader.getSystemClassLoader() : classLoader;
    }

    private Object proxyInvoked(@SuppressWarnings("unused") Object proxy, Method method, Object... args) {
        if (args == null) args = new Object[0];
        return expectations.invoke(method, args);
    }

    private WunderBarExpectations createExpectations() {
        switch (level) {
            case AUTO:
                throw new IllegalStateException("Unreachable code: AUTO level should have been resolved already");
            case UNIT:
                return new MockExpectations(type);
            case INTEGRATION:
                return new HttpServiceExpectations(bar);
            case SYSTEM:
                return new SystemExpectations(type, endpoint);
        }
        throw new UnsupportedOperationException("unreachable");
    }

    public boolean isAssignableTo(Field field) {
        return field.getType().isAssignableFrom(type);
    }

    void done() { expectations.done(); }
}

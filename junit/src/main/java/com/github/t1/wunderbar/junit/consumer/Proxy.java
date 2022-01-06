package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.junit.consumer.integration.IntegrationTestExpectations;
import com.github.t1.wunderbar.junit.consumer.system.SystemTestExpectations;
import com.github.t1.wunderbar.junit.consumer.unit.UnitTestExpectations;
import lombok.Getter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;

class Proxy {
    private final Level level;
    private final BarWriter bar;
    private final Class<?> type;
    private final URI endpoint;
    private final Technology technology;
    private final Object proxy;
    @Getter private final WunderBarExpectations expectations;

    public Proxy(Level level, BarWriter bar, Class<?> type, URI endpoint, Technology technology) {
        this.level = level;
        this.bar = bar;
        this.type = type;
        this.endpoint = endpoint;
        this.technology = technology;
        this.proxy = createProxy(type);
        this.expectations = createExpectations();
    }

    @Override public String toString() {
        return "Proxy(" + level + ":" + type.getSimpleName() + ":" + expectations.baseUri() + ")";
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
        if (method.getName().equals("toString") && method.getParameterCount() == 0)
            return this.toString();
        return expectations.invoke(method, args);
    }

    private WunderBarExpectations createExpectations() {
        switch (level) {
            case AUTO:
                throw new IllegalStateException("Unreachable code: AUTO level should have been resolved already");
            case UNIT:
                return new UnitTestExpectations(type);
            case INTEGRATION:
                return new IntegrationTestExpectations(endpoint, technology, bar);
            case SYSTEM:
                return new SystemTestExpectations(type, endpoint, technology, bar);
        }
        throw new UnsupportedOperationException("unreachable");
    }

    public boolean isAssignableTo(Field field) {
        return field.getType().isAssignableFrom(type);
    }

    Object getStubbingProxy() {return expectations.asStubbingProxy(proxy);}

    Object getSutProxy() {return expectations.asSutProxy(proxy);}

    void done() {expectations.done();}
}

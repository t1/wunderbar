package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.junit.consumer.integration.IntegrationTestExpectations;
import com.github.t1.wunderbar.junit.consumer.system.SystemTestExpectations;
import com.github.t1.wunderbar.junit.consumer.unit.UnitTestExpectations;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URI;

class Proxy<T> implements ProxyFactory<T> {
    private final Level level;
    private final BarWriter bar;
    private final Class<T> type;
    private final URI endpoint;
    private final Technology technology;
    private final T proxy;
    @Getter private final WunderBarExpectations<T> expectations;

    public Proxy(Level level, BarWriter bar, Class<T> type, URI endpoint, Technology technology) {
        this.level = level;
        this.bar = bar;
        this.type = type;
        this.endpoint = endpoint;
        this.technology = technology;
        this.proxy = createProxy(type);
        this.expectations = createExpectations();
    }

    @Override public String toString() {
        return "Proxy(" + technology.path() + "-" + level + ":" + type.getSimpleName() + ":" + expectations.baseUri() + ")";
    }

    private T createProxy(Class<T> type) {
        return type.cast(java.lang.reflect.Proxy.newProxyInstance(getClassLoader(), new Class[]{type}, new ProxyInvocationHandler(this)));
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

    private WunderBarExpectations<T> createExpectations() {
        return switch (level) {
            case AUTO ->
                    throw new IllegalStateException("Unreachable code: AUTO level should have been resolved already");
            case UNIT -> new UnitTestExpectations<>(type);
            case INTEGRATION -> new IntegrationTestExpectations<>(endpoint, technology, bar);
            case SYSTEM -> new SystemTestExpectations<>(type, endpoint, technology, bar);
        };
    }

    public boolean isAssignableTo(Field field) {
        return field.getType().isAssignableFrom(type);
    }

    @Override public T getStubbingProxy() {return expectations.asStubbingProxy(proxy);}

    @Override public T getSutProxy() {return expectations.asSutProxy(proxy);}

    void done() {expectations.done();}

    @RequiredArgsConstructor
    static class ProxyInvocationHandler implements InvocationHandler {
        final Proxy<?> proxy;

        @Override public Object invoke(Object proxy1, Method method, Object[] args) throws Throwable {
            return proxy.proxyInvoked(proxy1, method, args);
        }
    }
}

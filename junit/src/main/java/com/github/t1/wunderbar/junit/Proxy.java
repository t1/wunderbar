package com.github.t1.wunderbar.junit;

import com.github.t1.wunderbar.junit.integration.HttpServiceInvocations;
import com.github.t1.wunderbar.junit.integration.Invocations;
import com.github.t1.wunderbar.junit.unit.MockInvocations;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

class Proxy {
    private final WunderBarExtension settings;
    private final String id;
    private final Class<?> type;
    final Object instance;
    private final Invocations invocations;

    public Proxy(WunderBarExtension settings, String id, Class<?> type) {
        this.settings = settings;
        this.id = id;
        this.type = type;
        this.instance = createProxy(type);
        this.invocations = createInvocations();
    }

    private <T> T createProxy(Class<T> type) {
        return type.cast(java.lang.reflect.Proxy.newProxyInstance(getClassLoader(), new Class[]{type}, this::proxyInvoked));
    }

    private static ClassLoader getClassLoader() {
        var classLoader = Thread.currentThread().getContextClassLoader();
        return (classLoader == null) ? ClassLoader.getSystemClassLoader() : classLoader;
    }

    private Invocations createInvocations() {
        switch (settings.level()) {
            case UNIT:
                return new MockInvocations(type);
            case INTEGRATION:
                return new HttpServiceInvocations(id);
        }
        throw new UnsupportedOperationException("unreachable");
    }

    public boolean isAssignableTo(Field field) {
        return field.getType().isAssignableFrom(type);
    }

    private Object proxyInvoked(@SuppressWarnings("unused") Object proxy, Method method, Object... args) throws Exception {
        if (args == null) args = new Object[0];
        return invocations.invoke(method, args);
    }

    void done() { invocations.done(); }
}

package com.github.t1.wunderbar.junit;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstances;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Slf4j
class WunderBarJUnit implements Extension, BeforeEachCallback, AfterEachCallback {

    private TestInstances testInstances;
    private final List<Object> proxies = new ArrayList<>();
    private final List<Stub> stubs = new ArrayList<>();
    private final List<Stub> invocations = new ArrayList<>();

    @Override public void beforeEach(ExtensionContext context) {
        this.testInstances = context.getRequiredTestInstances();

        forEach(Service.class, this::proxy);

        forEach(SystemUnderTest.class, this::initSut);
    }

    private void forEach(Class<? extends Annotation> annotationClass, Consumer<Field> action) {
        testInstances.getAllInstances().stream()
            .flatMap(instance -> Stream.of(instance.getClass().getDeclaredFields()))
            .filter(field -> field.isAnnotationPresent(annotationClass))
            .forEach(action);
    }

    private void proxy(Field field) {
        var proxy = createProxy(field.getType());
        setField(instanceFor(field), field, proxy);
        this.proxies.add(proxy);
    }

    private Object instanceFor(Field field) {
        return testInstances.findInstance(field.getDeclaringClass()).orElseThrow();
    }

    private <T> T createProxy(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(getClassLoader(), new Class[]{type}, this::proxyInvoked));
    }

    private static ClassLoader getClassLoader() {
        var classLoader = Thread.currentThread().getContextClassLoader();
        return (classLoader == null) ? ClassLoader.getSystemClassLoader() : classLoader;
    }

    private Object proxyInvoked(Object proxy, Method method, Object... args) throws Exception {
        if (args == null) args = new Object[0];

        for (var stub : stubs)
            if (stub.matches(method, args))
                return invokeStub(stub);

        var stub = Stub.on(method, args);
        stubs.add(stub);
        OngoingStubbing.stub = stub;

        return null;
    }

    private Object invokeStub(Stub stub) throws Exception {
        invocations.add(stub);
        return stub.invoke();
    }


    private void initSut(Field field) {
        var testInstance = instanceFor(field);
        if (getField(testInstance, field) == null)
            setField(testInstance, field, newInstance(field));
        var systemUnderTest = getField(testInstance, field);
        Stream.of(field.getType().getDeclaredFields())
            .filter(Objects::nonNull)
            .forEach(targetField -> injectProxy(systemUnderTest, targetField));
    }

    private void injectProxy(Object instance, Field field) {
        proxies.stream()
            .filter(proxy -> field.getType().isAssignableFrom(proxy.getClass()))
            .forEach(proxy -> setField(instance, field, proxy));
    }


    @Override public void afterEach(ExtensionContext context) {
        if (OngoingStubbing.stub != null)
            throw new JUnitWunderBarException("unfinished stubbing of " + OngoingStubbing.stub);

        proxies.clear();

        if (!invocations.isEmpty()) {
            log.info("Invocations in {}", context.getDisplayName());
            invocations.forEach(invocation -> log.info("- {}", invocation));
        }
        invocations.clear();

        stubs.forEach(Stub::close);
        stubs.clear();
    }


    @SneakyThrows(ReflectiveOperationException.class)
    private Object newInstance(Field field) {
        return field.getType().getConstructor().newInstance();
    }

    @SneakyThrows(ReflectiveOperationException.class)
    private static Object getField(Object instance, Field field) {
        field.setAccessible(true);
        return field.get(instance);
    }

    @SneakyThrows(ReflectiveOperationException.class)
    private static void setField(Object instance, Field field, Object value) {
        field.setAccessible(true);
        field.set(instance, value);
    }
}

package com.github.t1.bar.junit;

import lombok.SneakyThrows;
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

class BarJUnit implements Extension, BeforeEachCallback, AfterEachCallback {

    private TestInstances testInstances;
    private final List<Object> mocks = new ArrayList<>();
    private final List<Stub> stubs = new ArrayList<>();
    private final List<Stub> invocations = new ArrayList<>();

    @Override public void beforeEach(ExtensionContext context) {
        this.testInstances = context.getRequiredTestInstances();

        forEach(Service.class, this::mock);

        forEach(SUT.class, this::initSut);
    }

    private void forEach(Class<? extends Annotation> annotationClass, Consumer<Field> action) {
        testInstances.getAllInstances().stream()
            .flatMap(instance -> Stream.of(instance.getClass().getDeclaredFields()))
            .filter(field -> field.isAnnotationPresent(annotationClass))
            .forEach(action);
    }

    private void mock(Field field) {
        Object mock = createMock(field.getType());
        setField(instanceFor(field), field, mock);
        this.mocks.add(mock);
    }

    private Object instanceFor(Field field) {
        return testInstances.findInstance(field.getDeclaringClass()).orElseThrow();
    }

    private <T> T createMock(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(getClassLoader(), new Class[]{type}, this::proxyInvoked));
    }

    private static ClassLoader getClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return (classLoader == null) ? ClassLoader.getSystemClassLoader() : classLoader;
    }

    private Object proxyInvoked(Object proxy, Method method, Object... args) throws Exception {
        for (Stub stub : stubs)
            if (stub.matches(method, args))
                return invokeStub(stub);

        Stub stub = new Stub(method, args);
        stubs.add(stub);
        BarOngoingStubbing.stub = stub;

        return null;
    }

    private Object invokeStub(Stub stub) throws Exception {
        invocations.add(stub);
        return stub.invoke();
    }


    private void initSut(Field field) {
        Object testInstance = instanceFor(field);
        if (getField(testInstance, field) == null)
            setField(testInstance, field, newInstance(field));
        Object sutInstance = getField(testInstance, field);
        Stream.of(field.getType().getDeclaredFields())
            .filter(Objects::nonNull)
            .forEach(targetField -> injectMock(sutInstance, targetField));
    }

    private void injectMock(Object instance, Field field) {
        mocks.stream()
            .filter(mock -> field.getType().isAssignableFrom(mock.getClass()))
            .forEach(mock -> setField(instance, field, mock));
    }


    @Override public void afterEach(ExtensionContext context) {
        if (BarOngoingStubbing.stub != null)
            throw new JUnitBarException("unfinished stubbing of " + BarOngoingStubbing.stub);

        if (!invocations.isEmpty()) {
            System.out.println("Invocations in " + context.getDisplayName());
            invocations.forEach(invocation -> System.out.println("- " + invocation));
        }

        invocations.clear();
        mocks.clear();
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

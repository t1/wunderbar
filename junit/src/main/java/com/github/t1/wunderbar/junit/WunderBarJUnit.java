package com.github.t1.wunderbar.junit;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import static java.time.temporal.ChronoUnit.NANOS;
import static java.util.Locale.ROOT;

@Slf4j
class WunderBarJUnit implements Extension, BeforeEachCallback, AfterEachCallback, AfterAllCallback {
    private static WunderBarExtension settings;

    private ExtensionContext context;
    private Instant start;
    private final List<Proxy> proxies = new ArrayList<>();

    @Override public void beforeEach(ExtensionContext context) {
        this.context = context;
        if (settings == null) init();

        forEachField(Service.class, this::proxy);

        forEachField(SystemUnderTest.class, this::initSut);

        log.info("==================== start {}", testId());
        start = Instant.now();
    }

    private void init() {
        settings = context.getRequiredTestInstances().getAllInstances().stream()
            .map(Object::getClass)
            .filter(testClass -> testClass.isAnnotationPresent(WunderBarExtension.class))
            .findFirst()
            .map(testClass -> testClass.getAnnotation(WunderBarExtension.class))
            .orElseThrow(() -> new JUnitWunderBarException("annotation not found: " + WunderBarExtension.class.getName()));
        log.info("start {} tests", settings.level().name().toLowerCase(ROOT));

        Bar.bar = new Bar(topContext().getDisplayName());
    }

    private ExtensionContext topContext() {
        ExtensionContext result = context;
        while (result.getParent().orElse(null) != context.getRoot())
            result = result.getParent().orElseThrow();
        return result;
    }

    private void forEachField(Class<? extends Annotation> annotationType, Consumer<Field> action) {
        context.getRequiredTestInstances().getAllInstances().stream()
            .flatMap(this::allFields)
            .filter(field -> field.isAnnotationPresent(annotationType))
            .forEach(action);
    }

    private Stream<Field> allFields(Object instance) {
        Builder<Field> builder = Stream.builder();
        for (Class<?> c = instance.getClass(); c != null; c = c.getSuperclass())
            Stream.of(c.getDeclaredFields()).forEach(builder::add);
        return builder.build();
    }

    private void proxy(Field field) {
        var proxy = new Proxy(settings, testId(), field.getType());
        setField(instanceFor(field), field, proxy.instance);
        this.proxies.add(proxy);
    }

    private String testId() {
        var top = topContext();
        var elements = new LinkedList<String>();
        for (ExtensionContext c = context; c != top && c.getParent().isPresent(); c = c.getParent().get())
            elements.push(c.getDisplayName().replaceAll("\\(\\)$", ""));
        return String.join("/", elements);
    }

    private Object instanceFor(Field field) {
        return context.getRequiredTestInstances().findInstance(field.getDeclaringClass()).orElseThrow();
    }

    private void initSut(Field sutField) {
        var testInstance = instanceFor(sutField);
        if (getField(testInstance, sutField) == null)
            setField(testInstance, sutField, newInstance(sutField));
        var systemUnderTest = getField(testInstance, sutField);
        injectProxiesIntoSut(systemUnderTest);
    }

    private void injectProxiesIntoSut(Object systemUnderTest) {
        Stream.of(systemUnderTest.getClass().getDeclaredFields())
            .filter(Objects::nonNull)
            .forEach(targetField -> injectProxy(systemUnderTest, targetField));
    }

    private void injectProxy(Object instance, Field field) {
        proxies.stream()
            .filter(proxy -> proxy.isAssignableTo(field))
            .forEach(proxy -> setField(instance, field, proxy.instance));
    }


    @Override public void afterEach(ExtensionContext context) {
        if (ExpectedResponseBuilder.buildingInvocation != null)
            throw new JUnitWunderBarException("unfinished stubbing of " + ExpectedResponseBuilder.buildingInvocation);

        proxies.forEach(Proxy::done);
        proxies.clear();

        log.info("{} took {} ms", testId(), Duration.between(start, Instant.now()).get(NANOS) / 1_000_000);
    }

    @Override public void afterAll(ExtensionContext context) {
        if (context == topContext()) done();
    }

    private void done() {
        settings = null;

        Bar.bar.close();
        Bar.bar = null;
    }


    @SneakyThrows(ReflectiveOperationException.class)
    private static Object newInstance(Field field) {
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

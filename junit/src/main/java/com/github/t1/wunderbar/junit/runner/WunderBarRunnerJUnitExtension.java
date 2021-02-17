package com.github.t1.wunderbar.junit.runner;

import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.http.HttpServerInteraction;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import static com.github.t1.wunderbar.junit.Utils.invoke;

class WunderBarRunnerJUnitExtension implements Extension, BeforeEachCallback, AfterEachCallback {
    static WunderBarRunnerJUnitExtension INSTANCE;
    WunderBarRunnerExtension settings;

    private ExtensionContext context;
    List<Consumer<List<HttpServerInteraction>>> beforeDynamicTestConsumers = new ArrayList<>();
    List<Consumer<List<HttpServerInteraction>>> afterDynamicTestConsumers = new ArrayList<>();

    @Override public void beforeEach(ExtensionContext context) {
        INSTANCE = this;
        this.context = context;
        this.settings = findSettings();

        addAllMethodsTo(BeforeDynamicTest.class, beforeDynamicTestConsumers);
        addAllMethodsTo(AfterDynamicTest.class, afterDynamicTestConsumers);
    }

    private WunderBarRunnerExtension findSettings() {
        return context.getRequiredTestInstances().getAllInstances().stream()
            .filter(test -> test.getClass().isAnnotationPresent(WunderBarRunnerExtension.class))
            .findFirst()
            .map(instance -> instance.getClass().getAnnotation(WunderBarRunnerExtension.class))
            .orElseThrow(() -> new WunderBarException("annotation not found: " + WunderBarRunnerExtension.class.getName()));
    }

    @Override public void afterEach(ExtensionContext context) {
        afterDynamicTestConsumers.clear();
        beforeDynamicTestConsumers.clear();
        this.settings = null;
        this.context = null;
        INSTANCE = null;
    }

    private void addAllMethodsTo(Class<? extends Annotation> annotationType, List<Consumer<List<HttpServerInteraction>>> consumers) {
        for (Object instance : context.getRequiredTestInstances().getAllInstances())
            allMethods(instance)
                .filter(method -> method.isAnnotationPresent(annotationType))
                .forEach(method -> consumers.add(list -> invoke(instance, method, list)));
    }

    private Stream<Method> allMethods(Object instance) {
        Builder<Method> builder = Stream.builder();
        for (Class<?> c = instance.getClass(); c != null; c = c.getSuperclass())
            Stream.of(c.getDeclaredMethods()).forEach(builder::add);
        return builder.build();
    }
}

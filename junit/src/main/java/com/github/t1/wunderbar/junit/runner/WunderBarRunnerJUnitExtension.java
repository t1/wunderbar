package com.github.t1.wunderbar.junit.runner;

import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.http.HttpServerInteraction;
import lombok.SneakyThrows;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import static com.github.t1.wunderbar.junit.Utils.invoke;

class WunderBarRunnerJUnitExtension implements Extension, BeforeEachCallback, AfterEachCallback {
    static WunderBarRunnerJUnitExtension INSTANCE;
    WunderBarRunner settings;

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

    private WunderBarRunner findSettings() {
        return context.getRequiredTestInstances().getAllInstances().stream()
            .filter(test -> test.getClass().isAnnotationPresent(WunderBarRunner.class))
            .findFirst()
            .map(instance -> instance.getClass().getAnnotation(WunderBarRunner.class))
            .or(this::findDeprecatedSettings)
            .orElseThrow(() -> new WunderBarException("annotation not found: " + WunderBarRunner.class.getName()));
    }

    @SuppressWarnings("removal")
    private Optional<? extends WunderBarRunner> findDeprecatedSettings() {
        return context.getRequiredTestInstances().getAllInstances().stream()
            .filter(test -> test.getClass().isAnnotationPresent(WunderBarRunnerExtension.class))
            .findFirst()
            .map(instance -> instance.getClass().getAnnotation(WunderBarRunnerExtension.class))
            .map(old -> new WunderBarRunner() {
                @Override public Class<? extends Annotation> annotationType() { return WunderBarRunnerExtension.class; }

                @Override public String baseUri() { return old.baseUri(); }
            });
    }

    private void addAllMethodsTo(Class<? extends Annotation> annotationType, List<Consumer<List<HttpServerInteraction>>> consumers) {
        for (Object instance : context.getRequiredTestInstances().getAllInstances())
            allMethods(instance)
                .filter(method -> method.isAnnotationPresent(annotationType))
                .forEach(method -> consumers.add(list -> invokeWith(instance, method, list)));
    }

    private void invokeWith(Object instance, Method method, List<HttpServerInteraction> list) {
        var args = new Object[method.getParameterCount()];
        for (int i = 0; i < args.length; i++) {
            if (method.getParameters()[i].getParameterizedType().getTypeName().equals("java.util.List<" + HttpServerInteraction.class.getName() + ">"))
                args[i] = list;
            else throw new WunderBarException("invalid argument type for parameter " + i + " of " + method);
        }
        invoke(instance, method, args);
    }

    private Stream<Method> allMethods(Object instance) {
        Builder<Method> builder = Stream.builder();
        for (Class<?> c = instance.getClass(); c != null; c = c.getSuperclass())
            Stream.of(c.getDeclaredMethods()).forEach(builder::add);
        return builder.build();
    }


    URI baseUri() {
        var baseUri = settings.baseUri();
        var matcher = FUNCTION.matcher(baseUri);
        if (matcher.matches())
            baseUri = matcher.group("prefix") + call(matcher.group("method")) + matcher.group("suffix");
        return URI.create(baseUri);
    }

    @SneakyThrows(ReflectiveOperationException.class)
    private String call(String methodName) {
        var instance = context.getRequiredTestInstance();
        var method = instance.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(instance).toString();
    }


    @Override public void afterEach(ExtensionContext context) {
        afterDynamicTestConsumers.clear();
        beforeDynamicTestConsumers.clear();
        this.settings = null;
        this.context = null;
        INSTANCE = null;
    }

    private static final Pattern FUNCTION = Pattern.compile("(?<prefix>.*)\\{(?<method>.*)\\(\\)}(?<suffix>.*)");
}

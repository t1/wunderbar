package com.github.t1.wunderbar.junit.provider;

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

class WunderBarApiProviderJUnitExtension implements Extension, BeforeEachCallback, AfterEachCallback {
    static WunderBarApiProviderJUnitExtension INSTANCE;
    WunderBarApiProvider settings;

    private ExtensionContext context;
    List<Consumer<List<HttpServerInteraction>>> beforeDynamicTestConsumers = new ArrayList<>();
    List<Consumer<HttpServerInteraction>> beforeInteractionConsumers = new ArrayList<>();
    List<Consumer<HttpServerInteraction>> afterInteractionConsumers = new ArrayList<>();
    List<Consumer<List<HttpServerInteraction>>> afterDynamicTestConsumers = new ArrayList<>();

    @Override public void beforeEach(ExtensionContext context) {
        INSTANCE = this;
        this.context = context;
        this.settings = findSettings();

        addAllListMethodsTo(BeforeDynamicTest.class, beforeDynamicTestConsumers);
        addAllMethodsTo(BeforeInteraction.class, beforeInteractionConsumers);
        addAllMethodsTo(AfterInteraction.class, afterInteractionConsumers);
        addAllListMethodsTo(AfterDynamicTest.class, afterDynamicTestConsumers);
    }

    private WunderBarApiProvider findSettings() {
        return context.getRequiredTestInstances().getAllInstances().stream()
            .filter(test -> test.getClass().isAnnotationPresent(WunderBarApiProvider.class))
            .findFirst()
            .map(instance -> instance.getClass().getAnnotation(WunderBarApiProvider.class))
            .or(this::findDeprecatedSettings)
            .orElseThrow(() -> new WunderBarException("annotation not found: " + WunderBarApiProvider.class.getName()));
    }

    @SuppressWarnings("removal")
    private Optional<? extends WunderBarApiProvider> findDeprecatedSettings() {
        return context.getRequiredTestInstances().getAllInstances().stream()
            .filter(test -> test.getClass().isAnnotationPresent(WunderBarRunnerExtension.class))
            .findFirst()
            .map(instance -> instance.getClass().getAnnotation(WunderBarRunnerExtension.class))
            .map(old -> new WunderBarApiProvider() {
                @Override public Class<? extends Annotation> annotationType() { return WunderBarRunnerExtension.class; }

                @Override public String baseUri() { return old.baseUri(); }
            });
    }

    private void addAllMethodsTo(Class<? extends Annotation> annotationType, List<Consumer<HttpServerInteraction>> consumers) {
        for (Object instance : context.getRequiredTestInstances().getAllInstances())
            allMethods(instance)
                .filter(method -> method.isAnnotationPresent(annotationType))
                .forEach(method -> consumers.add(interaction -> invokeWith(instance, method, interaction)));
    }

    private void invokeWith(Object instance, Method method, HttpServerInteraction interaction) {
        var args = new Object[method.getParameterCount()];
        for (int i = 0; i < args.length; i++) {
            if (method.getParameters()[i].getType().equals(HttpServerInteraction.class))
                args[i] = interaction;
            else throw new WunderBarException("invalid argument type for parameter " + i + " of " + method);
        }
        invoke(instance, method, args);
    }

    private void addAllListMethodsTo(Class<? extends Annotation> annotationType, List<Consumer<List<HttpServerInteraction>>> consumers) {
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

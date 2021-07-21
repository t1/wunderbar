package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import lombok.SneakyThrows;
import lombok.Value;
import org.assertj.core.api.BDDSoftAssertions;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import static com.github.t1.wunderbar.junit.Utils.invoke;

class WunderBarApiProviderJUnitExtension implements Extension, BeforeEachCallback, AfterEachCallback {
    static WunderBarApiProviderJUnitExtension INSTANCE;
    WunderBarApiProvider settings;

    private ExtensionContext context;
    List<Consumer<List<HttpInteraction>>> beforeDynamicTestMethods = new ArrayList<>();
    List<Function<HttpInteraction, Object>> beforeInteractionMethods = new ArrayList<>();
    List<Function<HttpInteraction, Object>> afterInteractionMethods = new ArrayList<>();
    List<Consumer<OnInteractionErrorParams>> onInteractionErrorMethods = new ArrayList<>();
    List<Consumer<List<HttpInteraction>>> afterDynamicTestMethods = new ArrayList<>();

    @Override public void beforeEach(ExtensionContext context) {
        INSTANCE = this;
        this.context = context;
        this.settings = findSettings();

        addAllListMethodsTo(BeforeDynamicTest.class, beforeDynamicTestMethods);
        addAllMethodsTo(BeforeInteraction.class, beforeInteractionMethods);
        addAllMethodsTo(AfterInteraction.class, afterInteractionMethods);
        addAllOnInteractionErrorMethods();
        addAllListMethodsTo(AfterDynamicTest.class, afterDynamicTestMethods);

        if (onInteractionErrorMethods.isEmpty())
            onInteractionErrorMethods.add(this::defaultOnInteractionError);
    }

    private WunderBarApiProvider findSettings() {
        return context.getRequiredTestInstances().getAllInstances().stream()
            .filter(test -> test.getClass().isAnnotationPresent(WunderBarApiProvider.class))
            .findFirst()
            .map(instance -> instance.getClass().getAnnotation(WunderBarApiProvider.class))
            .orElseThrow(() -> new WunderBarException("annotation not found: " + WunderBarApiProvider.class.getName()));
    }

    private void addAllMethodsTo(Class<? extends Annotation> annotationType, List<Function<HttpInteraction, Object>> consumers) {
        for (Object instance : context.getRequiredTestInstances().getAllInstances())
            allMethods(instance)
                .filter(method -> method.isAnnotationPresent(annotationType))
                .forEach(method -> consumers.add(interaction -> invokeWith(instance, method, interaction)));
    }

    private Object invokeWith(Object instance, Method method, HttpInteraction interaction) {
        var args = new Object[method.getParameterCount()];
        for (int i = 0; i < args.length; i++) {
            if (method.getParameters()[i].getType().equals(HttpInteraction.class))
                args[i] = interaction;
            else throw new WunderBarException("invalid argument type for parameter " + i + " of " + method);
        }
        return invoke(instance, method, args);
    }

    private void addAllListMethodsTo(Class<? extends Annotation> annotationType, List<Consumer<List<HttpInteraction>>> consumers) {
        for (Object instance : context.getRequiredTestInstances().getAllInstances())
            allMethods(instance)
                .filter(method -> method.isAnnotationPresent(annotationType))
                .forEach(method -> consumers.add(list -> invokeWith(instance, method, list)));
    }

    private void invokeWith(Object instance, Method method, List<HttpInteraction> list) {
        var args = new Object[method.getParameterCount()];
        for (int i = 0; i < args.length; i++) {
            if (method.getParameters()[i].getParameterizedType().getTypeName().equals("java.util.List<" + HttpInteraction.class.getName() + ">"))
                args[i] = list;
            else throw new WunderBarException("invalid argument type for parameter " + i + " of " + method);
        }
        invoke(instance, method, args);
    }

    private void addAllOnInteractionErrorMethods() {
        for (Object instance : context.getRequiredTestInstances().getAllInstances())
            allMethods(instance)
                .filter(method -> method.isAnnotationPresent(OnInteractionError.class))
                .forEach(method -> onInteractionErrorMethods.add(params -> invokeWith(instance, method, params)));
    }

    private void invokeWith(Object instance, Method method, OnInteractionErrorParams params) {
        var args = new Object[method.getParameterCount()];
        for (int i = 0; i < args.length; i++) {
            if (method.getParameters()[i].getType().equals(HttpInteraction.class))
                args[i] = params.getInteraction();
            else if (method.getParameters()[i].getType().equals(HttpResponse.class))
                args[i] = params.getActual();
            else if (method.getParameters()[i].getType().equals(BDDSoftAssertions.class))
                args[i] = params.getAssertions();
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


    private void defaultOnInteractionError(OnInteractionErrorParams params) { params.assertions.assertAll(); }


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
        afterDynamicTestMethods.clear();
        beforeDynamicTestMethods.clear();
        this.settings = null;
        this.context = null;
        INSTANCE = null;
    }

    private static final Pattern FUNCTION = Pattern.compile("(?<prefix>.*)\\{(?<method>.*)\\(\\)}(?<suffix>.*)");

    static @Value class OnInteractionErrorParams {
        HttpInteraction interaction;
        HttpResponse actual;
        BDDSoftAssertions assertions;
    }
}

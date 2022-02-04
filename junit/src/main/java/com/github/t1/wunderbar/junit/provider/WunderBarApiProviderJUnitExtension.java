package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.junit.WunderBarException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import static com.github.t1.wunderbar.junit.provider.OnInteractionErrorMethodHandler.DEFAULT_ON_INTERACTION_ERROR;

class WunderBarApiProviderJUnitExtension implements Extension, BeforeEachCallback, AfterEachCallback {
    static WunderBarApiProviderJUnitExtension INSTANCE;
    private WunderBarApiProvider settings;

    private ExtensionContext context;
    List<BeforeDynamicTestMethodHandler> beforeDynamicTestMethods = new ArrayList<>();
    List<BeforeInteractionMethodHandler> beforeInteractionMethods = new ArrayList<>();
    List<AfterInteractionMethodHandler> afterInteractionMethods = new ArrayList<>();
    List<OnInteractionErrorMethodHandler> onInteractionErrorMethods = new ArrayList<>();
    List<AfterDynamicTestMethodHandler> afterDynamicTestMethods = new ArrayList<>();

    @Override public void beforeEach(ExtensionContext context) {
        INSTANCE = this;
        this.context = context;
        this.settings = findSettings();

        scanForBeforeDynamicTestMethods();
        scanForBeforeInteractionMethods();
        scanForAfterInteractionMethods();
        scanForOnInteractionErrorMethods();
        scanForAfterDynamicTestMethods();
    }

    private WunderBarApiProvider findSettings() {
        return context.getRequiredTestInstances().getAllInstances().stream()
            .filter(test -> test.getClass().isAnnotationPresent(WunderBarApiProvider.class))
            .findFirst()
            .map(instance -> instance.getClass().getAnnotation(WunderBarApiProvider.class))
            .orElseThrow(() -> new WunderBarException("annotation not found: " + WunderBarApiProvider.class.getName()));
    }

    private void scanForBeforeDynamicTestMethods() {
        for (Object instance : context.getRequiredTestInstances().getAllInstances())
            allMethods(instance, BeforeDynamicTest.class)
                .forEach(method -> beforeDynamicTestMethods.add(new BeforeDynamicTestMethodHandler(instance, method)));
    }

    private void scanForBeforeInteractionMethods() {
        for (Object instance : context.getRequiredTestInstances().getAllInstances())
            allMethods(instance, BeforeInteraction.class)
                .forEach(method -> beforeInteractionMethods.add(new BeforeInteractionMethodHandler(instance, method)));
    }

    private void scanForAfterInteractionMethods() {
        for (Object instance : context.getRequiredTestInstances().getAllInstances())
            allMethods(instance, AfterInteraction.class)
                .forEach(method -> afterInteractionMethods.add(new AfterInteractionMethodHandler(instance, method)));
    }

    private void scanForOnInteractionErrorMethods() {
        for (Object instance : context.getRequiredTestInstances().getAllInstances())
            allMethods(instance, OnInteractionError.class)
                .forEach(method -> onInteractionErrorMethods.add(new OnInteractionErrorMethodHandler(instance, method)));
        if (onInteractionErrorMethods.isEmpty())
            onInteractionErrorMethods.add(new OnInteractionErrorMethodHandler(null, DEFAULT_ON_INTERACTION_ERROR));
    }

    private void scanForAfterDynamicTestMethods() {
        for (Object instance : context.getRequiredTestInstances().getAllInstances())
            allMethods(instance, AfterDynamicTest.class)
                .forEach(method -> afterDynamicTestMethods.add(new AfterDynamicTestMethodHandler(instance, method)));
    }

    private Stream<Method> allMethods(Object instance, Class<? extends Annotation> annotationType) {
        Builder<Method> builder = Stream.builder();
        for (Class<?> c = instance.getClass(); c != null; c = c.getSuperclass())
            Stream.of(c.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(annotationType))
                .sorted(ORDER)
                .forEach(builder::add);
        return builder.build();
    }

    private static final Comparator<Method> ORDER = Comparator.comparingInt(method ->
        method.isAnnotationPresent(Order.class) ? method.getAnnotation(Order.class).value() : Order.DEFAULT);


    URI baseUri() {
        var baseUri = settings.baseUri();
        var matcher = FUNCTION.matcher(baseUri);
        if (matcher.matches())
            baseUri = matcher.group("prefix") + call(matcher.group("method")) + matcher.group("suffix");
        return URI.create(baseUri);
    }

    private static final Pattern FUNCTION = Pattern.compile("(?<prefix>.*)\\{(?<method>.*)\\(\\)}(?<suffix>.*)");

    @SneakyThrows(ReflectiveOperationException.class)
    private String call(String methodName) {
        var instance = context.getRequiredTestInstance();
        var method = instance.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(instance).toString();
    }

    @Override public void afterEach(ExtensionContext context) {
        afterDynamicTestMethods.clear();
        onInteractionErrorMethods.clear();
        afterInteractionMethods.clear();
        beforeInteractionMethods.clear();
        beforeDynamicTestMethods.clear();
        this.settings = null;
        this.context = null;
        INSTANCE = null;
    }
}

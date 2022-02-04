package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.http.HttpClient;
import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.Test;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.function.Executable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import static com.github.t1.wunderbar.junit.provider.OnInteractionErrorMethodHandler.DEFAULT_ON_INTERACTION_ERROR;
import static java.util.stream.Collectors.toList;

class WunderBarApiProviderJUnitExtension implements Extension, BeforeEachCallback, AfterEachCallback {
    static WunderBarApiProviderJUnitExtension INSTANCE;
    private WunderBarApiProvider settings;

    private ExtensionContext context;
    private final List<BeforeDynamicTestMethodHandler> beforeDynamicTestMethods = new ArrayList<>();
    private final List<BeforeInteractionMethodHandler> beforeInteractionMethods = new ArrayList<>();
    private final List<AfterInteractionMethodHandler> afterInteractionMethods = new ArrayList<>();
    private final List<OnInteractionErrorMethodHandler> onInteractionErrorMethods = new ArrayList<>();
    private final List<AfterDynamicTestMethodHandler> afterDynamicTestMethods = new ArrayList<>();

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

    public static Executable createExecutable(List<HttpInteraction> interactions, Test test) {return new HttpBarExecutable(interactions, test);}

    @AllArgsConstructor
    private static class HttpBarExecutable implements Executable, Executions {
        @Getter @Setter private List<HttpInteraction> interactions;
        @Getter private final Test test;

        private final WunderBarApiProviderJUnitExtension extension = INSTANCE;
        private final HttpClient httpClient = new HttpClient(extension.baseUri());

        @Override public String toString() {
            return getDisplayName() + " [with " + test.getInteractionCount() + " tests]";
        }

        @Override public String getDisplayName() {return test.getDisplayName();}

        @Override public void execute() {
            System.out.println("==================== start " + test);
            extension.beforeDynamicTestMethods.forEach(handler -> handler.invoke(this));

            var actuals = new ArrayList<HttpResponse>();
            for (HttpInteraction interaction : interactions) {
                System.out.println("=> execute " + interaction.getNumber() + " of " + test.getInteractionCount());
                actuals.add(new ExecutionImpl(interaction).run());
            }

            System.out.println("=> cleanup " + test);
            extension.afterDynamicTestMethods.forEach(consumer -> consumer.invoke(this));
        }

        private class ExecutionImpl implements Execution {
            @Getter HttpInteraction expected;
            @Getter @Setter HttpResponse actual;

            public ExecutionImpl(HttpInteraction expected) {
                this.expected = expected;
            }

            @Override public String toString() {
                return getDisplayName() + "[" + expected.getNumber() + "/" + test.getInteractionCount() + "]";
            }

            @Override public String getDisplayName() {return test.getDisplayName();}

            private HttpResponse run() {
                extension.beforeInteractionMethods.forEach(handler -> handler.invoke(this));
                var numbering = expected.getNumber() + "/" + test.getInteractionCount();
                System.out.println("-- actual request " + numbering + ":\n" + expected.getRequest() + "\n");

                this.actual = httpClient.send(expected.getRequest()).withFormattedBody();

                System.out.println("-- actual response " + numbering + ":\n" + actual + "\n");
                extension.afterInteractionMethods.forEach(consumer -> consumer.invoke(this));
                extension.onInteractionErrorMethods.forEach(consumer -> consumer.invoke(this));

                return actual;
            }

            @Override public void expect(Function<HttpInteraction, HttpInteraction> function) {expected = function.apply(expected);}
        }
    }

    interface Executions extends WunderBarExecutions {
        List<HttpInteraction> getInteractions();

        default List<HttpRequest> getExpectedRequests() {return mapExpectedResponses(HttpInteraction::getRequest);}

        default List<HttpResponse> getExpectedResponses() {return mapExpectedResponses(HttpInteraction::getResponse);}

        default <T> List<T> mapExpectedResponses(Function<HttpInteraction, T> function) {
            return getInteractions().stream().map(function).collect(toList());
        }

        void setInteractions(List<HttpInteraction> interactions);
    }

    interface Execution extends WunderBarExecution {
        HttpInteraction getExpected();
        HttpResponse getActual();

        void expect(Function<HttpInteraction, HttpInteraction> function);
        void setActual(HttpResponse actual);
    }
}

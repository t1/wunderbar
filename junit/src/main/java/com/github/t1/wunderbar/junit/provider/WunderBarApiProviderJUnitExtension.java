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
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.function.Executable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import static com.github.t1.wunderbar.common.Utils.invoke;
import static com.github.t1.wunderbar.junit.JunitUtils.ORDER;
import static com.github.t1.wunderbar.junit.provider.OnInteractionErrorMethodHandler.DEFAULT_ON_INTERACTION_ERROR;

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
        return invoke(instance, method).toString();
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

    Executable createExecutable(List<HttpInteraction> interactions, Test test) {return new HttpBarExecutable(interactions, test);}

    @AllArgsConstructor
    private class HttpBarExecutable implements Executable, Executions {
        @Getter @Setter private List<HttpInteraction> interactions;
        @Getter private final Test test;
        @Getter private final List<HttpResponse> actualResponses = new ArrayList<>();

        private final HttpClient httpClient = new HttpClient(baseUri());

        @Override public String toString() {
            return getDisplayName() + " [with " + getInteractionCount() + " tests]";
        }

        @Override public int getInteractionCount() {
            return test.interactionCount();
        }

        @Override public String getDisplayName() {return test.getDisplayName();}

        @Override public void execute() {
            System.out.println("==================== start " + test);
            beforeDynamicTestMethods.forEach(handler -> handler.invoke(this));

            for (HttpInteraction interaction : interactions) {
                System.out.println("=> execute " + interaction.getNumber() + " of " + test.interactionCount());
                actualResponses.add(new ExecutionImpl(interaction).run());
            }

            System.out.println("=> cleanup " + test);
            afterDynamicTestMethods.forEach(consumer -> consumer.invoke(this));
        }

        @Getter private class ExecutionImpl implements Execution {
            HttpInteraction expected;
            @Setter HttpResponse actual;

            public ExecutionImpl(HttpInteraction expected) {
                this.expected = expected;
            }

            @Override public String toString() {
                return getDisplayName() + "[" + getInteractionNumber() + "/" + getInteractionCount() + "]";
            }

            @Override public String getDisplayName() {return test.getDisplayName();}

            @Override public int getInteractionNumber() {return expected.getNumber();}

            @Override public int getInteractionCount() {return test.interactionCount();}

            private HttpResponse run() {
                beforeInteractionMethods.forEach(handler -> handler.invoke(this));
                var numbering = expected.getNumber() + "/" + test.interactionCount();
                System.out.println("-- actual request " + numbering + ":\n" + expected.getRequest() + "\n");

                this.actual = httpClient.send(expected.getRequest()).withFormattedBody();

                System.out.println("-- actual response " + numbering + ":\n" + actual + "\n");
                afterInteractionMethods.forEach(consumer -> consumer.invoke(this));
                onInteractionErrorMethods.forEach(consumer -> consumer.invoke(this));

                return actual;
            }

            @Override
            public void expect(Function<HttpInteraction, HttpInteraction> function) {expected = function.apply(expected);}
        }
    }

    interface Executions extends WunderBarExecutions {
        List<HttpInteraction> getInteractions();

        default List<HttpRequest> getExpectedRequests() {return mapExpectedResponses(HttpInteraction::getRequest);}

        default List<HttpResponse> getExpectedResponses() {return mapExpectedResponses(HttpInteraction::getResponse);}

        default <T> List<T> mapExpectedResponses(Function<HttpInteraction, T> function) {
            return getInteractions().stream().map(function).toList();
        }

        List<HttpResponse> getActualResponses();


        void setInteractions(List<HttpInteraction> interactions);
    }

    interface Execution extends WunderBarExecution {
        HttpInteraction getExpected();

        HttpResponse getActual();

        void expect(Function<HttpInteraction, HttpInteraction> function);
    }
}

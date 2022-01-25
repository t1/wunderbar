package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.junit.http.HttpClient;
import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProviderJUnitExtension.OnInteractionErrorParams;
import com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.Test;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.function.Executable;

import java.util.function.Function;

@RequiredArgsConstructor
class HttpBarExecutable implements Executable {

    public static HttpBarExecutable of(BarReader bar, Test test) {return new HttpBarExecutable(bar, test);}

    private final BarReader bar;
    private final Test test;

    private final WunderBarApiProviderJUnitExtension extension = WunderBarApiProviderJUnitExtension.INSTANCE;
    private final HttpClient httpClient = new HttpClient(extension.baseUri());

    @Override public void execute() {
        var interactions = bar.interactionsFor(test);

        System.out.println("==================== start " + test);
        extension.beforeDynamicTestMethods.forEach(consumer -> consumer.accept(interactions));

        for (var interaction : interactions)
            new Execution(interaction).run();

        extension.afterDynamicTestMethods.forEach(consumer -> consumer.accept(interactions));
    }

    @AllArgsConstructor
    private class Execution {
        HttpInteraction expected;

        private void run() {
            extension.beforeInteractionMethods.forEach(this::applyInteractionMethods);
            System.out.println("-- request " + expected.getNumber() + ":\n" + expected.getRequest() + "\n");

            HttpResponse actual = httpClient.send(expected.getRequest());

            System.out.println("-- actual response " + expected.getNumber() + ":\n" + actual + "\n");
            extension.afterInteractionMethods.forEach(consumer -> consumer.apply(expected));
            var onErrorParams = new OnInteractionErrorParams(expected, actual);
            extension.onInteractionErrorMethods.forEach(consumer -> consumer.accept(onErrorParams));
        }

        private void applyInteractionMethods(Function<HttpInteraction, Object> consumer) {
            var result = consumer.apply(expected);
            if (result instanceof HttpInteraction) expected = (HttpInteraction) result;
            if (result instanceof HttpRequest) expected = expected.withRequest((HttpRequest) result);
            if (result instanceof HttpResponse) expected = expected.withResponse((HttpResponse) result);
        }
    }
}

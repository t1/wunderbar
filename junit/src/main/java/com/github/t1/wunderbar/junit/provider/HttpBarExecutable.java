package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.junit.WunderBarException;
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

        for (var interaction : interactions) {
            System.out.println("=> execute " + interaction.getNumber() + " of " + test.getInteractionCount());
            new Execution(interaction).run();
        }

        System.out.println("=> cleanup " + test);
        extension.afterDynamicTestMethods.forEach(consumer -> consumer.accept(interactions));
    }

    @AllArgsConstructor
    private class Execution {
        HttpInteraction expected;

        private void run() {
            extension.beforeInteractionMethods.forEach(this::applyInteractionMethods);
            var numbering = expected.getNumber() + "/" + test.getInteractionCount();
            System.out.println("-- actual request " + numbering + ":\n" + expected.getRequest() + "\n");

            HttpResponse actual = httpClient.send(expected.getRequest()).withFormattedBody();

            System.out.println("-- actual response " + numbering + ":\n" + actual + "\n");
            extension.afterInteractionMethods.forEach(consumer -> consumer.apply(expected));
            var onErrorParams = new OnInteractionErrorParams(expected, actual);
            extension.onInteractionErrorMethods.forEach(consumer -> consumer.accept(onErrorParams));
        }

        private void applyInteractionMethods(Function<HttpInteraction, Object> consumer) {
            var result = consumer.apply(expected);
            if (result instanceof HttpInteraction) expected = (HttpInteraction) result;
            else if (result instanceof HttpRequest) expected = expected.withRequest((HttpRequest) result);
            else if (result instanceof HttpResponse) expected = expected.withResponse((HttpResponse) result);
            else if (result != null) throw new WunderBarException("unexpected return type " + result.getClass()); // TODO test (+ null)
        }
    }
}

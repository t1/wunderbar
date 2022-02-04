package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.junit.http.HttpClient;
import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.Test;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
class HttpBarExecutable implements Executable {
    private List<HttpInteraction> interactions;
    private final Test test;

    private final WunderBarApiProviderJUnitExtension extension = WunderBarApiProviderJUnitExtension.INSTANCE;
    private final HttpClient httpClient = new HttpClient(extension.baseUri());

    @Override public void execute() {
        System.out.println("==================== start " + test);
        extension.beforeDynamicTestMethods.forEach(handler -> interactions = handler.invoke(test, interactions));

        var actuals = new ArrayList<HttpResponse>();
        for (var interaction : interactions) {
            System.out.println("=> execute " + interaction.getNumber() + " of " + test.getInteractionCount());
            actuals.add(new Execution(interaction).run());
        }

        System.out.println("=> cleanup " + test);
        extension.afterDynamicTestMethods.forEach(consumer -> consumer.invoke(interactions, actuals));
    }

    class Execution {
        HttpInteraction expected;
        HttpResponse actual;

        public Execution(HttpInteraction expected) {
            this.expected = expected;
        }

        private HttpResponse run() {
            extension.beforeInteractionMethods.forEach(handler -> expected = handler.invoke(test, expected));
            var numbering = expected.getNumber() + "/" + test.getInteractionCount();
            System.out.println("-- actual request " + numbering + ":\n" + expected.getRequest() + "\n");

            this.actual = httpClient.send(expected.getRequest()).withFormattedBody();

            System.out.println("-- actual response " + numbering + ":\n" + actual + "\n");
            extension.afterInteractionMethods.forEach(consumer -> consumer.invoke(this));
            extension.onInteractionErrorMethods.forEach(consumer -> consumer.invoke(expected, actual));

            return actual;
        }
    }
}

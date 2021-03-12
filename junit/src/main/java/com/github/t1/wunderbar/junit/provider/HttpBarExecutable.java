package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.junit.http.HttpClient;
import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProviderJUnitExtension.OnInteractionErrorParams;
import com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.Test;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.BDDSoftAssertions;
import org.junit.jupiter.api.function.Executable;

import javax.json.Json;
import javax.json.JsonPointer;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import java.util.function.Function;

import static com.github.t1.wunderbar.junit.provider.CustomBDDAssertions.then;

@RequiredArgsConstructor
class HttpBarExecutable implements Executable {

    public static HttpBarExecutable of(BarReader bar, Test test) { return new HttpBarExecutable(bar, test); }

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
        HttpInteraction interaction;

        public void run() {
            extension.beforeInteractionMethods.forEach(this::apply);
            System.out.println("-- request " + interaction.getNumber() + ":\n" + interaction.getRequest() + "\n");

            HttpResponse actual = httpClient.send(interaction.getRequest());

            System.out.println("-- actual response " + interaction.getNumber() + ":\n" + actual + "\n");
            extension.afterInteractionMethods.forEach(consumer -> consumer.apply(interaction));
            var assertions = checkResponse(actual, interaction.getResponse());
            var onErrorParams = new OnInteractionErrorParams(interaction, actual, assertions);
            extension.onInteractionErrorMethods.forEach(consumer -> consumer.accept(onErrorParams));
        }

        private void apply(Function<HttpInteraction, Object> consumer) {
            var result = consumer.apply(interaction);
            if (result instanceof HttpInteraction) interaction = (HttpInteraction) result;
            if (result instanceof HttpRequest) interaction = interaction.withRequest((HttpRequest) result);
            if (result instanceof HttpResponse) interaction = interaction.withResponse((HttpResponse) result);
        }


        private BDDSoftAssertions checkResponse(HttpResponse actual, HttpResponse expected) {
            var softly = new BDDSoftAssertions();
            softly.then(actual.getStatusString()).describedAs("status").isEqualTo(expected.getStatusString());
            softly.then(actual.getContentType().isCompatible(expected.getContentType()))
                .describedAs("Content-Type: " + expected.getContentType() + " to be compatible to " + actual.getContentType())
                .isTrue();
            checkBody(softly, body(actual), body(expected));
            return softly;
        }

        private JsonValue body(HttpResponse actual) { return actual.getJsonBody().orElse(JsonValue.NULL); }

        private void checkBody(BDDSoftAssertions softly, JsonValue actual, JsonValue expected) {
            if (actual instanceof JsonStructure && expected instanceof JsonStructure)
                checkForUnexpectedErrors(softly, (JsonStructure) actual, (JsonStructure) expected);
            softly.check(() -> then(actual).isEqualToIgnoringNewFields(expected));
        }

        /** GraphQL: show unexpected errors in addition to the missing or only partial data */
        private void checkForUnexpectedErrors(BDDSoftAssertions softly, JsonStructure actual, JsonStructure expected) {
            if (ERRORS.containsValue(actual) && !ERRORS.containsValue(expected))
                softly.then(ERRORS.getValue(actual)).as("errors").isNull();
        }
    }

    private static final JsonPointer ERRORS = Json.createPointer("/errors");
}

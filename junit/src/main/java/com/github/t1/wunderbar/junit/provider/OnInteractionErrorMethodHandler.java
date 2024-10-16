package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.common.Utils;
import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProviderJUnitExtension.Execution;
import jakarta.json.Json;
import jakarta.json.JsonPointer;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.assertj.core.api.BDDSoftAssertions;

import java.lang.reflect.Method;

import static com.github.t1.wunderbar.junit.assertions.WunderBarBDDAssertions.then;
import static com.github.t1.wunderbar.junit.http.HttpUtils.isCompatible;

@RequiredArgsConstructor
class OnInteractionErrorMethodHandler {
    static final Method DEFAULT_ON_INTERACTION_ERROR;

    static {
        try {
            DEFAULT_ON_INTERACTION_ERROR = OnInteractionErrorMethodHandler.class.getDeclaredMethod("defaultOnInteractionError", BDDSoftAssertions.class);
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException();
        }
    }

    private static void defaultOnInteractionError(BDDSoftAssertions assertions) {assertions.assertAll();}


    private final Object instance;
    private final Method method;

    public void invoke(Execution execution) {
        var params = new OnInteractionErrorParams(execution);
        Object[] args = args(params);
        var result = Utils.invoke(instance, method, args);
        if (result != null) throw new WunderBarException("unexpected return type " + result.getClass()); // TODO test
    }

    private Object[] args(OnInteractionErrorParams params) {
        var args = new Object[method.getParameterCount()];
        for (int i = 0; i < args.length; i++) {
            Class<?> type = method.getParameters()[i].getType();
            if (type.equals(HttpInteraction.class))
                args[i] = params.execution.getExpected();
            else if (type.equals(HttpRequest.class))
                args[i] = params.execution.getExpected().getRequest();
            else if (type.equals(HttpResponse.class))
                args[i] = params.execution.getExpected().getResponse();
            else if (type.equals(BDDSoftAssertions.class))
                args[i] = params.getAssertions();
            else if (type.equals(WunderBarExecution.class))
                args[i] = params.getExecution();
            else
                throw new WunderBarException("invalid argument type " + type + " for parameter " + i + " of " + method);
        }
        return args;
    }

    static @Value class OnInteractionErrorParams {
        Execution execution;
        BDDSoftAssertions assertions = new BDDSoftAssertions();

        OnInteractionErrorParams(Execution execution) {
            this.execution = execution;
            checkResponse();
        }

        private void checkResponse() {
            var expected = execution.getExpected().getResponse();
            var actual = execution.getActual();
            assertions.then(actual.getStatusString()).describedAs("status").isEqualTo(expected.getStatusString());
            assertions.then(isCompatible(actual.getContentType(), expected.getContentType()))
                    .describedAs("Content-Type: " + actual.getContentType() + " to be compatible to " + expected.getContentType())
                    .isTrue();
            checkBody(body(actual), body(expected));
        }

        private JsonValue body(HttpResponse response) {
            if (response.isProblemDetail())
                response = withoutVolatileProblemDetails(response);
            return response.json().orElse(JsonValue.NULL);
        }

        /**
         * The Problem Details fields <code>title</code> and <code>detail</code> are meant for human readability
         * and, like <code>instance</code>, explicitly allowed to change between calls; don't check those
         */
        private HttpResponse withoutVolatileProblemDetails(HttpResponse response) {
            return response.with(json -> json
                    .remove("title")
                    .remove("detail")
                    .remove("instance")
            ).withFormattedBody();
        }

        private void checkBody(JsonValue actual, JsonValue expected) {
            if (actual instanceof JsonStructure && expected instanceof JsonStructure)
                checkForUnexpectedErrors((JsonStructure) actual, (JsonStructure) expected);
            assertions.check(() -> then(actual).isEqualToIgnoringNewFields(expected));
        }

        /** GraphQL: show unexpected errors in addition to the missing or only partial data */
        private void checkForUnexpectedErrors(JsonStructure actual, JsonStructure expected) {
            if (ERRORS.containsValue(actual) && !ERRORS.containsValue(expected))
                assertions.then(ERRORS.getValue(actual)).as("errors").isNull();
        }

        private static final JsonPointer ERRORS = Json.createPointer("/errors");
    }
}

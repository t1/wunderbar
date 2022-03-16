package test.provider;

import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.AfterDynamicTest;
import com.github.t1.wunderbar.junit.provider.AfterInteraction;
import com.github.t1.wunderbar.junit.provider.BeforeDynamicTest;
import com.github.t1.wunderbar.junit.provider.BeforeInteraction;
import com.github.t1.wunderbar.junit.provider.OnInteractionError;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProvider;
import org.assertj.core.api.BDDSoftAssertions;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;
import test.MockServer;
import test.NonCI;

import java.util.List;

import static com.github.t1.wunderbar.common.mock.GraphQLResponseBuilder.graphQlError;
import static com.github.t1.wunderbar.junit.assertions.WunderBarBDDAssertions.then;
import static com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.findTestsIn;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;

@NonCI
@WunderBarApiProvider(baseUri = "{endpoint()}")
class FailingAT {
    @RegisterExtension static MockServer mockServer = new MockServer();
    @RegisterExtension ExpectationsExtension expectations = new ExpectationsExtension();

    @SuppressWarnings("unused")
    String endpoint() {return mockServer.baseUri() + "/dummy-mock-server";}

    @BeforeDynamicTest void shouldFailBeforeToModifyPassedInteractions(List<HttpInteraction> interactions) {
        var throwable = catchThrowable(() -> interactions.remove(0));

        then(throwable).isInstanceOf(UnsupportedOperationException.class);
    }

    @BeforeDynamicTest void shouldFailBeforeToModifyPassedRequests(List<HttpRequest> requests) {
        var throwable = catchThrowable(() -> requests.remove(0));

        then(throwable).isInstanceOf(UnsupportedOperationException.class);
    }

    @BeforeDynamicTest void shouldFailBeforeToModifyPassedResponses(List<HttpResponse> responses) {
        var throwable = catchThrowable(() -> responses.remove(0));

        then(throwable).isInstanceOf(UnsupportedOperationException.class);
    }

    // @BeforeDynamicTest // we can't check for the exception thrown
    @SuppressWarnings("unused")
    List<HttpRequest> shouldFailToReturnOneLessRequest(List<HttpRequest> requests) {
        var stackTrace = Thread.currentThread().getStackTrace(); // TODO create helpful stack trace
        return requests.subList(0, requests.size() - 1);
    }

    // @BeforeDynamicTest // we can't check for the exception thrown
    @SuppressWarnings("unused")
    List<HttpResponse> shouldFailToReturnOneLessResponse(List<HttpResponse> responses) {
        return responses.subList(0, responses.size() - 1);
    }

    @BeforeInteraction void setup() {
        expectations.addGraphQLProduct("unexpected-fail", graphQlError("unexpected-fail", "product unexpected-fail fails unexpectedly"));
    }

    @AfterInteraction void cleanup() {expectations.cleanup();}


    @AfterDynamicTest void shouldFailAfterToModifyPassedInteractions(List<HttpInteraction> interactions) {
        var throwable = catchThrowable(() -> interactions.remove(0));

        then(throwable).isInstanceOf(UnsupportedOperationException.class);
    }

    @AfterDynamicTest void shouldFailAfterToModifyPassedRequests(List<HttpRequest> requests) {
        var throwable = catchThrowable(() -> requests.remove(0));

        then(throwable).isInstanceOf(UnsupportedOperationException.class);
    }

    @AfterDynamicTest void shouldFailAfterToModifyPassedResponses(List<HttpResponse> responses) {
        var throwable = catchThrowable(() -> responses.remove(0));

        then(throwable).isInstanceOf(UnsupportedOperationException.class);
    }


    @TestFactory DynamicNode failingConsumerTests() {
        return findTestsIn("src/test/resources/failing-wunder-bar");
    }

    @OnInteractionError void onInteractionError(BDDSoftAssertions assertions) {
        var errors = assertions.assertionErrorsCollected();
        thenSoftly(softly -> {
            softly.then(errors).hasSize(2);
            softly.then(errors.get(0).getMessage()).startsWith(
                "[errors] \n" +
                "expected: null\n" +
                " but was: [{\"extensions\"={\"code\"=");
            // TODO should also match     : \"unexpected-fail\"}, \"message\"=\"product unexpected-fail fails unexpectedly\"}]");
            // but sometimes it's actually: \"validation-error\"}, \"message\"=\"no body in GraphQL request\"}]");
            then(errors.get(1).getMessage()).startsWith(
                "[json diff (ignoring `add` operations)] \n" +
                "Expecting empty but was: [\"remove /data:\n" +
                "  expected: {\"product\":{\"id\":\"unexpected-fail\",\"description\":\"this will not be reached\"}}\n" +
                "    actual: null\"]");
        });
    }
}

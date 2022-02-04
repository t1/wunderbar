package test.provider;

import com.github.t1.wunderbar.junit.provider.AfterInteraction;
import com.github.t1.wunderbar.junit.provider.BeforeInteraction;
import com.github.t1.wunderbar.junit.provider.OnInteractionError;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProvider;
import org.assertj.core.api.BDDSoftAssertions;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;
import test.DummyServer;
import test.NonCI;

import java.net.URI;

import static com.github.t1.wunderbar.common.mock.GraphQLResponseBuilder.graphQLResponse;
import static com.github.t1.wunderbar.common.mock.GraphQLResponseBuilder.graphQlError;
import static com.github.t1.wunderbar.junit.assertions.WunderBarBDDAssertions.then;
import static com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.findTestsIn;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static test.consumer.TestData.someId;

@NonCI
@WunderBarApiProvider(baseUri = "{endpoint()}")
class FailingAT {
    @RegisterExtension DummyServer dummyServer = new DummyServer();
    @RegisterExtension ExpectationsExtension expectations = new ExpectationsExtension();

    @SuppressWarnings("unused")
    URI endpoint() {return dummyServer.baseUri();}

    @BeforeInteraction void setup() {
        expectations.addGraphQLProduct(someId(), graphQLResponse().build());
        expectations.addGraphQLProduct("unexpected-fail", graphQlError("unexpected-fail", "product unexpected-fail fails unexpectedly"));
    }

    @AfterInteraction void cleanup() {expectations.cleanup();}

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

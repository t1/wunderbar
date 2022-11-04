package test.provider;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.ProblemDetails;
import com.github.t1.wunderbar.junit.provider.Actual;
import com.github.t1.wunderbar.junit.provider.AfterInteraction;
import com.github.t1.wunderbar.junit.provider.BeforeInteraction;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProvider;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;
import test.MockServer;

import jakarta.json.Json;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

import static com.github.t1.wunderbar.junit.assertions.WunderBarBDDAssertions.then;
import static com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.findTestsIn;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The tests for forbidden and unknown ids are quite straight forward.
 * But we do quite a round-trip with the id for the happy path:
 * <ol>
 * <li>The files request the URI with <code>expected-product-id</code> and expect that id in the response.
 * <li>We replace the URI with <code>requested-product-id</code> in {@link #prepareRequest(HttpRequest)}.
 * <li>We stub a response to return <code>generated-product-id</code> in {@link #prepareRequest(HttpRequest)}.
 * <li>We replace the <code>expected-product-id</code> in the expected response with the <code>generated-product-id</code>
 * in {@link #adjustResponse(HttpRequest, HttpResponse, HttpResponse)}.
 * </ol>
 */
@WunderBarApiProvider(baseUri = "{endpoint()}")
class WunderBarAT {
    @RegisterExtension static MockServer mockServer = new MockServer();
    @RegisterExtension ExpectationsExtension expectations = new ExpectationsExtension();

    @SuppressWarnings("unused")
    String endpoint() {return mockServer.baseUri() + "/some-mock";}

    @BeforeInteraction HttpRequest prepareRequest(HttpRequest request) {
        var productId = request.matchUri("/rest/products/(.*)").group(1);
        switch (productId) {
            case "existing-product-id":
                expectations.addRestProduct("requested-product-id", HttpResponse.builder().body(Json.createObjectBuilder()
                    .add("id", "generated-product-id")
                    .add("name", "some-product-name")
                    .add("price", 1599)
                    .build()
                ).build());
                return request.withUri("/rest/products/requested-product-id");
            case "forbidden-product-id":
                expectations.addRestProduct("forbidden-product-id", ProblemDetails.of(new ForbiddenException()).toResponse());
                break;
            case "unknown-product-id":
                expectations.addRestProduct("unknown-product-id", ProblemDetails.of(new NotFoundException()).toResponse());
                break;
            default:
                fail("unexpected request for product id " + productId);
        }
        return request;
    }

    @AfterInteraction HttpResponse adjustResponse(HttpRequest request, HttpResponse expected, @Actual HttpResponse actual) {
        var productId = request.matchUri("/rest/products/(.*)").group(1);
        if (productId.equals("requested-product-id")) {
            then(expected).has("/id", "existing-product-id");
            then(actual).has("/id", "generated-product-id");
            return expected.patch(patch -> patch.replace("/id", actual.get("/id")));
        } else return actual;
    }

    @AfterInteraction void cleanup() {expectations.cleanup();}


    @TestFactory DynamicNode consumerTestsInDir() {
        return findTestsIn("src/test/resources/test-wunder-bar");
    }

    @TestFactory DynamicNode consumerTestsInJar() {
        return findTestsIn("src/test/resources/test-wunder-bar.jar");
    }

    @TestFactory DynamicNode consumerTestsInBar() {
        return findTestsIn("src/test/resources/test-wunder-bar.bar");
    }
}

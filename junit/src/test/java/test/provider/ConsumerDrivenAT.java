package test.provider;

import com.github.t1.wunderbar.junit.provider.AfterInteraction;
import com.github.t1.wunderbar.junit.provider.BeforeInteraction;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProvider;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;
import test.DummyServer;

import javax.json.Json;
import java.net.URI;

import static com.github.t1.wunderbar.common.mock.RestErrorSupplier.restError;
import static com.github.t1.wunderbar.common.mock.RestResponseSupplier.restResponse;
import static com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.findTestsIn;

@WunderBarApiProvider(baseUri = "{endpoint()}")
class ConsumerDrivenAT {
    @RegisterExtension DummyServer dummyServer = new DummyServer();
    @RegisterExtension ExpectationsExtension expectations = new ExpectationsExtension();

    @SuppressWarnings("unused")
    URI endpoint() {return dummyServer.baseUri();}

    @BeforeInteraction void setup() {
        expectations.addRestProduct("existing-product-id", restResponse().body(Json.createObjectBuilder()
            .add("id", "existing-product-id")
            .add("name", "some-product-name")
            .add("price", 1599)
            .build()
        ));
        expectations.addRestProduct("forbidden-product-id", restError()
            .status(403)
            .detail("HTTP 403 Forbidden")
            .title("ForbiddenException")
            .type("urn:problem-type:javax.ws.rs.ForbiddenException")
            .build());
        expectations.addRestProduct("unknown-product-id", restError()
            .status(404)
            .detail("HTTP 404 Not Found")
            .title("NotFoundException")
            .type("urn:problem-type:javax.ws.rs.NotFoundException")
            .build());
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

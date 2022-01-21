package test.provider;

import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.ProblemDetails;
import com.github.t1.wunderbar.junit.provider.AfterInteraction;
import com.github.t1.wunderbar.junit.provider.BeforeInteraction;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProvider;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;
import test.DummyServer;

import javax.json.Json;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import java.net.URI;

import static com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.findTestsIn;

@WunderBarApiProvider(baseUri = "{endpoint()}")
class ConsumerDrivenAT {
    @RegisterExtension DummyServer dummyServer = new DummyServer();
    @RegisterExtension ExpectationsExtension expectations = new ExpectationsExtension();

    @SuppressWarnings("unused")
    URI endpoint() {return dummyServer.baseUri();}

    @BeforeInteraction void setup() {
        expectations.addRestProduct("existing-product-id", HttpResponse.builder().body(Json.createObjectBuilder()
            .add("id", "existing-product-id")
            .add("name", "some-product-name")
            .add("price", 1599)
            .build()
        ).build());
        expectations.addRestProduct("forbidden-product-id", ProblemDetails.of(new ForbiddenException()).toResponse());
        expectations.addRestProduct("unknown-product-id", ProblemDetails.of(new NotFoundException()).toResponse());
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

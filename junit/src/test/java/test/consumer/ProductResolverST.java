package test.consumer;

import com.github.t1.wunderbar.common.mock.MockService;
import com.github.t1.wunderbar.junit.consumer.Technology;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;
import test.DummyServer;

import java.net.URI;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

@WunderBarApiConsumer
class ProductResolverST extends ProductResolverTest {
    @RegisterExtension DummyServer dummyServer = new DummyServer();

    String endpoint() {return dummyServer.baseUri() + "/{technology}";}

    @Override void verifyBaseUri(URI baseUri, Technology technology) {
        then(baseUri).isEqualTo(dummyServer.baseUri().resolve("/" + technology.path()));
    }

    @Test void shouldResolveProduct() {
        super.shouldResolveProduct();
        then(MockService.getExpectations()).singleElement()
            .extracting("expectedRequest.body").asInstanceOf(STRING)
            .contains("\"id\": \"not-actually-called\"");
        MockService.cleanup();
    }

    @AfterEach
    void postFlightCheck(TestInfo testInfo) {
        if ("shouldFailToCallTheSameExpectedResponseBuilderTwice()".equals(testInfo.getDisplayName()))
            MockService.cleanup();
        else then(MockService.getExpectations()).isEmpty();
    }
}

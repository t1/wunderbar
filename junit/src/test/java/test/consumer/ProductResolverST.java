package test.consumer;

import com.github.t1.wunderbar.junit.consumer.Technology;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import org.junit.jupiter.api.extension.RegisterExtension;
import test.DummyServer;

import java.net.URI;

import static org.assertj.core.api.BDDAssertions.then;

@WunderBarApiConsumer
class ProductResolverST extends ProductResolverTest {
    @RegisterExtension DummyServer dummyServer = new DummyServer();

    String endpoint() {return dummyServer.baseUri() + "/{technology}";}

    @Override void verifyBaseUri(URI baseUri, Technology technology) {
        then(baseUri).isEqualTo(dummyServer.baseUri().resolve("/" + technology.path()));
    }

    @Override protected void thenFailedDepletion(Throwable throwable) {
        then(throwable).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("no matching expectation found");
    }
}

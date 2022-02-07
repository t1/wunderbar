package test.consumer;

import com.github.t1.wunderbar.junit.consumer.Level;
import com.github.t1.wunderbar.junit.consumer.Technology;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import test.DummyServer;

import java.net.URI;

import static com.github.t1.wunderbar.junit.consumer.Level.SYSTEM;
import static org.assertj.core.api.BDDAssertions.then;

/** {@link WunderBarApiConsumer} with <code>level = AUTO</code> is inherited */
class ProductResolverST extends ProductResolverTest {
    @Test void testLevelShouldBeSystem(Level level) {then(level).isEqualTo(SYSTEM);}

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

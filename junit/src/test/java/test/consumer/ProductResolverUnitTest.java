package test.consumer;

import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;

import java.net.URI;

import static org.assertj.core.api.BDDAssertions.then;

/** test that the {@link WunderBarApiConsumer} with <code>level = AUTO</code> is inherited */
class ProductResolverUnitTest extends ProductResolverTest {
    /**
     * Mockito relies on <code>equals</code> being overloaded, but we want to test ITs with our own deep equals.
     * We make this a non-test by 'removing' the <code>Test</code> annotation.
     */
    @Override void shouldUpdateProduct() {}

    @Override void verifyBaseUri(URI baseUri) {then(baseUri).isNull();}
}

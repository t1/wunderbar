package test.consumer;

import com.github.t1.wunderbar.junit.consumer.Level;
import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.Technology;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import test.consumer.ProductResolver.Item;
import test.consumer.ProductResolver.Product;
import test.consumer.ProductResolver.Products;

import java.net.URI;

import static com.github.t1.wunderbar.junit.consumer.Level.INTEGRATION;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.baseUri;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;
import static test.consumer.ProductResolverIT.WithConfigKeyGenerator.TEST_CONFIG_KEY;
import static test.consumer.ProductResolverIT.WithConfigKeyGenerator.WITH_CONFIG_KEY_BAR;

/** {@link WunderBarApiConsumer} with <code>level = AUTO</code> is inherited */
class ProductResolverIT extends ProductResolverTest {
    @Test void testLevelShouldBeIntegration(Level level) {then(level).isEqualTo(INTEGRATION);}

    void verifyBaseUri(URI baseUri, Technology technology) {then(baseUri.toString()).startsWith("http://localhost:");}

    @WunderBarApiConsumer(level = INTEGRATION)
    @Nested class FixedPort {
        @Service(port = 18373) Products productsWithFixedPort;

        @Test void shouldSetFixedPort() {
            var givenProduct = Product.builder().id("x").name("some-product-name").build();
            var baseUri = baseUri(productsWithFixedPort);
            given(productsWithFixedPort.product(givenProduct.getId())).returns(givenProduct);

            var resolvedProduct = resolver.product(Item.builder().productId(givenProduct.getId()).build());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
            then(baseUri).isEqualTo(URI.create("http://localhost:18373"));
        }
    }

    /** These test fail with a Mockito UnfinishedStubbing exception, when running at unit test level */
    @Nested class StubThenNull {
        @Test void shouldFailToCallWillThrowWithNull() {
            var stub = given(products.product("dummy"));

            var throwable = catchThrowable(() -> stub.willThrow(null));

            then(throwable).hasMessage("can't throw null from an expectation");
        }
    }

    @GraphQLClientApi(configKey = TEST_CONFIG_KEY)
    interface ProductsWithConfigKey {
        Product product(String id);
    }

    @WunderBarApiConsumer(level = INTEGRATION, fileName = WITH_CONFIG_KEY_BAR)
    @Nested class WithConfigKeyGenerator {
        static final String TEST_CONFIG_KEY = "test-config-key";
        static final String WITH_CONFIG_KEY_BAR = "target/WithConfigKeyGenerator-bar/";

        @Service ProductsWithConfigKey productsWithConfigKey;

        @Test void shouldGetWithConfigKey() {
            var propName = TEST_CONFIG_KEY + "/mp-graphql/";
            System.setProperty(propName + "url", "original-url");
            System.setProperty(propName + "username", "original-username");
            System.setProperty(propName + "password", "original-password");
            var givenProduct = Product.builder().id("nam").build();
            given(productsWithConfigKey.product(givenProduct.getId())).returns(givenProduct);

            var resolvedProduct = productsWithConfigKey.product(givenProduct.getId());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
            then(System.getProperty(propName + "url")).isEqualTo("original-url");
            then(System.getProperty(propName + "username")).isEqualTo("original-username");
            then(System.getProperty(propName + "password")).isEqualTo("original-password");
        }
    }
}

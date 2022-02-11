package test.consumer;

import com.github.t1.wunderbar.junit.Register;
import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.Some;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import test.MockServer;
import test.consumer.ProductResolver.Item;
import test.consumer.ProductResolver.Product;
import test.consumer.ProductResolver.Products;

import static com.github.t1.wunderbar.junit.assertions.WunderBarBDDAssertions.then;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;

@SuppressWarnings({"removal", "deprecated"})
@WunderBarApiConsumer(endpoint = "{deprecatedEndpoint()}")
class ProductResolverDeprecatedST {
    @Service Products products;
    @SystemUnderTest ProductResolver resolver;

    @RegisterExtension static MockServer mockServer = new MockServer();
    @Register SomeProduct productGenerator;

    boolean endpointCalled = false;

    @SuppressWarnings("unused")
    String deprecatedEndpoint() {
        endpointCalled = true;
        return mockServer.baseUri() + "/graphql";
    }

    @Test void shouldResolveProductFromFunctionEndpoint(@Some Product product) {
        given(products.product(product.getId())).returns(product);

        var resolvedProduct = resolver.product(new Item(product.getId()));

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product);
        then(endpointCalled).as("endpoint function called").isTrue();
    }
}

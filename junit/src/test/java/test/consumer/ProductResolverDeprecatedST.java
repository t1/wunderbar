package test.consumer;

import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import test.DummyServer;
import test.consumer.ProductResolver.Item;
import test.consumer.ProductResolver.Products;

import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static com.github.t1.wunderbar.junit.provider.CustomBDDAssertions.then;
import static test.consumer.TestData.someProduct;

@SuppressWarnings({"removal", "deprecated"})
@WunderBarApiConsumer(endpoint = "{deprecatedEndpoint()}")
class ProductResolverDeprecatedST {
    @Service Products products;
    @SystemUnderTest ProductResolver resolver;

    @RegisterExtension DummyServer dummyServer = new DummyServer();

    boolean endpointCalled = false;

    @SuppressWarnings("unused")
    String deprecatedEndpoint() {
        endpointCalled = true;
        return dummyServer.baseUri() + "/graphql";
    }

    @Test void shouldResolveProductFromFunctionEndpoint() {
        var product = someProduct();
        given(products.product(product.getId())).willReturn(product);

        var resolvedProduct = resolver.product(new Item(product.getId()));

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product);
        then(endpointCalled).as("endpoint function called").isTrue();
    }
}

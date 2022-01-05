package test.consumer;

import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import io.smallrye.graphql.client.GraphQLClientException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import test.DummyServer;
import test.consumer.ProductResolver.Item;
import test.consumer.ProductResolver.Product;
import test.consumer.ProductResolver.Products;
import test.consumer.ProductResolverTest.RestService;

import javax.ws.rs.WebApplicationException;
import java.net.URI;

import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.baseUri;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarApiConsumer
class ProductResolverST { // not `extends ProductResolverTest`, as we must not call the `given` methods
    @Service(endpoint = "{endpoint()}/{technology}") Products products;
    @SystemUnderTest ProductResolver resolver;

    @RegisterExtension DummyServer dummyServer = new DummyServer();

    boolean endpointCalled = false;

    @SuppressWarnings("unused")
    URI endpoint() {
        endpointCalled = true;
        return dummyServer.baseUri();
    }

    @Test void shouldResolveProduct() {
        var resolvedProduct = resolver.product(new Item("existing-product-id"));

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(
            Product.builder().id("existing-product-id").name("some-product-name").price(15_99).build());
        then(endpointCalled).as("endpoint function called").isTrue();
    }

    @Test void shouldFailToResolveUnknownProduct() {
        var throwable = catchThrowableOfType(() -> resolver.product(new Item("unknown-product-id")), GraphQLClientException.class);

        then(throwable.getErrors()).hasSize(1);
        var error = throwable.getErrors().get(0);
        then(error.getMessage()).isEqualTo("product unknown-product-id not found");
        then(error.getExtensions().get("code")).isEqualTo("product-not-found"); // TODO simplify after #1224 is merged
    }

    @Test void shouldFailToResolveForbiddenProduct() {
        var throwable = catchThrowableOfType(() -> resolver.product(new Item("forbidden-product-id")), GraphQLClientException.class);

        then(throwable.getErrors()).hasSize(1);
        var error = throwable.getErrors().get(0);
        then(error.getMessage()).isEqualTo("product forbidden-product-id is forbidden");
        then(error.getExtensions().get("code")).isEqualTo("product-forbidden"); // TODO simplify after #1224 is merged
    }

    @Test void shouldGetBaseUri() {
        var baseUri = baseUri(products);
        System.out.println("actual service uri: " + baseUri);
        then(baseUri.toString()).startsWith("http://localhost:");
    }

    @Nested class REST {
        // don't bother with a SystemUnderTest here again
        @Service(endpoint = "{endpoint()}/{technology}") RestService restService;

        @SuppressWarnings("unused")
        URI endpoint() {return ProductResolverST.this.endpoint();}

        @Test void shouldGetProduct() {
            var response = restService.getProduct("existing-product-id");

            then(response).usingRecursiveComparison().isEqualTo(
                Product.builder().id("existing-product-id").name("some-product-name").price(15_99).build());
            then(endpointCalled).as("endpoint function called").isTrue();
        }

        @Test void shouldFailToGetUnknownProduct() {
            var throwable = catchThrowableOfType(() -> restService.getProduct("unknown-product-id"), WebApplicationException.class);

            then(throwable.getResponse().getStatusInfo()).isEqualTo(NOT_FOUND);
        }

        @Test void shouldFailToGetForbiddenProduct() {
            var throwable = catchThrowableOfType(() -> restService.getProduct("forbidden-product-id"), WebApplicationException.class);

            then(throwable.getResponse().getStatusInfo()).isEqualTo(FORBIDDEN);
        }
    }
}

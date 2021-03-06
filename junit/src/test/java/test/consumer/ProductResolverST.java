package test.consumer;

import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientException;
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

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarApiConsumer(endpoint = "{endpoint()}/{technology}")
class ProductResolverST { // not `extends ProductResolverTest`, as we must not call the `given` methods

    @Service Products products;
    @SystemUnderTest ProductResolver resolver;

    @RegisterExtension DummyServer dummyServer = new DummyServer();

    @SuppressWarnings("unused")
    URI endpoint() { return dummyServer.baseUri(); }


    @Test void shouldResolveProduct() {
        var resolvedProduct = resolver.product(new Item("existing-product-id"));

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(
            Product.builder().id("existing-product-id").name("some-product-name").price(15_99).build());
    }

    @Test void shouldFailToResolveUnknownProduct() {
        var throwable = catchThrowableOfType(() -> resolver.product(new Item("unknown-product-id")), GraphQLClientException.class);

        then(throwable.getErrors()).hasSize(1);
        var error = throwable.getErrors().get(0);
        then(error.getMessage()).isEqualTo("product unknown-product-id not found");
        then(error.getErrorCode()).isEqualTo("product-not-found");
    }

    @Test void shouldFailToResolveForbiddenProduct() {
        var throwable = catchThrowableOfType(() -> resolver.product(new Item("forbidden-product-id")), GraphQLClientException.class);

        then(throwable.getErrors()).hasSize(1);
        var error = throwable.getErrors().get(0);
        then(error.getMessage()).isEqualTo("product forbidden-product-id is forbidden");
        then(error.getErrorCode()).isEqualTo("product-forbidden");
    }

    @Nested class REST {
        // don't bother with a SystemUnderTest here again
        @Service RestService restService;

        @SuppressWarnings("unused")
        URI endpoint() { return ProductResolverST.this.endpoint(); }

        @Test void shouldGetProduct() {
            var response = restService.getProduct("existing-product-id");

            then(response).usingRecursiveComparison().isEqualTo(
                Product.builder().id("existing-product-id").name("some-product-name").price(15_99).build());
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

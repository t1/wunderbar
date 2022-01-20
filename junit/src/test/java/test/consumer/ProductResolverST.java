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
import test.consumer.RestProducts.ProductsRestClient;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import java.net.URI;

import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.baseUri;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static com.github.t1.wunderbar.junit.http.HttpUtils.PROBLEM_DETAIL_TYPE;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarApiConsumer
class ProductResolverST { // TODO extends ProductResolverTest {
    @Service(endpoint = "{endpoint()}/{technology}") Products products;
    @SystemUnderTest ProductResolver resolver;

    @RegisterExtension DummyServer dummyServer = new DummyServer();

    boolean endpointCalled = false;

    @SuppressWarnings("unused")
    URI endpoint() {
        endpointCalled = true;
        return dummyServer.baseUri();
    }

    Product product = Product.builder().id("existing-product-id").name("some-product-name").price(15_99).build();

    @Test void shouldResolveProduct() {
        given(products.product(product.getId())).willReturn(product);
        given(products.product("not-actually-called")).willReturn(Product.builder().id("unreachable").build());

        var resolvedProduct = resolver.product(new Item(product.getId()));

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product);
        then(endpointCalled).as("endpoint function called").isTrue();
    }

    @Test void shouldFailToResolveUnknownProduct() {
        given(products.product("unknown-product-id")).willThrow(new ProductNotFoundException("unknown-product-id"));

        var throwable = catchThrowableOfType(() -> resolver.product(new Item("unknown-product-id")), GraphQLClientException.class);

        then(throwable.getErrors()).hasSize(1);
        var error = throwable.getErrors().get(0);
        then(error.getMessage()).isEqualTo("product unknown-product-id not found");
        then(error.getExtensions().get("code")).isEqualTo("product-not-found"); // TODO simplify after #1224 is merged
    }

    @Test void shouldFailToResolveForbiddenProduct() {
        given(products.product("forbidden-product-id")).willThrow(new ProductForbiddenException("forbidden-product-id"));

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
        @Service(endpoint = "{endpoint()}/{technology}") ProductsRestClient restProducts;
        @SystemUnderTest RestProducts restResolver;

        @SuppressWarnings("unused")
        URI endpoint() {return ProductResolverST.this.endpoint();}

        @Test void shouldGetProduct() {
            var givenProduct = Product.builder().id("existing-product-id").name("some-product-name").price(15_99).build();
            given(restProducts.product(givenProduct.getId())).willReturn(product);

            var response = resolveProduct(givenProduct.getId());

            then(response).usingRecursiveComparison().isEqualTo(givenProduct);
            then(endpointCalled).as("endpoint function called").isTrue();
        }

        @Test void shouldFailToGetUnknownProduct() {
            given(restProducts.product("unknown-product-id")).willThrow(new NotFoundException("unknown-product-id"));

            var throwable = catchThrowableOfType(() -> resolveProduct("unknown-product-id"), WebApplicationException.class);

            var response = throwable.getResponse();
            then(response.getStatusInfo()).isEqualTo(NOT_FOUND);
            then(response.getHeaderString("Content-Type")).isEqualTo(PROBLEM_DETAIL_TYPE.toString());
        }

        @Test void shouldFailToGetForbiddenProduct() {
            given(restProducts.product("forbidden-product-id")).willThrow(new ForbiddenException("forbidden-product-id"));

            var throwable = catchThrowableOfType(() -> resolveProduct("forbidden-product-id"), WebApplicationException.class);

            then(throwable.getResponse().getStatusInfo()).isEqualTo(FORBIDDEN);
        }

        private Product resolveProduct(String productId) {
            return restResolver.product(new Item(productId));
        }
    }
}

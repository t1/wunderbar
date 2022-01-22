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
import static test.consumer.TestData.someProduct;

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

    Product product = someProduct();
    String productId = product.getId();

    @Test void shouldResolveProduct() {
        given(products.product(productId)).willReturn(product);
        given(products.product("not-actually-called")).willReturn(Product.builder().id("unreachable").build());

        var resolvedProduct = resolver.product(new Item(productId));

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product);
        then(endpointCalled).as("endpoint function called").isTrue();
    }

    @Test void shouldFailToResolveUnknownProduct() {
        given(products.product(productId)).willThrow(new ProductNotFoundException(productId));

        var throwable = catchThrowableOfType(() -> resolver.product(new Item(productId)), GraphQLClientException.class);

        then(throwable.getErrors()).hasSize(1);
        var error = throwable.getErrors().get(0);
        then(error.getMessage()).isEqualTo("product " + productId + " not found");
        then(error.getCode()).isEqualTo("product-not-found");
    }

    @Test void shouldFailToResolveForbiddenProduct() {
        given(products.product(productId)).willThrow(new ProductForbiddenException(productId));

        var throwable = catchThrowableOfType(() -> resolver.product(new Item(productId)), GraphQLClientException.class);

        then(throwable.getErrors()).hasSize(1);
        var error = throwable.getErrors().get(0);
        then(error.getMessage()).isEqualTo("product " + productId + " is forbidden");
        then(error.getCode()).isEqualTo("product-forbidden");
    }

    @Test void shouldGetBaseUri() {
        var baseUri = baseUri(products);

        then(baseUri.toString()).startsWith("http://localhost:");
    }

    @Nested class REST {
        @Service(endpoint = "{endpoint()}/{technology}") ProductsRestClient restProducts;
        @SystemUnderTest RestProducts restResolver;

        @SuppressWarnings("unused")
        URI endpoint() {return ProductResolverST.this.endpoint();}

        @Test void shouldGetProduct() {
            given(restProducts.product(productId)).willReturn(product);

            var response = resolveProduct(productId);

            then(response).usingRecursiveComparison().isEqualTo(product);
            then(endpointCalled).as("endpoint function called").isTrue();
        }

        @Test void shouldFailToGetUnknownProduct() {
            given(restProducts.product(productId)).willThrow(new NotFoundException(productId));

            var throwable = catchThrowableOfType(() -> resolveProduct(productId), WebApplicationException.class);

            var response = throwable.getResponse();
            then(response.getStatusInfo()).isEqualTo(NOT_FOUND);
            then(response.getHeaderString("Content-Type")).isEqualTo(PROBLEM_DETAIL_TYPE.toString());
        }

        @Test void shouldFailToGetForbiddenProduct() {
            given(restProducts.product(productId)).willThrow(new ForbiddenException(productId));

            var throwable = catchThrowableOfType(() -> resolveProduct(productId), WebApplicationException.class);

            then(throwable.getResponse().getStatusInfo()).isEqualTo(FORBIDDEN);
        }

        private Product resolveProduct(String productId) {
            return restResolver.product(new Item(productId));
        }
    }
}

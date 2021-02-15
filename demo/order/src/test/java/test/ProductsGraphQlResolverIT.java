package test;

import com.github.t1.wunderbar.demo.order.OrderItem;
import com.github.t1.wunderbar.demo.order.Product;
import com.github.t1.wunderbar.demo.order.ProductsGraphQlResolver;
import com.github.t1.wunderbar.demo.order.ProductsGraphQlResolver.Products;
import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.WunderBarConsumerExtension;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientException;
import org.junit.jupiter.api.Test;

import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarConsumerExtension
class ProductsGraphQlResolverIT {
    @Service Products products;
    @SystemUnderTest ProductsGraphQlResolver resolver;

    @Test void shouldResolveProduct() {
        var givenProduct = Product.builder().id("x").name("some-product-name").build();
        given(products.product(givenProduct.getId())).willReturn(givenProduct);

        var resolvedProduct = resolver.product(item(givenProduct.getId()));

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
    }

    @Test void shouldFailToResolveUnknownProduct() {
        given(products.product("x")).willThrow(new ProductNotFoundException("x"));

        var throwable = catchThrowableOfType(() -> resolver.product(item("x")), GraphQlClientException.class);

        then(throwable.getErrors()).hasSize(1);
        var error = throwable.getErrors().get(0);
        then(error.getMessage()).isEqualTo("product x not found");
        then(error.getErrorCode()).isEqualTo("product-not-found");
    }

    @Test void shouldFailToResolveForbiddenProduct() {
        given(products.product("x")).willThrow(new ProductForbiddenException("x"));

        var throwable = catchThrowableOfType(() -> resolver.product(item("x")), GraphQlClientException.class);

        then(throwable.getErrors()).hasSize(1);
        var error = throwable.getErrors().get(0);
        then(error.getMessage()).isEqualTo("product x is forbidden");
        then(error.getErrorCode()).isEqualTo("product-forbidden");
    }

    private OrderItem item(String x) {
        return OrderItem.builder().productId(x).build();
    }

    private static class ProductNotFoundException extends RuntimeException {
        public ProductNotFoundException(String id) { super("product " + id + " not found"); }
    }

    private static class ProductForbiddenException extends RuntimeException {
        public ProductForbiddenException(String id) { super("product " + id + " is forbidden"); }
    }
}

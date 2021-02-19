package test;

import com.github.t1.wunderbar.demo.order.OrderItem;
import com.github.t1.wunderbar.demo.order.Product;
import com.github.t1.wunderbar.demo.order.ProductsResolver;
import com.github.t1.wunderbar.demo.order.ProductsResolver.Products;
import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.WunderBarConsumerExtension;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientException;
import org.junit.jupiter.api.Test;

import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarConsumerExtension
class ProductsResolverIT {
    @Service Products products;
    @SystemUnderTest ProductsResolver resolver;

    @Test void shouldResolveProduct() {
        var givenProduct = Product.builder().id("x").name("some-product-name").build();
        given(products.product(givenProduct.getId())).willReturn(givenProduct);

        var resolvedProduct = resolver.product(item(givenProduct.getId()));

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
    }

    @Test void shouldResolveTwoProducts() {
        var givenProduct1 = Product.builder().id("x1").name("some-product-name 1").build();
        var givenProduct2 = Product.builder().id("x2").name("some-product-name 2").build();
        given(products.product(givenProduct1.getId())).willReturn(givenProduct1);
        given(products.product(givenProduct2.getId())).willReturn(givenProduct2);

        var resolvedProduct1 = resolver.product(item(givenProduct1.getId()));
        var resolvedProduct2 = resolver.product(item(givenProduct2.getId()));

        then(resolvedProduct1).usingRecursiveComparison().isEqualTo(givenProduct1);
        then(resolvedProduct2).usingRecursiveComparison().isEqualTo(givenProduct2);
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

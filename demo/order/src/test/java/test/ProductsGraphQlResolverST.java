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

import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarConsumerExtension(fileName = "target/system-wunder.bar")
class ProductsGraphQlResolverST {
    @Service Products products;
    @SystemUnderTest ProductsGraphQlResolver resolver;

    private OrderItem item(String s) { return OrderItem.builder().productId(s).build(); }

    @Test void shouldResolveProduct() {
        var resolvedProduct = resolver.product(item("existing-product-id"));

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(
            Product.builder().id("existing-product-id").name("some-product-name").build());
    }

    @Test void shouldFailToResolveUnknownProduct() {
        var throwable = catchThrowableOfType(() -> resolver.product(item("unknown-product-id")), GraphQlClientException.class);

        then(throwable.getErrors()).hasSize(1);
        var error = throwable.getErrors().get(0);
        then(error.getMessage()).isEqualTo("product unknown-product-id not found");
        then(error.getErrorCode()).isEqualTo("product-not-found");
    }

    @Test void shouldFailToResolveForbiddenProduct() {
        var throwable = catchThrowableOfType(() -> resolver.product(item("forbidden-product-id")), GraphQlClientException.class);

        then(throwable.getErrors()).hasSize(1);
        var error = throwable.getErrors().get(0);
        then(error.getMessage()).isEqualTo("product forbidden-product-id is forbidden");
        then(error.getErrorCode()).isEqualTo("product-forbidden");
    }
}

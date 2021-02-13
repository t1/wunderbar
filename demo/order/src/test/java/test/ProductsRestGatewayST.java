package test;

import com.github.t1.wunderbar.demo.order.OrderItem;
import com.github.t1.wunderbar.demo.order.Product;
import com.github.t1.wunderbar.demo.order.ProductsRestGateway;
import com.github.t1.wunderbar.demo.order.ProductsRestGateway.ProductsRestClient;
import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.WunderBarConsumerExtension;
import org.junit.jupiter.api.Test;

import javax.ws.rs.WebApplicationException;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarConsumerExtension(fileName = "target/system-wunder.bar")
class ProductsRestGatewayST {
    @Service ProductsRestClient products;
    @SystemUnderTest ProductsRestGateway gateway;

    private static OrderItem item(String productId) {
        return OrderItem.builder().productId(productId).build();
    }

    @Test void shouldGetProduct() {
        var response = gateway.product(item("existing-product-id"));

        then(response).usingRecursiveComparison().isEqualTo(
            Product.builder().id("existing-product-id").name("some-product-name").build());
    }

    @Test void shouldFailToGetUnknownProduct() {
        var throwable = catchThrowableOfType(() -> gateway.product(item("unknown-product-id")), WebApplicationException.class);

        then(throwable.getResponse().getStatusInfo()).isEqualTo(NOT_FOUND);
    }

    @Test void shouldFailToGetForbiddenProduct() {
        var throwable = catchThrowableOfType(() -> gateway.product(item("forbidden-product-id")), WebApplicationException.class);

        then(throwable.getResponse().getStatusInfo()).isEqualTo(FORBIDDEN);
    }
}

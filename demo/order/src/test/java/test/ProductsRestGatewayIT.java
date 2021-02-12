package test;

import com.github.t1.wunderbar.demo.order.OrderItem;
import com.github.t1.wunderbar.demo.order.Product;
import com.github.t1.wunderbar.demo.order.ProductsRestGateway;
import com.github.t1.wunderbar.demo.order.ProductsRestGateway.ProductsRestClient;
import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.WunderBarConsumerExtension;
import org.junit.jupiter.api.Test;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarConsumerExtension
class ProductsRestGatewayIT {
    @Service ProductsRestClient products;
    @SystemUnderTest ProductsRestGateway gateway;

    private static final String PRODUCT_ID = "some-product-id";
    private static final OrderItem ITEM = OrderItem.builder().productId(PRODUCT_ID).build();
    private static final Product PRODUCT = Product.builder().id(PRODUCT_ID).name("some-product-name").build();

    @Test void shouldGetProduct() {
        given(products.product(PRODUCT_ID)).willReturn(PRODUCT);

        var response = gateway.product(ITEM);

        then(response).usingRecursiveComparison().isEqualTo(PRODUCT);
    }

    @Test void shouldFailToGetUnknownProduct() {
        given(products.product(PRODUCT_ID)).willThrow(new NotFoundException());

        var throwable = catchThrowableOfType(() -> gateway.product(ITEM), WebApplicationException.class);

        then(throwable.getResponse().getStatusInfo()).isEqualTo(NOT_FOUND);
    }

    @Test void shouldFailToGetForbiddenProduct() {
        given(products.product(PRODUCT_ID)).willThrow(new ForbiddenException());

        var throwable = catchThrowableOfType(() -> gateway.product(ITEM), WebApplicationException.class);

        then(throwable.getResponse().getStatusInfo()).isEqualTo(FORBIDDEN);
    }
}

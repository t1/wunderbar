package test.rest;

import com.github.t1.wunderbar.demo.order.OrderItem;
import com.github.t1.wunderbar.demo.order.Product;
import com.github.t1.wunderbar.demo.order.ProductsGateway;
import com.github.t1.wunderbar.demo.order.ProductsGateway.ProductsRestClient;
import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import org.junit.jupiter.api.Test;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarApiConsumer
class ProductsGatewayIT {
    @Service ProductsRestClient products;
    @SystemUnderTest ProductsGateway gateway;

    private static final String PRODUCT_ID = "some-product-id";
    private static final OrderItem ITEM = OrderItem.builder().productId(PRODUCT_ID).build();
    private static final Product PRODUCT = Product.builder()
        .id(PRODUCT_ID)
        .name("some-product-name")
        .description("some product description")
        .price(15_99)
        .build();

    @Test void shouldGetProduct() {
        given(products.product(PRODUCT_ID)).willReturn(PRODUCT);

        var response = gateway.product(ITEM);

        then(response).usingRecursiveComparison().isEqualTo(PRODUCT);
    }

    @Test void shouldGetTwoProducts() {
        var givenProduct2 = Product.builder().id("some-product-id-2").name("some-product-name 2").build();
        given(products.product(PRODUCT_ID)).willReturn(PRODUCT);
        given(products.product(givenProduct2.getId())).willReturn(givenProduct2);

        var response1 = gateway.product(ITEM);
        var response2 = gateway.product(OrderItem.builder().productId(givenProduct2.getId()).build());

        then(response1).usingRecursiveComparison().isEqualTo(PRODUCT);
        then(response2).usingRecursiveComparison().isEqualTo(givenProduct2);
    }

    /** before you mutate an existing object, make sure it exists in the unmodified state */
    @Test void shouldUpdateProductPrice() {
        given(products.product(PRODUCT_ID)).willReturn(PRODUCT);
        var expected = PRODUCT.withPrice(12_99);
        given(products.patch(new Product().withId(PRODUCT_ID).withPrice(12_99))).willReturn(expected);

        var preCheck = gateway.product(ITEM);
        var updated = gateway.productWithPriceUpdate(ITEM, 12_99);

        then(preCheck).usingRecursiveComparison().isEqualTo(PRODUCT);
        then(updated).usingRecursiveComparison().isEqualTo(expected);
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

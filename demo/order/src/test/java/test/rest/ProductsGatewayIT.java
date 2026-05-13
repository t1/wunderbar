package test.rest;

import com.github.t1.wunderbar.demo.order.OrderItem;
import com.github.t1.wunderbar.demo.order.Product;
import com.github.t1.wunderbar.demo.order.ProductsGateway;
import com.github.t1.wunderbar.demo.order.ProductsGateway.ProductsRestClient;
import com.github.t1.wunderbar.junit.Register;
import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.Some;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer.Output;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;
import test.SomeProducts;

import static com.github.t1.wunderbar.junit.ContractFormat.OPENAPI;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarApiConsumer(output = {
        @Output(fileName = "target/wunder.bar"),
        @Output(format = OPENAPI, fileName = "target/openapi.json")
})
@Register(SomeProducts.class)
class ProductsGatewayIT {
    @Service ProductsRestClient products;
    @SystemUnderTest ProductsGateway gateway;

    @Some Product product;

    OrderItem item() {return OrderItem.builder().productId(product.getId()).build();}

    @Test void shouldGetProduct() {
        given(products.product(product.getId())).returns(product);

        var response = gateway.product(item());

        then(response).usingRecursiveComparison().isEqualTo(product);
    }

    @Test void shouldGetTwoProducts(@Some Product givenProduct2) {
        given(products.product(product.getId())).returns(product);
        given(products.product(givenProduct2.getId())).returns(givenProduct2);

        var response1 = gateway.product(item());
        var response2 = gateway.product(OrderItem.builder().productId(givenProduct2.getId()).build());

        then(response1).usingRecursiveComparison().isEqualTo(product);
        then(response2).usingRecursiveComparison().isEqualTo(givenProduct2);
    }

    /// before you mutate an existing object, make sure it exists in the unmodified state
    @Test void shouldUpdateProductPrice(@Some int newPrice) {
        given(products.product(product.getId())).returns(product);
        var expected = product.withPrice(newPrice);
        given(products.patch(new Product().withId(product.getId()).withPrice(newPrice))).returns(expected);

        var preCheck = gateway.product(item());
        var updated = gateway.productWithPriceUpdate(item(), newPrice);

        then(preCheck).usingRecursiveComparison().isEqualTo(product);
        then(updated).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test void shouldFailToGetUnknownProduct() {
        given(products.product(product.getId())).willThrow(new NotFoundException());

        var throwable = catchThrowableOfType(WebApplicationException.class, () -> gateway.product(item()));

        then(throwable.getResponse().getStatusInfo()).isEqualTo(NOT_FOUND);
    }

    @Test void shouldFailToGetForbiddenProduct() {
        given(products.product(product.getId())).willThrow(new ForbiddenException());

        var throwable = catchThrowableOfType(WebApplicationException.class, () -> gateway.product(item()));

        then(throwable.getResponse().getStatusInfo()).isEqualTo(FORBIDDEN);
    }
}

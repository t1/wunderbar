package test;

import com.github.t1.wunderbar.demo.client.OrderItem;
import com.github.t1.wunderbar.demo.client.Product;
import com.github.t1.wunderbar.demo.client.ProductsRestGateway;
import com.github.t1.wunderbar.demo.client.ProductsRestGateway.ProductsRestClient;
import com.github.t1.wunderbar.junit.Service;
import com.github.t1.wunderbar.junit.SystemUnderTest;
import com.github.t1.wunderbar.junit.WunderBarExtension;
import org.junit.jupiter.api.Test;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.WebApplicationException;

import static com.github.t1.wunderbar.junit.ExpectedResponseBuilder.given;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarExtension
class ProductsRestGatewayIT {
    @Service ProductsRestClient products;
    @SystemUnderTest ProductsRestGateway gateway;

    private static final String PRODUCT_ID = "some-product-id";
    private static final OrderItem ITEM = OrderItem.builder().productId(PRODUCT_ID).build();
    private static final Product PRODUCT = Product.builder().id(PRODUCT_ID).name("some-product-name").build();

    @Test void shouldCallRestService() {
        given(products.product(PRODUCT_ID)).willReturn(PRODUCT);

        var response = gateway.product(ITEM);

        then(response).usingRecursiveComparison().isEqualTo(PRODUCT);
    }

    @Test void shouldFailToCallForbiddenRestService() {
        given(products.product(PRODUCT_ID)).willThrow(new ForbiddenException());

        var throwable = catchThrowableOfType(() -> gateway.product(ITEM), WebApplicationException.class);

        then(throwable.getResponse().getStatusInfo()).isEqualTo(FORBIDDEN);
    }
}

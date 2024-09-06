package test.rest;

import com.github.t1.wunderbar.demo.order.OrderItem;
import com.github.t1.wunderbar.demo.order.Product;
import com.github.t1.wunderbar.demo.order.ProductsGateway;
import com.github.t1.wunderbar.demo.order.ProductsGateway.ProductsRestClient;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static test.SomeProducts.someProduct;

@ExtendWith(MockitoExtension.class)
class ProductsGatewayUnitTest {
    @Mock ProductsRestClient products;
    @InjectMocks ProductsGateway gateway;

    Product product = someProduct("123");

    OrderItem item() {return OrderItem.builder().productId(product.getId()).build();}

    @Test void shouldGetProduct() {
        given(products.product(product.getId())).willReturn(product);

        var response = gateway.product(item());

        then(response).usingRecursiveComparison().isEqualTo(product);
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

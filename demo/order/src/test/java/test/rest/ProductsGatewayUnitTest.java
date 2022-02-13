package test.rest;

import com.github.t1.wunderbar.demo.order.OrderItem;
import com.github.t1.wunderbar.demo.order.Product;
import com.github.t1.wunderbar.demo.order.ProductsGateway;
import com.github.t1.wunderbar.demo.order.ProductsGateway.ProductsRestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import test.SomeProduct;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ProductsGatewayUnitTest {
    @Mock ProductsRestClient products;
    @InjectMocks ProductsGateway gateway;

    Product product = SomeProduct.buildProduct("123");

    OrderItem item() {return OrderItem.builder().productId(product.getId()).build();}

    @Test void shouldGetProduct() {
        given(products.product(product.getId())).willReturn(product);

        var response = gateway.product(item());

        then(response).usingRecursiveComparison().isEqualTo(product);
    }

    @Test void shouldFailToGetUnknownProduct() {
        given(products.product(product.getId())).willThrow(new NotFoundException());

        var throwable = catchThrowableOfType(() -> gateway.product(item()), WebApplicationException.class);

        then(throwable.getResponse().getStatusInfo()).isEqualTo(NOT_FOUND);
    }

    @Test void shouldFailToGetForbiddenProduct() {
        given(products.product(product.getId())).willThrow(new ForbiddenException());

        var throwable = catchThrowableOfType(() -> gateway.product(item()), WebApplicationException.class);

        then(throwable.getResponse().getStatusInfo()).isEqualTo(FORBIDDEN);
    }
}

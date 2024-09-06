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
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import test.SomeProductIds;
import test.SomeProducts;

import java.net.URI;
import java.nio.file.Path;

import static com.github.t1.wunderbar.junit.assertions.WebApplicationExceptionAssert.WEB_APPLICATION_EXCEPTION;
import static com.github.t1.wunderbar.junit.assertions.WunderBarBDDAssertions.then;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.BDDAssertions.catchThrowable;

@WunderBarApiConsumer(fileName = "target/system-wunder.jar")
@Register({SomeProducts.class, SomeProductIds.class})
class ProductsGatewayST {
    /** this server would normally be a real server running somewhere */
    private final HttpServer server = new HttpServer(this::handle);

    HttpResponse handle(HttpRequest request) {
        var response = HttpResponse.builder();
        var path = request.getUri().getPath();
        then(path).startsWith("/rest/products/");
        var id = Path.of(path).getFileName().toString();
        if (id.equals(existing.getId())) {
            response.body(existing);
        } else if (id.equals(forbidden.getId())) {
            response.status(FORBIDDEN)
                    .problemDetail("the product " + id + " is forbidden")
                    .problemTitle("ForbiddenException")
                    .problemType("urn:problem-type:forbidden");
        } else {
            response.status(NOT_FOUND)
                    .problemDetail("there is no product with the id " + id)
                    .problemTitle("NotFoundException")
                    .problemType("urn:problem-type:not-found");
        }
        return response.build();
    }

    @AfterEach void stop() {server.stop();}


    @Service(endpoint = "{endpoint()}") ProductsRestClient products;
    @SystemUnderTest ProductsGateway gateway;

    @SuppressWarnings("unused")
    URI endpoint() {return server.baseUri().resolve("/rest");}

    private static OrderItem item(String productId) {
        return OrderItem.builder().productId(productId).build();
    }

    @Some Product existing;
    @Some Product forbidden;

    @Test void shouldGetProduct() {
        var response = gateway.product(item(existing.getId()));

        then(response).usingRecursiveComparison().isEqualTo(existing);
    }

    @Test void shouldFailToGetUnknownProduct(@Some("product-id") String id) {
        var throwable = catchThrowable(() -> gateway.product(item(id)));

        then(throwable).asInstanceOf(WEB_APPLICATION_EXCEPTION)
                .hasStatus(NOT_FOUND)
                .hasType("urn:problem-type:not-found")
                .hasTitle("NotFoundException")
                .hasDetail("there is no product with the id " + id);
    }

    @Test void shouldFailToGetForbiddenProduct() {
        var throwable = catchThrowable(() -> gateway.product(item(forbidden.getId())));

        then(throwable).asInstanceOf(WEB_APPLICATION_EXCEPTION)
                .hasStatus(FORBIDDEN)
                .hasType("urn:problem-type:forbidden")
                .hasTitle("ForbiddenException")
                .hasDetail("the product " + forbidden.getId() + " is forbidden");
    }
}

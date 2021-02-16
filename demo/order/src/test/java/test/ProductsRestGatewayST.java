package test;

import com.github.t1.wunderbar.demo.order.OrderItem;
import com.github.t1.wunderbar.demo.order.Product;
import com.github.t1.wunderbar.demo.order.ProductsRestGateway;
import com.github.t1.wunderbar.demo.order.ProductsRestGateway.ProductsRestClient;
import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.WunderBarConsumerExtension;
import com.github.t1.wunderbar.junit.http.HttpServer;
import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import javax.ws.rs.WebApplicationException;
import java.net.URI;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarConsumerExtension(fileName = "target/system-wunder.bar", endpoint = "{endpoint()}")
class ProductsRestGatewayST {
    private static final HttpServer SERVER = new HttpServer(ProductsRestGatewayST::handle);

    @SuppressWarnings("unused")
    static URI endpoint() { return SERVER.baseUri(); }

    static HttpServerResponse handle(HttpServerRequest request) {
        var response = HttpServerResponse.builder();
        switch (request.getUri().toString()) {
            case "/products/existing-product-id":
                response.body("{\"id\":\"existing-product-id\", \"name\":\"some-product-name\"}");
                break;
            case "/products/forbidden-product-id":
                response.status(FORBIDDEN).body("{\n" +
                    "    \"detail\": \"HTTP 403 Forbidden\",\n" +
                    "    \"title\": \"ForbiddenException\",\n" +
                    "    \"type\": \"urn:problem-type:javax.ws.rs.ForbiddenException\"\n" +
                    "}\n");
                break;
            default:
                response.status(NOT_FOUND);
        }
        return response.build();
    }

    @AfterAll static void stop() { SERVER.stop(); }


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

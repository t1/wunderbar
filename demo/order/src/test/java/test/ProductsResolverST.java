package test;

import com.github.t1.wunderbar.demo.order.OrderItem;
import com.github.t1.wunderbar.demo.order.Product;
import com.github.t1.wunderbar.demo.order.ProductsResolver;
import com.github.t1.wunderbar.demo.order.ProductsResolver.Products;
import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.WunderBarConsumer;
import com.github.t1.wunderbar.junit.http.HttpServer;
import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import java.io.StringReader;
import java.net.URI;

import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarConsumer(fileName = "target/system-wunder.jar", endpoint = "{endpoint()}")
class ProductsResolverST {
    /** this server would normally be a real server running somewhere */
    private static final HttpServer SERVER = new HttpServer(ProductsResolverST::handle);

    @SuppressWarnings("unused")
    static URI endpoint() { return SERVER.baseUri().resolve("/graphql"); }

    static HttpServerResponse handle(HttpServerRequest request) {
        assert request.getUri().toString().equals("/graphql") : "unexpected uri " + request.getUri();
        assert request.getBody().isPresent();
        var body = Json.createReader(new StringReader(request.getBody().get())).readObject();
        assert body.getString("query").equals("query product($id: String!) { product(id: $id) {id name} }")
            : "unexpected query: [" + body.getString("query") + "]";
        var response = HttpServerResponse.builder();
        var id = body.getJsonObject("variables").getString("id");
        switch (id) {
            case "existing-product-id":
                response.body("{\"data\":{\"product\":{\"id\":\"" + id + "\", \"name\":\"some-product-name\"}}}");
                break;
            case "forbidden-product-id":
                response.body("{\"errors\": [\n" +
                    "{\"extensions\": {\"code\": \"product-forbidden\"},\"message\": \"product " + id + " is forbidden\"}" +
                    "]}\n");
                break;
            default:
                response.body("{\"errors\": [\n" +
                    "{\"extensions\": {\"code\": \"product-not-found\"},\"message\": \"product " + id + " not found\"}" +
                    "]}\n");
                break;
        }
        return response.build();
    }

    @AfterAll static void stop() { SERVER.stop(); }


    @Service Products products;
    @SystemUnderTest ProductsResolver resolver;

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

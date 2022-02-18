package test.consumer;

import com.github.t1.wunderbar.junit.Register;
import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.Some;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import com.github.t1.wunderbar.junit.consumer.integration.GraphQlResponse;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import test.consumer.ProductResolver.Item;
import test.consumer.ProductResolver.Product;
import test.consumer.ProductResolver.Products;

import java.util.Map;

import static com.github.t1.wunderbar.junit.assertions.WunderBarBDDAssertions.then;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static org.assertj.core.api.Assertions.catchThrowable;

@WunderBarApiConsumer
@Register(SomeProducts.class)
class ProductResolverRealServiceST {
    @Service(endpoint = "{endpoint()}") Products products;
    @SystemUnderTest ProductResolver resolver;

    @Some Product product;

    private final HttpServer server = new HttpServer(this::handle);

    private HttpResponse handle(HttpRequest request) {
        then(request).isGraphQL()
            .hasQuery("query product($id: String!) { product(id: $id) {id name price} }")
            .hasVariable("id", product.id);
        return HttpResponse.builder()
            .body(GraphQlResponse.builder()
                .data(Map.of("product", product))
                .build())
            .build();
    }

    @AfterEach
    void afterAll() {server.stop();}

    @SuppressWarnings("unused")
    String endpoint() {return server.baseUri() + "/{technology}";}

    @Test void shouldCallSutProxy() {
        var resolvedProduct = resolver.product(new Item(product.id));

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product);
    }

    @Test void shouldFailToCallStub() {
        var throwable = catchThrowable(() -> given(products.product(product.id)).returns(product));

        then(throwable).isInstanceOf(WunderBarException.class)
            .hasMessageStartingWith("failed to add expectation to mock server");
    }
}

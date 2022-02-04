package test.consumer;

import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import com.github.t1.wunderbar.junit.consumer.integration.GraphQlResponse;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import test.consumer.ProductResolver.Item;
import test.consumer.ProductResolver.Product;
import test.consumer.ProductResolver.Products;

import java.util.Map;

import static com.github.t1.wunderbar.junit.assertions.WunderBarBDDAssertions.then;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static org.assertj.core.api.Assertions.catchThrowable;
import static test.consumer.TestData.someProduct;

@WunderBarApiConsumer
class ProductResolverRealServiceST {
    @Service(endpoint = "{endpoint()}") Products products;
    @SystemUnderTest ProductResolver resolver;

    private static final Product PRODUCT = someProduct();
    private static final String PRODUCT_ID = PRODUCT.getId();

    private static final HttpServer SERVER = new HttpServer(ProductResolverRealServiceST::handle);

    private static HttpResponse handle(HttpRequest request) {
        then(request).isGraphQL()
            .hasQuery("query product($id: String!) { product(id: $id) {id name price} }")
            .hasVariable("id", PRODUCT_ID);
        return HttpResponse.builder()
            .body(GraphQlResponse.builder()
                .data(Map.of("product", PRODUCT))
                .build())
            .build();
    }

    @AfterAll
    static void afterAll() {SERVER.stop();}

    @SuppressWarnings("unused")
    String endpoint() {return SERVER.baseUri() + "/{technology}";}

    @Test void shouldCallSutProxy() {
        var resolvedProduct = resolver.product(new Item(PRODUCT_ID));

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(PRODUCT);
    }

    @Test void shouldFailToCallStub() {
        var throwable = catchThrowable(() -> given(products.product(PRODUCT_ID)).returns(PRODUCT));

        then(throwable).isInstanceOf(WunderBarException.class)
            .hasMessageStartingWith("failed to add expectation to mock server");
    }
}

package test.graphql;

import com.github.t1.wunderbar.demo.order.OrderItem;
import com.github.t1.wunderbar.demo.order.Product;
import com.github.t1.wunderbar.demo.order.ProductsResolver;
import com.github.t1.wunderbar.demo.order.ProductsResolver.Products;
import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import com.github.t1.wunderbar.junit.http.Authorization;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.HttpServer;
import io.smallrye.graphql.client.GraphQLClientException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import java.io.StringReader;
import java.net.URI;

import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarApiConsumer(fileName = "target/system-wunder.jar")
class ProductsResolverST {
    /** this server would normally be a real server running somewhere */
    private static final HttpServer SERVER = new HttpServer(ProductsResolverST::handle);

    private static final String SYSTEM_TEST_USER = "system-test-user";
    private static final String SYSTEM_TEST_PASSWORD = "system-test-password";
    private static final Authorization SYSTEM_TEST_CREDENTIALS = new Authorization.Basic(SYSTEM_TEST_USER, SYSTEM_TEST_PASSWORD);

    static final String PRODUCTS_MP_GRAPHQL_CONFIG = Products.class.getName() + "/mp-graphql/";

    static HttpResponse handle(HttpRequest request) {
        assert request.getUri().toString().equals("/graphql") : "unexpected uri " + request.getUri();
        assert request.getBody().isPresent();
        var body = Json.createReader(new StringReader(request.getBody().get())).readObject();
        var query = body.getString("query");
        assert query.equals("query product($id: String!) { product(id: $id) {id name description price} }")
            : "unexpected query: [" + query + "]";
        if (isMutation(query)) // currently not used
            assert SYSTEM_TEST_CREDENTIALS.equals(request.getAuthorization()) : "expected mutation to be authorized with the system test credentials";
        else assert request.getAuthorization() == null : "expected query not to be authorized";

        var response = HttpResponse.builder();
        var id = body.getJsonObject("variables").getString("id");
        switch (id) {
            case "existing-product-id":
                response.body("{\"data\":{\"product\":{\"id\":\"" + id + "\", \"name\":\"some-product-name\", " +
                              "\"description\":null, \"price\":1599}}}");
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

    private static boolean isMutation(String query) {return query.startsWith("mutation ");}

    @BeforeAll
    static void setUp() {
        System.setProperty(PRODUCTS_MP_GRAPHQL_CONFIG + "url", "dummy"); // TODO remove this after #1222 is merged
        System.setProperty(PRODUCTS_MP_GRAPHQL_CONFIG + "username", SYSTEM_TEST_USER);
        System.setProperty(PRODUCTS_MP_GRAPHQL_CONFIG + "password", SYSTEM_TEST_PASSWORD);
    }

    @AfterAll static void tearDown() {
        SERVER.stop();
        System.clearProperty(PRODUCTS_MP_GRAPHQL_CONFIG + "url"); // TODO remove this after #1222 is merged
        System.clearProperty(PRODUCTS_MP_GRAPHQL_CONFIG + "username");
        System.clearProperty(PRODUCTS_MP_GRAPHQL_CONFIG + "password");
    }


    @Service(endpoint = "{endpoint()}") Products products;
    @SystemUnderTest ProductsResolver resolver;

    @SuppressWarnings("unused")
    static URI endpoint() {return SERVER.baseUri().resolve("/graphql");}

    private OrderItem item(String s) {return OrderItem.builder().productId(s).build();}

    @Test void shouldResolveProduct() {
        var resolvedProduct = resolver.product(item("existing-product-id"));

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(
            Product.builder().id("existing-product-id").name("some-product-name").price(15_99).build());
    }

    @Test void shouldFailToResolveUnknownProduct() {
        var throwable = catchThrowableOfType(() -> resolver.product(item("unknown-product-id")), GraphQLClientException.class);

        then(throwable.getErrors()).hasSize(1);
        var error = throwable.getErrors().get(0);
        then(error.getMessage()).isEqualTo("product unknown-product-id not found");
        then(error.getExtensions().get("code")).isEqualTo("product-not-found"); // TODO simplify after #1224 is merged
    }

    @Test void shouldFailToResolveForbiddenProduct() {
        var throwable = catchThrowableOfType(() -> resolver.product(item("forbidden-product-id")), GraphQLClientException.class);

        then(throwable.getErrors()).hasSize(1);
        var error = throwable.getErrors().get(0);
        then(error.getMessage()).isEqualTo("product forbidden-product-id is forbidden");
        then(error.getExtensions().get("code")).isEqualTo("product-forbidden"); // TODO simplify after #1224 is merged
    }
}

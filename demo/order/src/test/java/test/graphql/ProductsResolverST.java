package test.graphql;

import com.github.t1.wunderbar.demo.order.OrderItem;
import com.github.t1.wunderbar.demo.order.Product;
import com.github.t1.wunderbar.demo.order.ProductsResolver;
import com.github.t1.wunderbar.demo.order.ProductsResolver.Products;
import com.github.t1.wunderbar.junit.Register;
import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.Some;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import com.github.t1.wunderbar.junit.http.Authorization;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.HttpServer;
import io.smallrye.graphql.client.GraphQLClientException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.SomeProducts;

import java.net.URI;

import static com.github.t1.wunderbar.junit.http.HttpUtils.JSONB;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarApiConsumer(fileName = "target/system-wunder.jar")
@Register(SomeProducts.class)
class ProductsResolverST {
    /** this server would normally be a real server running somewhere */
    private final HttpServer server = new HttpServer(this::handle);

    private static final String SYSTEM_TEST_USER = "system-test-user";
    private static final String SYSTEM_TEST_PASSWORD = "system-test-password";
    private static final Authorization SYSTEM_TEST_CREDENTIALS = new Authorization.Basic(SYSTEM_TEST_USER, SYSTEM_TEST_PASSWORD);

    static final String PRODUCTS_MP_GRAPHQL_CONFIG = Products.class.getName() + "/mp-graphql/";

    HttpResponse handle(HttpRequest request) {
        assert request.getUri().toString().equals("/graphql") : "unexpected uri " + request.getUri();
        assert request.hasBody();
        var body = request.jsonValue().asJsonObject();
        var query = body.getString("query");
        assert query.equals("query product($id: String!) { product(id: $id) {id name description price} }")
            : "unexpected query: [" + query + "]";
        if (isMutation(query)) // currently, not used
            assert SYSTEM_TEST_CREDENTIALS.equals(request.getAuthorization()) : "expected mutation to be authorized with the system test credentials";
        else assert request.getAuthorization() == null : "expected query not to be authorized";

        var response = HttpResponse.builder();
        var id = body.getJsonObject("variables").getString("id");
        if (id.equals(existing.getId()))
            response.body("{\"data\":{\"product\":" + JSONB.toJson(existing) + "}}");
        else if (id.equals(forbidden.getId()))
            response.body(error("product-forbidden", id, " is forbidden"));
        else
            response.body(error("product-not-found", id, " not found"));
        return response.build();
    }

    private String error(String code, String id, String messageSuffix) {
        return "{\"errors\": [\n" +
               "{\"extensions\": {\"code\": \"" + code + "\"},\"message\": \"product " + id + messageSuffix + "\"}" +
               "]}\n";
    }

    private static boolean isMutation(String query) {return query.startsWith("mutation ");}

    @BeforeAll
    static void setUp() {
        System.setProperty(PRODUCTS_MP_GRAPHQL_CONFIG + "username", SYSTEM_TEST_USER);
        System.setProperty(PRODUCTS_MP_GRAPHQL_CONFIG + "password", SYSTEM_TEST_PASSWORD);
    }

    @AfterEach void stopServer() {server.stop();}

    @AfterAll static void tearDown() {
        System.clearProperty(PRODUCTS_MP_GRAPHQL_CONFIG + "username");
        System.clearProperty(PRODUCTS_MP_GRAPHQL_CONFIG + "password");
    }


    @Service(endpoint = "{endpoint()}") Products products;
    @SystemUnderTest ProductsResolver resolver;

    @SuppressWarnings("unused")
    URI endpoint() {return server.baseUri().resolve("/graphql");}

    private OrderItem item(String id) {return OrderItem.builder().productId(id).build();}

    @Some Product existing;
    @Some Product forbidden;

    @Test void shouldResolveProduct() {
        var resolvedProduct = resolver.product(item(existing.getId()));

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(existing);
    }

    @Test void shouldFailToResolveUnknownProduct() {
        var throwable = catchThrowableOfType(() -> resolver.product(item("-1")), GraphQLClientException.class);

        then(throwable.getErrors()).hasSize(1);
        var error = throwable.getErrors().get(0);
        then(error.getMessage()).isEqualTo("product -1 not found");
        then(error.getCode()).isEqualTo("product-not-found");
    }

    @Test void shouldFailToResolveForbiddenProduct() {
        var throwable = catchThrowableOfType(() -> resolver.product(item(forbidden.getId())), GraphQLClientException.class);

        then(throwable.getErrors()).hasSize(1);
        var error = throwable.getErrors().get(0);
        then(error.getMessage()).isEqualTo("product " + forbidden.getId() + " is forbidden");
        then(error.getCode()).isEqualTo("product-forbidden");
    }
}

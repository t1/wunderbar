package test.acceptance;

import com.github.t1.wunderbar.demo.product.Product;
import com.github.t1.wunderbar.junit.http.Authorization;
import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.AfterDynamicTest;
import com.github.t1.wunderbar.junit.provider.BeforeInteraction;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProvider;
import io.smallrye.graphql.client.GraphQLClientException;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.smallrye.graphql.client.typesafe.api.Header;
import io.smallrye.graphql.client.typesafe.api.TypesafeGraphQLClientBuilder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import test.tools.QuarkusService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.github.t1.wunderbar.junit.assertions.WunderBarBDDAssertions.then;
import static com.github.t1.wunderbar.junit.http.HttpUtils.jsonString;
import static com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.findTestsIn;
import static com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.findTestsInArtifact;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static test.tools.QuarkusServiceExtension.ENDPOINT;

@Slf4j
@QuarkusService
@WunderBarApiProvider(baseUri = ENDPOINT)
class ConsumerDrivenAT {
    private static final String GRAPHQL_ENDPOINT = ENDPOINT + "/graphql";

    @SuppressWarnings("SpellCheckingInspection")
    private static final String JWT =
        "Bearer " +
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9" +
        "." +
        "eyJpc3MiOiJodHRwczovL2dpdGh1Yi5jb20vdDEiLCJ1cG4iOiJqYW5lQGRvZS5jb20iLCJncm91cHMiOlsiV3JpdGVyIl0sImlhd" +
        "CI6MTYxNTAwMjk1NiwiZXhwIjo0NjkwODQyOTU2LCJqdGkiOiJhMWY3YzAwYS0yYWI5LTQ3MGItYWVhNi0xYjZmZWU3NDM3ZTYifQ" +
        "." +
        "VRYLxspQqQ_IjORNR092L2lqPg6SE3xOeMQEUVLEuOZj1YoynM-oOMw0UAGQsZlv1w11pf9XIf2okptV2FKloFkkn6cWm0K1ZeYYv" +
        "Ud5OKaRU33AapZ2GSSKASfOkzshzw_y5G_e5-VqCXo5asspIYwSNzFy9JcA65JWhBttyepOPUx4Kmp3Eb5V9f-2rpfNGQbyHNh7rY" +
        "BpeLrnViaaVe_3wW4QKiAX17gncNf6nLWO-pH8_qlLcaWqBNrIBauA_YqrZT4kUcyb0uFz06hSThGiJliUS2KiZratjj3YvGj8X8_" +
        "ikqc7Tm_xldxlX_D5IHyuhNNe4sVppXDko7fQMw";

    private static final List<String> createdProductIds = new ArrayList<>();

    private final Backdoor backdoor = TypesafeGraphQLClientBuilder.newBuilder().endpoint(GRAPHQL_ENDPOINT).build(Backdoor.class);
    private Authorization authorization;

    /** We use this backdoor to set up and tear down the test data */
    @GraphQLClientApi
    @SuppressWarnings("UnusedReturnValue")
    @Header(name = "Authorization", constant = JWT)
    private interface Backdoor {
        @Query Product maybeProduct(@NonNull String id);
        @Mutation @NonNull Product store(@NonNull Product product);
        @Mutation Product delete(@NonNull String productId);
    }


    @GraphQLClientApi
    @SuppressWarnings("UnusedReturnValue")
    interface UnauthorizedClientApi {
        @Mutation @NonNull Product store(@NonNull Product product);
    }

    /** It's not the job of the client to check for auth, so we do it ourselves */
    @Test void shouldFailToStoreWhenUnauthorized() {
        var api = TypesafeGraphQLClientBuilder.newBuilder()
            .endpoint(GRAPHQL_ENDPOINT)
            .build(UnauthorizedClientApi.class);
        var product = Product.builder().id("unauthorized-product-id").build();

        var throwable = catchThrowableOfType(() -> api.store(product), GraphQLClientException.class);

        then(throwable.getErrors().get(0).getCode()).isEqualTo("unauthorized");
    }

    @TestFactory DynamicNode demoOrderConsumerTests() {
        authorization = Authorization.valueOf(JWT); // use real credentials
        // if there were different bar files, we might use different credentials
        return findTestsIn("../order/target/wunder.bar");
    }

    @TestFactory DynamicNode demoOrderConsumerSystemTests() {
        return findTestsIn("../order/target/system-wunder.jar");
    }

    // @TestFactory // disabled, as during maven release:prepare tests, this artifact is not installed as expected
    @SuppressWarnings("unused")
    DynamicNode demoOrderConsumerArtifactTests() throws IOException {
        return findTestsInArtifact("com.github.t1:wunderbar.demo.order:" + getVersion());
    }

    /**
     * Get the currently running version of the wunderbar.demo.product artifact.
     * We assume this to be the same version as the wunderbar.demo.order artifact.
     */
    private String getVersion() throws IOException {
        var pom = Files.readString(Path.of("pom.xml"), UTF_8);
        var matcher = Pattern.compile("<version>(?<version>[^<]+)</version>").matcher(pom);
        if (!matcher.find()) throw new RuntimeException("no version found in pom");
        return matcher.group("version");
    }


    @AfterDynamicTest void removeAllTestData() {
        createdProductIds.forEach(backdoor::delete);
        createdProductIds.clear();
    }

    /**
     * We provide a REST as well as a GraphQL service. To make our setup code simpler, we make it specific to the technology.
     * In this case the business logic is really simple, but if you have more complex setup logic, there may be better options.
     *
     * @return the request with the <code>Authorization</code> header set, when necessary
     */
    @BeforeInteraction HttpInteraction createTestData(HttpInteraction interaction) {
        var request = interaction.getRequest();
        var isGraphQL = request.getUri().getPath().equals("/graphql");
        log.info("create test data for " + (isGraphQL ? "graphql" : "rest") + " interaction " + interaction.getNumber() + ": "
                 + request.getMethod() + " " + request.getUri());
        var setup = isGraphQL
            ? new GraphQlSetUp(interaction)
            : new RestSetUp(interaction);

        request = setup.getRequest();
        request = authorized(request, isGraphQL, setup.getNeedsAuth());

        return interaction
            .withRequest(request.withFormattedBody())
            .withResponse(setup.getResponse());
    }

    private HttpRequest authorized(HttpRequest request, boolean isGraphQL, boolean needsAuth) {
        var isAuthorized = request.getAuthorization() != null;
        // How can you specify for an MP RestClient that one method needs authentication, but another one doesn't?
        // With MP GraphQL Client that's trivial, e.g. with an `@AuthorizationHeader` annotation.
        if (isGraphQL && needsAuth && !isAuthorized) throw new RuntimeException("expected request to be authorized");
        if (isAuthorized && !needsAuth) throw new RuntimeException("expected request NOT to be authorized");
        return needsAuth ? request.withAuthorization(authorization) : request;
    }


    private Product create(Product product) {
        log.debug("create product {}", product);
        var createdProduct = backdoor.store(product);
        log.debug("created product {}", createdProduct);
        createdProductIds.add(createdProduct.getId());
        log.debug("created product ids {}", createdProductIds);
        return createdProduct;
    }

    private void createForbiddenProduct(String id) {
        create(Product.builder().id(id).forbidden(true).build());
    }

    private Product checkExists() {
        var message = "the consumer has to request the _old_ state before requesting an update";
        then(ConsumerDrivenAT.createdProductIds).as(message).hasSize(1);
        var id = createdProductIds.get(0);
        var product = backdoor.maybeProduct(id);
        then(product).describedAs("product " + id + " does not yet exist; " + message).isNotNull();
        return product;
    }

    private void doNothing() {}

    private interface SetUp {
        Boolean getNeedsAuth();
        HttpRequest getRequest();
        HttpResponse getResponse();
    }

    @Getter
    private class RestSetUp implements SetUp {
        private final HttpRequest request;
        private final HttpResponse response;
        private final Boolean needsAuth;

        private RestSetUp(HttpInteraction interaction) {
            this.request = interaction.getRequest();
            this.response = interaction.getResponse();
            this.needsAuth = setup();
        }

        private boolean setup() {
            switch (response.getStatus().toEnum()) {
                case OK:
                    return switch (request.getMethod()) {
                        case "GET" -> {
                            create(response.as(Product.class));
                            yield false;
                        }
                        case "PATCH" -> {
                            checkExists();
                            yield true;
                        }
                        default -> throw new RuntimeException("unsupported method " + request.getMethod());
                    };
                case FORBIDDEN:
                    createForbiddenProduct(request.matchUri("/rest/products/(.*)").group(1));
                    return false;
                case NOT_FOUND:
                    doNothing();
                    return false;
                default:
                    throw new RuntimeException("unsupported status " + response.getStatus());
            }
        }
    }


    @Getter
    private class GraphQlSetUp implements SetUp {
        private HttpRequest request;
        private HttpResponse response;
        private final Boolean needsAuth;
        private final GraphQlResponse graphQlResponse;

        private GraphQlSetUp(HttpInteraction interaction) {
            this.request = interaction.getRequest();
            this.response = interaction.getResponse();
            this.graphQlResponse = response.as(GraphQlResponse.class);
            this.needsAuth = setup();
        }

        private boolean setup() {
            var operation = expectedErrorCode().or(this::dataName).orElseThrow();
            switch (operation) {
                case "product":
                    create(graphQlResponse.data.product);
                    return false;
                case "update":
                    patchExisting();
                    return true;
                case "product-forbidden":
                    createForbiddenProduct(jsonString(request.get("/variables/id")));
                    return false;
                case "product-not-found":
                    doNothing();
                    return false;
                default:
                    throw new RuntimeException("unsupported code: " + operation);
            }
        }

        private void create(Product product) {
            product.setId(null); // don't use the id from the consumer... the service will generate one
            var created = ConsumerDrivenAT.this.create(product);
            request = request.patch(patch -> patch.replace("/variables/id", created.getId()));
            response = response.patch(patch -> patch.replace("/data/product/id", created.getId()));
        }

        private void patchExisting() {
            var existing = checkExists();
            request = request.patch(patch -> patch.replace("/variables/patch/id", existing.getId()));
            response = response.patch(patch -> patch.replace("/data/update/id", existing.getId()));
        }

        private Optional<String> expectedErrorCode() {
            if (graphQlResponse.errors == null || graphQlResponse.errors.isEmpty()) return Optional.empty();
            if (graphQlResponse.errors.size() != 1)
                throw new RuntimeException("expected exactly one error but got " + graphQlResponse.errors);
            return Optional.of(graphQlResponse.errors.get(0).getExtensions().getCode());
        }

        private Optional<String> dataName() {return Optional.of(graphQlResponse.data.product != null ? "product" : "update");}
    }

    public static @Data class GraphQlResponse {
        GraphQlData data;
        List<GraphQlError> errors;
    }

    public static @Data class GraphQlData {
        Product product;
        Product update;
    }

    public static @Data class GraphQlError {
        String message;
        GraphQlErrorExtensions extensions;
    }

    public static @Data class GraphQlErrorExtensions {
        String code;
    }
}

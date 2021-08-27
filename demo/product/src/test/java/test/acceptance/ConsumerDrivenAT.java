package test.acceptance;

import com.github.t1.wunderbar.demo.product.Product;
import com.github.t1.wunderbar.junit.http.Authorization;
import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.AfterDynamicTest;
import com.github.t1.wunderbar.junit.provider.BeforeInteraction;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProvider;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientException;
import io.smallrye.graphql.client.typesafe.api.Header;
import io.smallrye.graphql.client.typesafe.api.TypesafeGraphQLClientBuilder;
import lombok.Data;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import test.tools.QuarkusService;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.github.t1.wunderbar.junit.provider.CustomBDDAssertions.then;
import static com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.findTestsIn;
import static com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.findTestsInArtifact;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static test.acceptance.ConsumerDrivenAT.ENDPOINT;

@QuarkusService
@WunderBarApiProvider(baseUri = ENDPOINT)
class ConsumerDrivenAT {
    protected static final String ENDPOINT = "http://localhost:8080";
    private static final String GRAPHQL_ENDPOINT = ENDPOINT + "/graphql";

    @SuppressWarnings("SpellCheckingInspection")
    private static final String JWT = "Bearer " +
                                      "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9" +
                                      "." +
                                      "eyJpc3MiOiJodHRwczovL2dpdGh1Yi5jb20vdDEiLCJ1cG4iOiJqYW5lQGRvZS5jb20iLCJncm91cHMiOlsiV3JpdGVyIl0sImlhd" +
                                      "CI6MTYxNTAwMjk1NiwiZXhwIjo0NjkwODQyOTU2LCJqdGkiOiJhMWY3YzAwYS0yYWI5LTQ3MGItYWVhNi0xYjZmZWU3NDM3ZTYifQ" +
                                      "." +
                                      "VRYLxspQqQ_IjORNR092L2lqPg6SE3xOeMQEUVLEuOZj1YoynM-oOMw0UAGQsZlv1w11pf9XIf2okptV2FKloFkkn6cWm0K1ZeYYv" +
                                      "Ud5OKaRU33AapZ2GSSKASfOkzshzw_y5G_e5-VqCXo5asspIYwSNzFy9JcA65JWhBttyepOPUx4Kmp3Eb5V9f-2rpfNGQbyHNh7rY" +
                                      "BpeLrnViaaVe_3wW4QKiAX17gncNf6nLWO-pH8_qlLcaWqBNrIBauA_YqrZT4kUcyb0uFz06hSThGiJliUS2KiZratjj3YvGj8X8_" +
                                      "ikqc7Tm_xldxlX_D5IHyuhNNe4sVppXDko7fQMw";
    private static final Authorization WRITER = Authorization.valueOf(JWT);

    private final Backdoor backdoor = TypesafeGraphQLClientBuilder.newBuilder().endpoint(GRAPHQL_ENDPOINT).build(Backdoor.class);
    private final List<String> created = new ArrayList<>();

    /** We use this backdoor to setup and tear down the test data */
    @GraphQLClientApi
    @SuppressWarnings("UnusedReturnValue")
    @Header(name = "Authorization", constant = JWT)
    private interface Backdoor {
        @Query boolean exists(@NonNull String id);
        @Mutation @NonNull Product store(@NonNull Product product);
        @Mutation @NonNull Product update(@NonNull Product patch);
        @Mutation @NonNull Product forbid(@NonNull String productId);
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

        then(throwable.getErrors().get(0).getErrorCode()).isEqualTo("unauthorized");
    }

    @TestFactory DynamicNode demoOrderConsumerTests() {
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
        created.forEach(backdoor::delete);
        created.clear();
    }

    /**
     * We provide a REST as well as a GraphQL service. To make our setup code simpler, we make it specific to the technology.
     * In this case the business logic is really simple, but if you have more complex setup logic, there may be better options.
     *
     * @return the request with the <code>Authorization</code> header set to the {@link #WRITER}, when necessary
     */
    @BeforeInteraction HttpRequest createTestData(HttpInteraction interaction) {
        var request = interaction.getRequest();
        var isGraphQL = request.getUri().getPath().equals("/graphql");
        System.out.println("create test data for " + (isGraphQL ? "graphql" : "rest") + " interaction " + interaction.getNumber() + ": "
                           + request.getMethod() + " " + request.getUri());
        var setup = isGraphQL
            ? new GraphQlSetUp(interaction)
            : new RestSetUp(interaction);

        var isAuthorized = request.getAuthorization() != null;
        // How can you specify for an MP RestClient that one method needs authentication, but another one doesn't?
        // With MP GraphQL Client that's trivial, e.g. with an `@AuthorizationHeader` annotation.
        if (isGraphQL && setup.needsAuth() && !isAuthorized) throw new RuntimeException("expected request to be authorized");
        if (isAuthorized && !setup.needsAuth()) throw new RuntimeException("expected request NOT to be authorized");
        return setup.needsAuth() ? request.withAuthorization(WRITER) : request;
    }


    private void create(Product product) {
        created.add(product.getId());
        backdoor.store(product);
    }

    private void createForbiddenProduct(String id) {
        create(Product.builder().id(id).build());
        backdoor.forbid(id);
    }

    private void checkExists(String id) {
        then(backdoor.exists(id))
            .describedAs("product " + id + " does not yet exist; request the _old_ state before you request an update")
            .isTrue();
    }

    private void doNothing() {}

    private interface SetUp {
        boolean needsAuth();
    }

    private class RestSetUp implements SetUp {
        private final HttpRequest request;
        private final HttpResponse response;
        protected boolean needsAuth;

        @SuppressWarnings({"CdiInjectionPointsInspection", "QsPrivateBeanMembersInspection"})
        private RestSetUp(HttpInteraction interaction) {
            this.request = interaction.getRequest();
            this.response = interaction.getResponse();

            setup();
        }

        @Override public boolean needsAuth() {return needsAuth;}

        private void setup() {
            switch (expectedStatus()) {
                case OK:
                    switch (requestMethod()) {
                        case "GET":
                            create(expectedProduct());
                            this.needsAuth = false;
                            return;
                        case "PATCH":
                            checkExists(expectedProduct().getId());
                            this.needsAuth = true;
                            return;
                        default:
                            throw new RuntimeException("unsupported method " + requestMethod());
                    }
                case FORBIDDEN:
                    createForbiddenProduct(requestedProductId());
                    this.needsAuth = false;
                    return;
                case NOT_FOUND:
                    doNothing();
                    this.needsAuth = false;
                    return;
                default:
                    throw new RuntimeException("unsupported status " + expectedStatus());
            }
        }

        private Status expectedStatus() {return response.getStatus().toEnum();}

        private String requestMethod() {return request.getMethod();}

        private Product expectedProduct() {
            var responseBody = response.getBody()
                .orElseThrow(() -> new RuntimeException("need a body to know how to make the service reply as expected"));
            return JSONB.fromJson(responseBody, Product.class);
        }

        private String requestedProductId() {
            var requestedPath = request.getUri().getPath();
            var requiredPathPrefix = "/rest/products/";
            if (!requestedPath.startsWith(requiredPathPrefix))
                throw new RuntimeException("expected path to start with `" + requiredPathPrefix + "` but was: " + requestedPath);
            return requestedPath.substring(requiredPathPrefix.length());
        }
    }


    private class GraphQlSetUp implements SetUp {
        private boolean needsAuth;

        private final GraphQlResponse graphQlResponse;

        @SuppressWarnings({"CdiInjectionPointsInspection", "QsPrivateBeanMembersInspection"})
        private GraphQlSetUp(HttpInteraction interaction) {
            HttpResponse response = interaction.getResponse();
            var responseBody = response.getBody()
                .orElseThrow(() -> new RuntimeException("need a body to know how to make the service reply as expected"));
            this.graphQlResponse = JSONB.fromJson(responseBody, GraphQlResponse.class);

            setup();
        }

        @Override public boolean needsAuth() {return needsAuth;}

        private void setup() {
            var code = expectedErrorCode().or(this::dataName).orElseThrow();
            switch (code) {
                case "product":
                    create(graphQlResponse.data.product);
                    this.needsAuth = false;
                    return;
                case "update":
                    checkExists(graphQlResponse.data.update.getId());
                    this.needsAuth = true;
                    return;
                case "product-forbidden":
                    createForbiddenProduct(expectedForbiddenProductId());
                    this.needsAuth = false;
                    return;
                case "product-not-found":
                    doNothing();
                    this.needsAuth = false;
                    return;
                default:
                    throw new RuntimeException("unsupported code: " + code);
            }
        }

        private Optional<String> expectedErrorCode() {
            if (graphQlResponse.errors == null || graphQlResponse.errors.isEmpty()) return Optional.empty();
            if (graphQlResponse.errors.size() != 1)
                throw new RuntimeException("expected exactly one error but got " + graphQlResponse.errors);
            return Optional.of(graphQlResponse.errors.get(0).getExtensions().getCode());
        }

        private Optional<String> dataName() {return Optional.of(graphQlResponse.data.product != null ? "product" : "update");}

        private String expectedForbiddenProductId() {
            var message = graphQlResponse.errors.get(0).getMessage();
            var pattern = Pattern.compile("product (?<id>.+) is forbidden");
            var matcher = pattern.matcher(message);
            then(matcher.matches()).isTrue();
            return matcher.group("id");
        }
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

    private static final Jsonb JSONB = JsonbBuilder.create();
}

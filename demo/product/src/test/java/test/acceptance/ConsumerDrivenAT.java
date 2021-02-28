package test.acceptance;

import com.github.t1.wunderbar.demo.product.Product;
import com.github.t1.wunderbar.junit.http.HttpServerInteraction;
import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import com.github.t1.wunderbar.junit.runner.AfterDynamicTest;
import com.github.t1.wunderbar.junit.runner.BeforeDynamicTest;
import com.github.t1.wunderbar.junit.runner.WunderBarRunner;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientApi;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientBuilder;
import lombok.Data;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.NonNull;
import org.junit.jupiter.api.DynamicNode;
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
import java.util.regex.Pattern;

import static com.github.t1.wunderbar.junit.runner.WunderBarTestFinder.findTestsIn;
import static com.github.t1.wunderbar.junit.runner.WunderBarTestFinder.findTestsInArtifact;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.BDDAssertions.then;
import static test.acceptance.ConsumerDrivenAT.ENDPOINT;

@QuarkusService
@WunderBarRunner(baseUri = ENDPOINT)
class ConsumerDrivenAT {
    protected static final String ENDPOINT = "http://localhost:8080";

    private final Backdoor backdoor = GraphQlClientBuilder.newBuilder().endpoint(ENDPOINT + "/graphql").build(Backdoor.class);
    private final List<String> created = new ArrayList<>();

    /** We use this backdoor to setup and tear down the test data */
    @GraphQlClientApi
    @SuppressWarnings("UnusedReturnValue")
    private interface Backdoor {
        @Mutation @NonNull Product store(@NonNull Product product);

        @Mutation @NonNull Product forbid(@NonNull String productId);

        @Mutation Product delete(@NonNull String productId);
    }


    @TestFactory DynamicNode demoOrderConsumerTests() {
        return findTestsIn("../order/target/wunder.bar");
    }

    @TestFactory DynamicNode demoOrderConsumerSystemTests() {
        return findTestsIn("../order/target/system-wunder.jar");
    }

    @TestFactory DynamicNode demoOrderConsumerArtifactTests() throws IOException {
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


    @AfterDynamicTest void removeAllTestData(List<HttpServerInteraction> interactions) {
        created.forEach(backdoor::delete);
        created.clear();
    }

    @BeforeDynamicTest void createTestData(List<HttpServerInteraction> interactions) {
        interactions.stream().map(this::createSetUp).forEach(Runnable::run);
    }

    /**
     * We provide a REST as well as a GraphQL service. To make our setup code simpler, we make it specific to the technology.
     * In this case the business logic is really simple, but if you have more complex setup logic, there may be better options.
     */
    private Runnable createSetUp(HttpServerInteraction interaction) {
        if (interaction.getRequest().getUri().getPath().equals("/graphql"))
            return new GraphQlSetUp(interaction);
        else return new RestSetUp(interaction);
    }


    private void create(Product product) {
        created.add(product.getId());
        backdoor.store(product);
    }

    private void createForbiddenProduct(String id) {
        create(Product.builder().id(id).build());
        backdoor.forbid(id);
    }

    private void doNothing() {}


    private class RestSetUp implements Runnable {
        protected final HttpServerRequest request;
        protected final HttpServerResponse response;

        @SuppressWarnings({"CdiInjectionPointsInspection", "QsPrivateBeanMembersInspection"})
        private RestSetUp(HttpServerInteraction interaction) {
            this.request = interaction.getRequest();
            this.response = interaction.getResponse();
        }

        @Override public void run() {
            switch (expectedStatus()) {
                case OK:
                    create(requestedProduct());
                    break;
                case FORBIDDEN:
                    createForbiddenProduct(requestedProductId());
                    break;
                case NOT_FOUND:
                    doNothing();
                    break;
                default:
                    throw new RuntimeException("unsupported status");
            }
        }

        private Status expectedStatus() { return response.getStatus().toEnum(); }

        private Product requestedProduct() {
            var responseBody = response.getBody()
                .orElseThrow(() -> new RuntimeException("need a body to know how to make the service reply as expected"));
            var product = JSONB.fromJson(responseBody, Product.class);
            if (!product.getId().equals(requestedProductId()))
                throw new RuntimeException("the id requested in the path doesn't match the expected response id");
            return product;
        }

        private String requestedProductId() {
            var requestedPath = request.getUri().getPath();
            var requiredPathPrefix = "/rest/products/";
            if (!requestedPath.startsWith(requiredPathPrefix))
                throw new RuntimeException("expected path to start with `" + requiredPathPrefix + "` but was: " + requestedPath);
            return requestedPath.substring(requiredPathPrefix.length());
        }
    }


    private class GraphQlSetUp implements Runnable {
        protected final HttpServerRequest request;
        protected final HttpServerResponse response;

        private final GraphQlResponse graphQlResponse;

        @SuppressWarnings({"CdiInjectionPointsInspection", "QsPrivateBeanMembersInspection"})
        private GraphQlSetUp(HttpServerInteraction interaction) {
            this.request = interaction.getRequest();
            this.response = interaction.getResponse();
            var responseBody = response.getBody()
                .orElseThrow(() -> new RuntimeException("need a body to know how to make the service reply as expected"));
            this.graphQlResponse = JSONB.fromJson(responseBody, GraphQlResponse.class);
        }

        @Override public void run() {
            switch (expectedErrorCode()) {
                case "":
                    create(graphQlResponse.data.product);
                    break;
                case "product-forbidden":
                    createForbiddenProduct(expectedForbiddenProductId());
                    break;
                case "product-not-found":
                    doNothing();
                    break;
                default:
                    throw new RuntimeException("unsupported error: " + expectedErrorCode());
            }
        }

        private String expectedErrorCode() {
            if (graphQlResponse.errors == null || graphQlResponse.errors.isEmpty()) return "";
            if (graphQlResponse.errors.size() != 1)
                throw new RuntimeException("expected exactly one error but got " + graphQlResponse.errors);
            return graphQlResponse.errors.get(0).getExtensions().getCode();
        }

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

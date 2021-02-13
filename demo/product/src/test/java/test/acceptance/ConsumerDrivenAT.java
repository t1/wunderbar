package test.acceptance;

import com.github.t1.wunderbar.demo.product.Product;
import com.github.t1.wunderbar.junit.consumer.integration.GraphQlError;
import com.github.t1.wunderbar.junit.http.HttpServerInteraction;
import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import com.github.t1.wunderbar.junit.runner.AfterBarTest;
import com.github.t1.wunderbar.junit.runner.BeforeBarTest;
import com.github.t1.wunderbar.junit.runner.WunderBarRunnerExtension;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientApi;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.NonNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.github.t1.wunderbar.junit.runner.WunderBarTestFinder.findTestsIn;
import static org.assertj.core.api.BDDAssertions.then;
import static test.acceptance.ConsumerDrivenAT.ENDPOINT;

@WunderBarRunnerExtension(baseUri = ENDPOINT)
class ConsumerDrivenAT {
    protected static final String ENDPOINT = "http://localhost:8080";

    private static Process SERVICE;

    private final Backdoor backdoor = GraphQlClientBuilder.newBuilder().endpoint(ENDPOINT + "/graphql").build(Backdoor.class);
    private final List<String> created = new ArrayList<>();

    @GraphQlClientApi
    @SuppressWarnings("UnusedReturnValue")
    private interface Backdoor {
        @Mutation @NonNull Product store(@NonNull Product product);

        @Mutation @NonNull Product forbid(@NonNull String productId);

        @Mutation Product delete(@NonNull String productId);
    }

    @BeforeAll static void startService() throws IOException {
        SERVICE = new ProcessBuilder().command("java", "-jar", "target/wunderbar.demo.product-runner.jar").inheritIO().start();
        then(SERVICE.isAlive()).isTrue();
    }

    @AfterAll static void stopService() {
        if (SERVICE != null) SERVICE.destroy();
    }

    @TestFactory DynamicNode demoOrderConsumerTests() {
        return findTestsIn("../order/target/wunder.bar");
    }


    @BeforeBarTest void setUp(List<HttpServerInteraction> interactions) {
        interactions.stream().map(this::createSetUp).forEach(SetUp::run);
    }

    private SetUp createSetUp(HttpServerInteraction interaction) {
        if (interaction.getRequest().getUri().getPath().equals("/graphql"))
            return new GraphQlSetUp(interaction);
        else return new RestSetUp(interaction);
    }

    private void store(Product product) {
        created.add(product.getId());
        backdoor.store(product);
    }

    private void forbidProduct(String id) {
        store(Product.builder().id(id).build());
        backdoor.forbid(id);
    }

    private void doNothing() {}

    private class GraphQlSetUp extends SetUp {
        private final GraphQlResponse graphQlResponse;

        @SuppressWarnings({"CdiInjectionPointsInspection", "QsPrivateBeanMembersInspection"})
        private GraphQlSetUp(HttpServerInteraction interaction) {
            super(interaction);
            this.graphQlResponse = JSONB.fromJson(responseBody(), GraphQlResponse.class);
        }

        @Override public void run() {
            switch (code()) {
                case "":
                    store(graphQlResponse.data.product);
                    break;
                case "product-forbidden":
                    forbidProduct(forbiddenProductId());
                    break;
                case "product-not-found":
                    doNothing();
                    break;
                default:
                    throw new RuntimeException("unsupported error: " + code());
            }
        }

        private String code() {
            if (graphQlResponse.errors == null || graphQlResponse.errors.isEmpty()) return "";
            assert graphQlResponse.errors.size() == 1 : "expected exactly one error but got " + graphQlResponse.errors;
            return (String) graphQlResponse.errors.get(0).getExtensions().get("code");
        }

        private String forbiddenProductId() {
            var message = graphQlResponse.errors.get(0).getMessage();
            var pattern = Pattern.compile("product (?<id>.+) is forbidden");
            var matcher = pattern.matcher(message);
            then(matcher.matches()).isTrue();
            return matcher.group("id");
        }
    }

    private class RestSetUp extends SetUp {
        @SuppressWarnings({"CdiInjectionPointsInspection", "QsPrivateBeanMembersInspection"})
        private RestSetUp(HttpServerInteraction interaction) { super(interaction); }

        @Override public void run() {
            switch (status()) {
                case OK:
                    store(requestedProduct());
                    break;
                case FORBIDDEN:
                    forbidProduct(forbiddenProductId());
                    break;
                case NOT_FOUND:
                    doNothing();
                    break;
                default:
                    throw new RuntimeException("unsupported status");
            }
        }

        private Status status() { return response.getStatus().toEnum(); }

        private Product requestedProduct() {
            return JSONB.fromJson(responseBody(), Product.class);
        }

        private String forbiddenProductId() {
            var path = request.getUri().getPath();
            if (!path.startsWith(REST_PREFIX)) throw new RuntimeException("expected path to start with `" + REST_PREFIX + "` but was: " + path);
            return path.substring(REST_PREFIX.length());
        }
    }

    private static abstract class SetUp implements Runnable {
        protected final HttpServerRequest request;
        protected final HttpServerResponse response;

        @SuppressWarnings({"CdiInjectionPointsInspection", "QsPrivateBeanMembersInspection"})
        private SetUp(HttpServerInteraction interaction) {
            this.request = interaction.getRequest();
            this.response = interaction.getResponse();
        }

        protected String responseBody() {
            return response.getBody().orElseThrow(() -> new RuntimeException("need a body to know how to make the service reply as expected"));
        }
    }

    @Getter @Setter @ToString
    public static class GraphQlResponse {
        Data data;
        List<GraphQlError> errors;
    }

    @Getter @Setter @ToString
    public static class Data {
        Product product;
    }

    @AfterBarTest void tearDown(List<HttpServerInteraction> interactions) {
        created.forEach(backdoor::delete);
    }

    private static final Jsonb JSONB = JsonbBuilder.create();
    private static final String REST_PREFIX = "/rest/products/";
}

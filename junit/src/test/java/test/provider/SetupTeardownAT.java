package test.provider;

import com.github.t1.wunderbar.common.mock.MockService;
import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.Actual;
import com.github.t1.wunderbar.junit.provider.AfterDynamicTest;
import com.github.t1.wunderbar.junit.provider.AfterInteraction;
import com.github.t1.wunderbar.junit.provider.BeforeDynamicTest;
import com.github.t1.wunderbar.junit.provider.BeforeInteraction;
import com.github.t1.wunderbar.junit.provider.OnInteractionError;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProvider;
import com.github.t1.wunderbar.junit.provider.WunderBarExecution;
import com.github.t1.wunderbar.junit.provider.WunderBarExecutions;
import org.assertj.core.api.BDDSoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;
import test.MockServer;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.github.t1.wunderbar.junit.assertions.WunderBarBDDAssertions.then;
import static com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.findTestsIn;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@WunderBarApiProvider(baseUri = "{endpoint()}")
class SetupTeardownAT {
    private static final HttpInteraction DUMMY_INTERACTION = new HttpInteraction(99, HttpRequest.builder().uri("/99").build(), HttpResponse.builder().body("bar").build());

    @RegisterExtension static MockServer mockServer = new MockServer();
    @RegisterExtension static ExpectationsExtension expectations = new ExpectationsExtension();

    @SuppressWarnings("unused")
    static String endpoint() {return mockServer.baseUri() + "/dummy-mock-server";}

    static List<String> called = new ArrayList<>();

    @BeforeAll static void beforeAll() {
        called.clear();
        expectations.add(
                HttpRequest.builder().uri("/4").build(),
                3,
                HttpResponse.builder().status(406).with("status", "UP").build());
    }


    @BeforeEach void beforeEach() {called(BeforeEach.class);}


    @Order(1)
    @BeforeDynamicTest void beforeDynamicTest(WunderBarExecutions executions) {
        called("---------------------------------------- " + executions);
    }

    @Order(2)
    @BeforeDynamicTest void beforeDynamicTest() {
        called(BeforeDynamicTest.class);
    }

    @Order(3)
    @BeforeDynamicTest List<HttpInteraction> beforeDynamicTest(List<HttpInteraction> interactions, WunderBarExecutions executions) {
        interactions = interactions.stream().map(this::incInteraction).collect(toList());
        called(BeforeDynamicTest.class, "interactions", interactionInfos(interactions), executions.getDisplayName());
        return interactions;
    }

    @Order(4)
    @BeforeDynamicTest List<HttpInteraction> addDummyInteraction(List<HttpInteraction> interactions, WunderBarExecutions executions) {
        interactions.add(DUMMY_INTERACTION);
        called(BeforeDynamicTest.class, "interactions", interactionInfos(interactions), executions.getDisplayName());
        return interactions;
    }

    @Order(5)
    @BeforeDynamicTest List<HttpRequest> beforeDynamicTestWithRequests(List<HttpRequest> requests, WunderBarExecutions executions) {
        requests = requests.stream().map(this::incUri).toList();
        called(BeforeDynamicTest.class, "requests", requestUris(requests), executions.getDisplayName());
        return requests;
    }

    @Order(6)
    @BeforeDynamicTest List<HttpResponse> beforeDynamicTestWithResponses(List<HttpResponse> responses, WunderBarExecutions executions) {
        responses = responses.stream().map(this::incStatus).toList();
        called(BeforeDynamicTest.class, "responses", responseStatus(responses), executions.getDisplayName());
        return responses;
    }

    @Order(7)
    @BeforeDynamicTest List<HttpInteraction> removeDummyInteraction(List<HttpInteraction> interactions, WunderBarExecutions executions) {
        var last = interactions.get(interactions.size() - 1);
        then(last.getNumber()).isEqualTo(DUMMY_INTERACTION.getNumber());
        then(last.getRequest()).hasUriEndingWith("/100"); // one inc
        then(last.getResponse()).asString().isEqualTo(DUMMY_INTERACTION.getResponse().getBody());
        then(last.getResponse()).hasStatus(CREATED); // one inc
        then(last.getResponse()).hasStatus(201); // one inc
        interactions.remove(last);
        return interactions;
    }


    @Order(1)
    @BeforeInteraction void beforeInteraction() {called(BeforeInteraction.class);}

    @Order(2)
    @BeforeInteraction HttpInteraction beforeInteraction(HttpInteraction interaction, WunderBarExecution execution) {
        interaction = incInteraction(interaction);
        called(BeforeInteraction.class, "interaction", interaction.getRequest().getUri(), interaction.getResponse().getStatusCode(), execution);
        return interaction;
    }

    @Order(3)
    @BeforeInteraction HttpRequest beforeInteraction(HttpRequest request, WunderBarExecution execution) {
        request = incUri(request);
        called(BeforeInteraction.class, "request", request.getUri(), execution.getDisplayName());
        return request;
    }

    @Order(4)
    @BeforeInteraction HttpResponse beforeInteraction(HttpResponse response, WunderBarExecution execution) {
        response = incStatus(response);
        called(BeforeInteraction.class, "response", response.getStatusCode(), execution.getDisplayName());
        return response;
    }


    @Order(1)
    @AfterInteraction void afterInteraction() {called(AfterInteraction.class);}

    @Order(2)
    @AfterInteraction HttpResponse afterInteraction(HttpInteraction interaction, WunderBarExecution execution) {
        interaction = incInteraction(interaction);
        called(AfterInteraction.class, "interaction", interaction.getRequest().getUri(), interaction.getResponse().getStatusCode(), execution);
        return interaction.getResponse();
    }

    @Order(3)
    @AfterInteraction
    HttpResponse afterInteraction(HttpRequest request, HttpResponse response, WunderBarExecution execution) {
        response = incStatus(response);
        called(AfterInteraction.class, "interaction", request.getUri(), response.getStatusCode(), execution);
        return response;
    }

    @Order(4)
    @AfterInteraction
    HttpResponse afterInteraction(HttpResponse expected, @Actual HttpResponse actual, WunderBarExecution execution) {
        called(AfterInteraction.class, "expected", expected.getStatusCode(), "actual", actual.getStatusCode(), execution);
        return expected.withStatus(actual.getStatus());
    }


    @Order(1)
    @OnInteractionError void onInteractionError(
            HttpInteraction interaction,
            HttpRequest request,
            HttpResponse expected,
            @Actual HttpResponse actual,
            BDDSoftAssertions softly,
            WunderBarExecution execution) {
        called(OnInteractionError.class,
                "interaction", interaction.getRequest().getUri(), interaction.getResponse().getStatusCode(),
                "request", request.getUri(),
                "expected", expected.getStatusCode(),
                "actual", actual.getStatusCode(),
                "errors", softly.assertionErrorsCollected(),
                execution);
        softly.assertAll();
    }


    @Order(1)
    @AfterDynamicTest void afterDynamicTest(List<HttpInteraction> interactions, WunderBarExecutions executions) {
        called(AfterDynamicTest.class, "interactions", interactionInfos(interactions), executions);
    }

    @Order(2)
    @AfterDynamicTest void afterDynamicTestWithRequests(List<HttpRequest> requests, WunderBarExecutions executions) {
        called(AfterDynamicTest.class, "requests", requestUris(requests), executions.getDisplayName());
    }

    @Order(3)
    @AfterDynamicTest void afterDynamicTestWithResponses(List<HttpResponse> responses, WunderBarExecutions executions) {
        called(AfterDynamicTest.class, "responses", responseStatus(responses), executions.getDisplayName());
    }

    @Order(4)
    @AfterDynamicTest
    void afterDynamicTestWithActuals(@Actual List<HttpResponse> actuals, WunderBarExecutions executions) {
        called(AfterDynamicTest.class, "actuals", responseStatus(actuals), executions.getDisplayName());
    }

    @Order(5)
    @AfterDynamicTest void afterDynamicTest() {called(AfterDynamicTest.class);}

    @Order(6)
    @AfterDynamicTest void afterDynamicTest(WunderBarExecutions executions) {
        called("---------------------------------------- done " + executions);
        called("");
    }


    @Order(1)
    @AfterEach void afterEach() {called(AfterEach.class);}


    @TestFactory DynamicNode consumerTests() {
        return findTestsIn("src/test/resources/health");
    }

    private List<String> interactionInfos(List<HttpInteraction> interactions) {
        return interactions.stream().map(httpInteraction ->
                        httpInteraction.getRequest().getUri()
                        + ":"
                        + httpInteraction.getResponse().getStatusCode())
                .toList();
    }

    private List<URI> requestUris(List<HttpRequest> requests) {
        return requests.stream().map(HttpRequest::getUri).toList();
    }

    private List<Integer> responseStatus(List<HttpResponse> responses) {
        return responses.stream().map(HttpResponse::getStatusCode).toList();
    }


    private void called(Class<? extends Annotation> annotation, Object... args) {
        var value = (annotation.getSimpleName() + " " + Stream.of(args).map(Object::toString).collect(joining(":"))).trim();
        called(value);
    }

    private void called(String value) {
        // System.out.println("# " + value);
        called.add(value);
    }


    private HttpInteraction incInteraction(HttpInteraction interaction) {
        return interaction
                .withRequest(incUri(interaction.getRequest()))
                .withResponse(incStatus(interaction.getResponse()));
    }

    private HttpRequest incUri(HttpRequest request) {return request.withUri("/" + (uriInt(request.uri()) + 1));}

    private int uriInt(String uri) {
        if (uri.equals("/q/health/ready")) return 0;
        assert uri.startsWith("/");
        return parseInt(uri.substring(1));
    }

    private HttpResponse incStatus(HttpResponse response) {
        return response.withStatusCode(response.getStatusCode() + 1);
    }

    @AfterAll static void afterAll() {
        then(MockService.getExpectations()).isEmpty();
        then(called).containsExactly(
                "BeforeEach",

                "---------------------------------------- shouldGetHealth [with 1 tests]",
                "BeforeDynamicTest",
                "BeforeDynamicTest interactions:[/1:401]:shouldGetHealth",
                "BeforeDynamicTest interactions:[/1:401, /99:200]:shouldGetHealth",
                "BeforeDynamicTest requests:[/2, /100]:shouldGetHealth",
                "BeforeDynamicTest responses:[402, 201]:shouldGetHealth",
                // => execute 1 of 1
                "BeforeInteraction",
                "BeforeInteraction interaction:/3:403:shouldGetHealth[1/1]",
                "BeforeInteraction request:/4:shouldGetHealth",
                "BeforeInteraction response:404:shouldGetHealth",
                // -- actual request 1/1:
                "AfterInteraction",
                "AfterInteraction interaction:/5:405:shouldGetHealth[1/1]",
                "AfterInteraction interaction:/4:406:shouldGetHealth[1/1]",
                "AfterInteraction expected:406:actual:406:shouldGetHealth[1/1]",

                "OnInteractionError interaction:/4:406:request:/4:expected:406:actual:406:errors:[]:shouldGetHealth[1/1]",
                // => cleanup shouldGetHealth
                "AfterDynamicTest interactions:[/2:402]:shouldGetHealth [with 1 tests]",
                "AfterDynamicTest requests:[/2]:shouldGetHealth",
                "AfterDynamicTest responses:[402]:shouldGetHealth",
                "AfterDynamicTest actuals:[406]:shouldGetHealth",
                "AfterDynamicTest",
                "---------------------------------------- done shouldGetHealth [with 1 tests]",
                "",
                "---------------------------------------- shouldGetHealthTwice [with 2 tests]",
                "BeforeDynamicTest",
                "BeforeDynamicTest interactions:[/1:401, /1:401]:shouldGetHealthTwice",
                "BeforeDynamicTest interactions:[/1:401, /1:401, /99:200]:shouldGetHealthTwice",
                "BeforeDynamicTest requests:[/2, /2, /100]:shouldGetHealthTwice",
                "BeforeDynamicTest responses:[402, 402, 201]:shouldGetHealthTwice",

                // 1/2
                "BeforeInteraction",
                "BeforeInteraction interaction:/3:403:shouldGetHealthTwice[1/2]",
                "BeforeInteraction request:/4:shouldGetHealthTwice",
                "BeforeInteraction response:404:shouldGetHealthTwice",

                "AfterInteraction",
                "AfterInteraction interaction:/5:405:shouldGetHealthTwice[1/2]",
                "AfterInteraction interaction:/4:406:shouldGetHealthTwice[1/2]",
                "AfterInteraction expected:406:actual:406:shouldGetHealthTwice[1/2]",

                "OnInteractionError interaction:/4:406:request:/4:expected:406:actual:406:errors:[]:shouldGetHealthTwice[1/2]",

                // 2/2
                "BeforeInteraction",
                "BeforeInteraction interaction:/3:403:shouldGetHealthTwice[2/2]",
                "BeforeInteraction request:/4:shouldGetHealthTwice",
                "BeforeInteraction response:404:shouldGetHealthTwice",

                "AfterInteraction",
                "AfterInteraction interaction:/5:405:shouldGetHealthTwice[2/2]",
                "AfterInteraction interaction:/4:406:shouldGetHealthTwice[2/2]",
                "AfterInteraction expected:406:actual:406:shouldGetHealthTwice[2/2]",

                "OnInteractionError interaction:/4:406:request:/4:expected:406:actual:406:errors:[]:shouldGetHealthTwice[2/2]",

                "AfterDynamicTest interactions:[/2:402, /2:402]:shouldGetHealthTwice [with 2 tests]",
                "AfterDynamicTest requests:[/2, /2]:shouldGetHealthTwice",
                "AfterDynamicTest responses:[402, 402]:shouldGetHealthTwice",
                "AfterDynamicTest actuals:[406, 406]:shouldGetHealthTwice",
                "AfterDynamicTest",
                "---------------------------------------- done shouldGetHealthTwice [with 2 tests]",
                "",
                "AfterEach");
    }
}

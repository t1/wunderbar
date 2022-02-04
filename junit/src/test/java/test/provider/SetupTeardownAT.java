package test.provider;

import com.github.t1.wunderbar.common.mock.MockService;
import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.ActualHttpResponse;
import com.github.t1.wunderbar.junit.provider.AfterDynamicTest;
import com.github.t1.wunderbar.junit.provider.AfterInteraction;
import com.github.t1.wunderbar.junit.provider.BeforeDynamicTest;
import com.github.t1.wunderbar.junit.provider.BeforeInteraction;
import com.github.t1.wunderbar.junit.provider.OnInteractionError;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProvider;
import com.github.t1.wunderbar.junit.provider.WunderBarExecution;
import com.github.t1.wunderbar.junit.provider.WunderBarExecutions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;
import test.DummyServer;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.github.t1.wunderbar.junit.assertions.WunderBarBDDAssertions.then;
import static com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.findTestsIn;
import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.CREATED;

@WunderBarApiProvider(baseUri = "{endpoint()}")
class SetupTeardownAT {
    private static final HttpInteraction DUMMY_INTERACTION = new HttpInteraction(99, HttpRequest.builder().uri("/99").build(), HttpResponse.builder().body("bar").build());

    @RegisterExtension static DummyServer dummyServer = new DummyServer();
    @RegisterExtension static ExpectationsExtension expectations = new ExpectationsExtension();

    @SuppressWarnings("unused")
    static URI endpoint() {return dummyServer.baseUri();}

    static List<String> called = new ArrayList<>();

    @BeforeAll static void beforeAll() {
        expectations.add(
            HttpRequest.builder().uri("/3").build(),
            3,
            HttpResponse.builder().status(400).with("status", "UP").build());
    }


    @BeforeEach void beforeEach() {called(BeforeEach.class);}


    @Order(1)
    @BeforeDynamicTest void beforeDynamicTest(WunderBarExecutions executions) {
        called.add("---------------------------------------- " + executions);
    }

    @Order(2)
    @BeforeDynamicTest void beforeDynamicTest() {
        called(BeforeDynamicTest.class);
    }

    @Order(3)
    @BeforeDynamicTest List<HttpInteraction> beforeDynamicTest(List<HttpInteraction> list, WunderBarExecutions executions) {
        list = list.stream().map(this::incInteraction).collect(toList());
        called(BeforeDynamicTest.class, "interactions", interactionInfos(list), executions.getDisplayName());
        return list;
    }

    @Order(4)
    @BeforeDynamicTest List<HttpInteraction> addDummyInteraction(List<HttpInteraction> list, WunderBarExecutions executions) {
        list.add(DUMMY_INTERACTION);
        called(BeforeDynamicTest.class, "interactions", interactionInfos(list), executions.getDisplayName());
        return list;
    }

    @Order(5)
    @BeforeDynamicTest List<HttpRequest> beforeDynamicTestWithRequests(List<HttpRequest> list, WunderBarExecutions executions) {
        list = list.stream().map(this::incUri).collect(toList());
        called(BeforeDynamicTest.class, "requests", requestUris(list), executions.getDisplayName());
        return list;
    }

    @Order(6)
    @BeforeDynamicTest List<HttpResponse> beforeDynamicTestWithResponses(List<HttpResponse> list, WunderBarExecutions executions) {
        list = list.stream().map(this::incStatus).collect(toList());
        called(BeforeDynamicTest.class, "responses", responseStatus(list), executions.getDisplayName());
        return list;
    }

    @Order(7)
    @BeforeDynamicTest List<HttpInteraction> removeDummyInteraction(List<HttpInteraction> list, WunderBarExecutions executions) {
        var last = list.get(list.size() - 1);
        then(last.getNumber()).isEqualTo(DUMMY_INTERACTION.getNumber());
        then(last.getRequest()).hasUriEndingWith("/100"); // one inc
        then(last.getResponse()).asString().isEqualTo(DUMMY_INTERACTION.getResponse().getBody());
        then(last.getResponse()).hasStatus(CREATED); // one inc
        then(last.getResponse()).hasStatus(201); // one inc
        list.remove(last);
        return list;
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
    @AfterInteraction HttpResponse afterInteraction(HttpRequest request, HttpResponse response, WunderBarExecution execution) {
        response = incStatus(response);
        called(AfterInteraction.class, "interaction", request.getUri(), response.getStatusCode(), execution);
        return response;
    }

    @Order(3)
    @AfterInteraction ActualHttpResponse afterInteraction(ActualHttpResponse actual, WunderBarExecution execution) {
        called(AfterInteraction.class, "actual", actual.getValue().getStatusCode(), execution);
        return actual.map(this::incStatus);
    }


    @Order(1)
    @OnInteractionError void onInteractionError() {called(OnInteractionError.class);}


    @Order(1)
    @AfterDynamicTest void afterDynamicTest(List<HttpInteraction> list, WunderBarExecutions executions) {
        called(AfterDynamicTest.class, "interactions", interactionInfos(list), executions);
    }

    @Order(2)
    @AfterDynamicTest void afterDynamicTestWithRequests(List<HttpRequest> list, WunderBarExecutions executions) {
        called(AfterDynamicTest.class, "requests", requestUris(list), executions.getDisplayName());
    }

    @Order(3)
    @AfterDynamicTest void afterDynamicTestWithResponses(List<HttpResponse> list, WunderBarExecutions executions) {
        called(AfterDynamicTest.class, "responses", responseStatus(list), executions.getDisplayName());
    }

    @Order(4)
    @AfterDynamicTest void afterDynamicTestWithActuals(List<ActualHttpResponse> actuals, WunderBarExecutions executions) {
        called(AfterDynamicTest.class, "actuals", responseStatus(actuals.stream().map(ActualHttpResponse::getValue).collect(toList())),
            executions.getDisplayName());
    }

    @Order(5)
    @AfterDynamicTest void afterDynamicTest() {called(AfterDynamicTest.class);}

    @Order(6)
    @AfterDynamicTest void afterDynamicTest(WunderBarExecutions executions) {
        called.add("---------------------------------------- done " + executions);
        called.add("");
    }


    @Order(1)
    @AfterEach void afterEach() {called(AfterEach.class);}


    @TestFactory DynamicNode consumerTests() {
        return findTestsIn("src/test/resources/health");
    }

    private List<String> interactionInfos(List<HttpInteraction> list) {
        return list.stream().map(httpInteraction ->
                ""
                + httpInteraction.getRequest().getUri()
                + ":"
                + httpInteraction.getResponse().getStatusCode())
            .collect(toList());
    }

    private List<URI> requestUris(List<HttpRequest> list) {
        return list.stream().map(HttpRequest::getUri).collect(toList());
    }

    private List<Integer> responseStatus(List<HttpResponse> list) {
        return list.stream().map(HttpResponse::getStatusCode).collect(toList());
    }


    private void called(Class<? extends Annotation> annotation, Object... args) {
        var value = (annotation.getSimpleName() + " " + Stream.of(args).map(Object::toString).collect(joining(":"))).trim();
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

            "---------------------------------------- shouldGetHealthTwice [with 2 tests]",
            "BeforeDynamicTest",
            "BeforeDynamicTest interactions:[/1:401, /1:401]:shouldGetHealthTwice",
            "BeforeDynamicTest interactions:[/1:401, /1:401, /99:200]:shouldGetHealthTwice",
            "BeforeDynamicTest requests:[/2, /2, /100]:shouldGetHealthTwice",
            "BeforeDynamicTest responses:[402, 402, 201]:shouldGetHealthTwice",

            // 1/2
            "BeforeInteraction",
            "BeforeInteraction interaction:/3:403:shouldGetHealthTwice[1/2]",
            "BeforeInteraction request:/3:shouldGetHealthTwice",
            "BeforeInteraction response:403:shouldGetHealthTwice",

            "AfterInteraction",
            "AfterInteraction interaction:/3:404:shouldGetHealthTwice[1/2]",
            "AfterInteraction actual:400:shouldGetHealthTwice[1/2]",

            "OnInteractionError",

            // 2/2
            "BeforeInteraction",
            "BeforeInteraction interaction:/3:403:shouldGetHealthTwice[2/2]",
            "BeforeInteraction request:/3:shouldGetHealthTwice",
            "BeforeInteraction response:403:shouldGetHealthTwice",

            "AfterInteraction",
            "AfterInteraction interaction:/3:404:shouldGetHealthTwice[2/2]",
            "AfterInteraction actual:400:shouldGetHealthTwice[2/2]",

            "OnInteractionError",

            "AfterDynamicTest interactions:[/2:402, /2:402]:shouldGetHealthTwice [with 2 tests]",
            "AfterDynamicTest requests:[/2, /2]:shouldGetHealthTwice",
            "AfterDynamicTest responses:[402, 402]:shouldGetHealthTwice",
            "AfterDynamicTest actuals:[401, 401]:shouldGetHealthTwice",
            "AfterDynamicTest",
            "---------------------------------------- done shouldGetHealthTwice [with 2 tests]",
            "",
            "---------------------------------------- shouldGetHealth [with 1 tests]",
            "BeforeDynamicTest",
            "BeforeDynamicTest interactions:[/1:401]:shouldGetHealth",
            "BeforeDynamicTest interactions:[/1:401, /99:200]:shouldGetHealth",
            "BeforeDynamicTest requests:[/2, /100]:shouldGetHealth",
            "BeforeDynamicTest responses:[402, 201]:shouldGetHealth",

            "BeforeInteraction",
            "BeforeInteraction interaction:/3:403:shouldGetHealth[1/1]",
            "BeforeInteraction request:/3:shouldGetHealth",
            "BeforeInteraction response:403:shouldGetHealth",

            "AfterInteraction",
            "AfterInteraction interaction:/3:404:shouldGetHealth[1/1]",
            "AfterInteraction actual:400:shouldGetHealth[1/1]",

            "OnInteractionError",

            "AfterDynamicTest interactions:[/2:402]:shouldGetHealth [with 1 tests]",
            "AfterDynamicTest requests:[/2]:shouldGetHealth",
            "AfterDynamicTest responses:[402]:shouldGetHealth",
            "AfterDynamicTest actuals:[401]:shouldGetHealth",
            "AfterDynamicTest",
            "---------------------------------------- done shouldGetHealth [with 1 tests]",
            "",
            "AfterEach");
    }
}

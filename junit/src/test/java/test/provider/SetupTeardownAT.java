package test.provider;

import com.github.t1.wunderbar.common.mock.MockService;
import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.AfterDynamicTest;
import com.github.t1.wunderbar.junit.provider.AfterInteraction;
import com.github.t1.wunderbar.junit.provider.BeforeDynamicTest;
import com.github.t1.wunderbar.junit.provider.BeforeInteraction;
import com.github.t1.wunderbar.junit.provider.OnInteractionError;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProvider;
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

@WunderBarApiProvider(baseUri = "{endpoint()}")
class SetupTeardownAT {
    @RegisterExtension static DummyServer dummyServer = new DummyServer();
    @RegisterExtension static ExpectationsExtension expectations = new ExpectationsExtension();

    @SuppressWarnings("unused")
    static URI endpoint() {return dummyServer.baseUri();}

    static List<String> called = new ArrayList<>();

    @BeforeAll static void beforeAll() {
        expectations.add(
            HttpRequest.builder().uri("/4").build(),
            3,
            HttpResponse.builder().status(400).with("status", "3").build());
    }


    @BeforeEach void beforeEach() {called(BeforeEach.class);}


    @Order(1)
    @BeforeDynamicTest void beforeDynamicTest(DynamicNode test) {
        called.add("---------------------------------------- " + test.getDisplayName());
        called(BeforeDynamicTest.class);
    }

    @Order(2)
    @BeforeDynamicTest List<HttpInteraction> beforeDynamicTest(List<HttpInteraction> list) {
        list = list.stream().map(this::incInteraction).collect(toList());
        called(BeforeDynamicTest.class, "interactions", interactionInfos(list));
        return list;
    }

    @Order(3)
    @BeforeDynamicTest List<HttpRequest> beforeDynamicTestWithRequests(List<HttpRequest> list) {
        list = list.stream().map(this::incUri).collect(toList());
        called(BeforeDynamicTest.class, "requests", requestUris(list));
        return list;
    }

    @Order(4)
    @BeforeDynamicTest List<HttpResponse> beforeDynamicTestWithResponses(List<HttpResponse> list) {
        list = list.stream().map(this::incStatus).collect(toList());
        called(BeforeDynamicTest.class, "responses", responseStatus(list));
        return list;
    }


    @Order(1)
    @BeforeInteraction void beforeInteraction() {called(BeforeInteraction.class);}

    @Order(2)
    @BeforeInteraction HttpInteraction beforeInteraction(HttpInteraction interaction) {
        interaction = incInteraction(interaction);
        called(BeforeInteraction.class, "interaction", interaction.getRequest().getUri(), interaction.getResponse().getStatusCode());
        return interaction;
    }

    @Order(3)
    @BeforeInteraction HttpRequest beforeInteraction(HttpRequest request) {
        request = incUri(request);
        called(BeforeInteraction.class, "request", request.getUri());
        return request;
    }

    @Order(4)
    @BeforeInteraction HttpResponse beforeInteraction(HttpResponse response) {
        response = incStatus(response);
        called(BeforeInteraction.class, "response", response.getStatusCode());
        return response;
    }


    @Order(1)
    @AfterInteraction void afterInteraction() {called(AfterInteraction.class);}

    @Order(2)
    @AfterInteraction HttpResponse afterInteraction(HttpRequest request, HttpResponse response) {
        called(AfterInteraction.class, "interaction", request.getUri(), response.getStatusCode());
        return response;//TODO why not incStatus(response);
    }


    @Order(1)
    @OnInteractionError void onInteractionError() {called(OnInteractionError.class);}


    @Order(1)
    @AfterDynamicTest void afterDynamicTest(List<HttpInteraction> list) {called(AfterDynamicTest.class, "interactions", interactionInfos(list));}

    @Order(2)
    @AfterDynamicTest void afterDynamicTestWithRequests(List<HttpRequest> list) {called(AfterDynamicTest.class, "requests", requestUris(list));}

    @Order(3)
    @AfterDynamicTest void afterDynamicTestWithResponses(List<HttpResponse> list) {called(AfterDynamicTest.class, "responses", responseStatus(list));}

    @Order(4)
    @AfterDynamicTest void afterDynamicTest() {
        called(AfterDynamicTest.class);
        called.add("---------------------------------------- done");
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

            "---------------------------------------- shouldGetHealthTwice",
            "BeforeDynamicTest",
            "BeforeDynamicTest interactions:[/1:401, /1:401]",
            "BeforeDynamicTest requests:[/2, /2]",
            "BeforeDynamicTest responses:[402, 402]",

            // 1/2
            "BeforeInteraction",
            "BeforeInteraction interaction:/3:403",
            "BeforeInteraction request:/4",
            "BeforeInteraction response:404",

            "AfterInteraction",
            "AfterInteraction interaction:/4:404",

            "OnInteractionError",

            // 2/2
            "BeforeInteraction",
            "BeforeInteraction interaction:/3:403",
            "BeforeInteraction request:/4",
            "BeforeInteraction response:404",

            "AfterInteraction",
            "AfterInteraction interaction:/4:404",

            "OnInteractionError",

            "AfterDynamicTest interactions:[/2:402, /2:402]",
            "AfterDynamicTest requests:[/2, /2]",
            "AfterDynamicTest responses:[402, 402]",
            "AfterDynamicTest",
            "---------------------------------------- done",

            "---------------------------------------- shouldGetHealth",
            "BeforeDynamicTest",
            "BeforeDynamicTest interactions:[/1:401]",
            "BeforeDynamicTest requests:[/2]",
            "BeforeDynamicTest responses:[402]",

            "BeforeInteraction",
            "BeforeInteraction interaction:/3:403",
            "BeforeInteraction request:/4",
            "BeforeInteraction response:404",

            "AfterInteraction",
            "AfterInteraction interaction:/4:404",

            "OnInteractionError",

            "AfterDynamicTest interactions:[/2:402]",
            "AfterDynamicTest requests:[/2]",
            "AfterDynamicTest responses:[402]",
            "AfterDynamicTest",
            "---------------------------------------- done",

            "AfterEach");
    }
}

package test.provider;

import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.provider.AfterDynamicTest;
import com.github.t1.wunderbar.junit.provider.AfterInteraction;
import com.github.t1.wunderbar.junit.provider.BeforeDynamicTest;
import com.github.t1.wunderbar.junit.provider.BeforeInteraction;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;
import test.DummyServer;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.findTestsIn;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarApiProvider(baseUri = "{endpoint()}")
class SetupTeardownAT {
    @RegisterExtension DummyServer dummyServer = new DummyServer();

    @SuppressWarnings("unused")
    URI endpoint() {return dummyServer.baseUri();}

    List<String> calledWithoutArgs = new ArrayList<>();
    List<String> calledWithArgs = new ArrayList<>();

    @BeforeEach void beforeEach() {calledWithoutArgs.add("beforeEach");}

    @BeforeDynamicTest void beforeWithoutArgs() {calledWithoutArgs.add("beforeWithoutArgsCalled");}

    @BeforeDynamicTest void beforeWithListArg(List<HttpInteraction> list) {calledWithArgs.add("beforeWithListArgCalled");}


    @BeforeInteraction void beforeInteractionWithoutArg() {calledWithoutArgs.add("beforeInteractionWithoutArgCalled");}

    @BeforeInteraction void beforeInteractionWithArg(HttpInteraction interaction) {calledWithArgs.add("beforeInteractionWithArgCalled");}


    @AfterInteraction void afterInteractionWithoutArg() {calledWithoutArgs.add("afterInteractionWithoutArgCalled");}

    @AfterInteraction void afterInteractionWithArg(HttpInteraction interaction) {calledWithArgs.add("afterInteractionWithArgCalled");}


    @AfterDynamicTest void afterWithoutArgs() {calledWithoutArgs.add("afterWithoutArgsCalled");}

    @AfterDynamicTest void afterWithListArg(List<HttpInteraction> list) {calledWithArgs.add("afterWithListArgCalled");}


    @TestFactory DynamicNode consumerTests() {
        return findTestsIn("src/test/resources/health");
    }

    @AfterEach void tearDown() {
        // two lists, as the order of the methods within the same lifecycle is undefined
        then(calledWithoutArgs).containsExactly(
            "beforeEach",
            "beforeWithoutArgsCalled",
            "beforeInteractionWithoutArgCalled",
            "afterInteractionWithoutArgCalled",
            "afterWithoutArgsCalled");
        then(calledWithArgs).containsExactly(
            "beforeWithListArgCalled",
            "beforeInteractionWithArgCalled",
            "afterInteractionWithArgCalled",
            "afterWithListArgCalled");
    }
}

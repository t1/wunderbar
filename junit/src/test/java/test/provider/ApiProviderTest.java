package test.provider;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProvider;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;

@WunderBarApiProvider(baseUri = "dummy")
class ApiProviderTest {
    @RegisterExtension static ApiProviderFixture fixture = new ApiProviderFixture();

    @TestFactory DynamicNode standardTest() {
        fixture
            .withTest("some-container/some-test",
                HttpRequest.builder()
                    .method("POST")
                    .body("{\n" +
                        "  \"query\":\"query product($id: String) { product(id: $id) {id name} }\",\n" +
                        "  \"variables\":{\"id\":\"x\"}\n" +
                        "}")
                    .build(),
                HttpResponse.builder()
                    .body("{\n" +
                        "  \"data\": {\n" +
                        "    \"product\": {\"id\": \"x\",\"name\": \"some-product-name\"}\n" +
                        "  },\n" +
                        "  \"errors\": []\n" +
                        "}\n")
                    .build())

            .expect("some-container/some-test", 1);

        return fixture.findTests();
    }

    @TestFactory DynamicNode flatTest() {
        fixture.withTest("flat")

            .expect("flat", 1);

        return fixture.findTests();
    }

    @TestFactory DynamicNode nestedTests() {
        fixture
            .withTest("root-1")
            .withTest("root-2")
            .withTest("root-2")
            .withTest("root-2")
            .withTest("root/flat-1")
            .withTest("root/flat-2")
            .withTest("root/flat-3")
            .withTest("root/nested/nest-1")
            .withTest("root/deeply/nested/deep-1")
            .withTest("root/deeply/nested/deep-2")
            .withTest("root-3")

            .expect("root-1", 1)
            .expect("root-2", 3)
            .expect("root/flat-1", 1)
            .expect("root/flat-2", 1)
            .expect("root/flat-3", 1)
            .expect("root/nested/nest-1", 1)
            .expect("root/deeply/nested/deep-1", 1)
            .expect("root/deeply/nested/deep-2", 1)
            .expect("root-3", 1);

        return fixture.findTests();
    }
}

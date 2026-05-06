package test.provider;

import com.github.t1.wunderbar.http.Authorization;
import com.github.t1.wunderbar.http.HttpRequest;
import com.github.t1.wunderbar.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProvider;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.t1.wunderbar.junit.ContractFormat.OPENAPI;

@WunderBarApiProvider(baseUri = "dummy")
class OpenApiTestFinderTest {
    @RegisterExtension static ApiProviderFixture fixture = new ApiProviderFixture();

    @TestFactory DynamicNode shouldFindTestsInOpenApiFile() {
        fixture.withFormat(OPENAPI)
                .withTest("open-api-test")
                .expect("open-api-test", 1);

        return fixture.findTests();
    }

    @TestFactory DynamicNode shouldFindTestsInOpenApiFileWithBasicAuthorization() {
        fixture.withFormat(OPENAPI)
                .withTest("open-api-auth-test",
                        HttpRequest.builder().authorization(new Authorization.Basic("dummy-user", "dummy-password")).build(),
                        HttpResponse.builder().body("{\"value\":\"ok\"}").build())
                .expect("open-api-auth-test", 1);

        return fixture.findTests();
    }
}

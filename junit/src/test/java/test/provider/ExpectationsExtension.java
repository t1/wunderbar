package test.provider;

import com.github.t1.wunderbar.common.mock.GraphQLBodyMatcher;
import com.github.t1.wunderbar.common.mock.MockService;
import com.github.t1.wunderbar.common.mock.WunderBarMockExpectation;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.Extension;

import javax.json.Json;

import static com.github.t1.wunderbar.common.mock.MockService.addExpectation;

@Slf4j
class ExpectationsExtension implements Extension {
    public void addGraphQLProduct(String id, HttpResponse response) {
        var expectation = add(GraphQLBodyMatcher.graphQlRequest()
            .query("query product($id: String!) { product(id: $id) {id name price} }")
            .variables(Json.createObjectBuilder().add("id", id).build())
            .operationName("product")
            .build(), response);
        log.debug("[GraphQL] generated expectation id {}", expectation.getId());
    }

    public void addRestProduct(String id, HttpResponse response) {
        var request = HttpRequest.builder().uri("/rest/products/" + id).build();
        var expectation = add(request, response);
        log.debug("[REST] generated expectation id {}", expectation.getId());
    }

    public WunderBarMockExpectation add(HttpRequest request, HttpResponse response) {
        return addExpectation(request, response);
    }

    public void cleanup() {MockService.cleanup();}
}

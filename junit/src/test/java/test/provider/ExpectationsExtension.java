package test.provider;

import com.github.t1.wunderbar.common.mock.GraphQLBodyMatcher;
import com.github.t1.wunderbar.common.mock.MockService;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.Extension;

import javax.json.Json;
import java.util.Stack;

import static com.github.t1.wunderbar.common.mock.MockService.addExpectation;

@Slf4j
class ExpectationsExtension implements Extension {
    private final Stack<Integer> expectationIds = new Stack<>();

    public void addGraphQLProduct(String id, HttpResponse response) {
        add(GraphQLBodyMatcher.graphQlRequest()
            .query("query product($id: String!) { product(id: $id) {id name price} }")
            .variables(Json.createObjectBuilder().add("id", id).build())
            .operationName("product")
            .build(), response);
        log.debug("[GraphQL] generated expectation ids: {}", expectationIds);
    }

    public void addRestProduct(String id, HttpResponse response) {
        var request = HttpRequest.builder().uri("/rest/products/" + id).build();
        add(request, response);
        log.debug("[REST] generated expectation ids: {}", expectationIds);
    }

    public void add(HttpRequest request, HttpResponse response) {
        var expectation = addExpectation(request, response);
        expectationIds.push(expectation.getId());
    }

    public void cleanup() {
        while (!expectationIds.isEmpty()) MockService.removeExpectation(expectationIds.pop());
        log.debug("[cleanup] generated expectation ids: {}", expectationIds);
    }
}

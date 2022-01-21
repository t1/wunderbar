package test.provider;

import com.github.t1.wunderbar.common.mock.GraphQLBodyMatcher;
import com.github.t1.wunderbar.common.mock.MockService;
import com.github.t1.wunderbar.common.mock.RequestMatcher;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.Extension;

import javax.json.Json;
import java.util.Stack;

import static com.github.t1.wunderbar.common.mock.MockService.addExpectation;

@Slf4j
public class ExpectationsExtension implements Extension {
    private final Stack<Integer> expectationIds = new Stack<>();

    public void addGraphQLProduct(String id, HttpResponse response) {
        var expectation = addExpectation(GraphQLBodyMatcher.graphQlRequest()
            .queryPattern("query product\\(\\$id: String!\\) \\{ product\\(id: \\$id\\) \\{id name (description )?price} }")
            .variables(Json.createObjectBuilder().add("id", id).build())
            .build(), response);
        expectationIds.push(expectation.getId());
        log.debug("[GraphQL] generated expectation ids: {}", expectationIds);
    }

    public void addRestProduct(String id, HttpResponse response) {
        var expectation = addExpectation(RequestMatcher.builder()
                .path("/rest/products/" + id).build(),
            response);
        expectationIds.push(expectation.getId());
        log.debug("[REST] generated expectation ids: {}", expectationIds);
    }

    public void cleanup() {
        while (!expectationIds.isEmpty()) MockService.removeExpectation(expectationIds.pop());
        log.debug("[cleanup] generated expectation ids: {}", expectationIds);
    }
}

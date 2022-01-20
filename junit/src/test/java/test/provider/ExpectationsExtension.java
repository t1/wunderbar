package test.provider;

import com.github.t1.wunderbar.common.mock.GraphQLBodyMatcher;
import com.github.t1.wunderbar.common.mock.MockService;
import com.github.t1.wunderbar.common.mock.RequestMatcher;
import com.github.t1.wunderbar.common.mock.ResponseSupplier;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.Extension;

import javax.json.Json;
import java.util.Stack;

@Slf4j
public class ExpectationsExtension implements Extension {
    private final Stack<Integer> expectationIds = new Stack<>();

    public void addGraphQLProduct(String id, ResponseSupplier responseSupplier) {
        var expectation = MockService.addExpectation(GraphQLBodyMatcher.graphQlRequest()
            .queryPattern("query product\\(\\$id: String!\\) \\{ product\\(id: \\$id\\) \\{id name (description )?price} }")
            .variables(Json.createObjectBuilder().add("id", id).build())
            .build(), responseSupplier);
        expectationIds.push(expectation.getId());
        log.debug("[GraphQL] generated expectation ids: {}", expectationIds);
    }

    public void addRestProduct(String id, ResponseSupplier responseSupplier) {
        var expectation = MockService.addExpectation(RequestMatcher.builder()
                .path("/rest/products/" + id).build(),
            responseSupplier);
        expectationIds.push(expectation.getId());
        log.debug("[REST] generated expectation ids: {}", expectationIds);
    }

    public void cleanup() {
        while (!expectationIds.isEmpty()) MockService.removeExpectation(expectationIds.pop());
        log.debug("[cleanup] generated expectation ids: {}", expectationIds);
    }
}

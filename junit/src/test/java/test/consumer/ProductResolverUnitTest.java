package test.consumer;

import com.github.t1.wunderbar.junit.consumer.Technology;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;

import javax.ws.rs.core.Response.StatusType;
import java.net.URI;

import static com.github.t1.wunderbar.junit.http.ProblemDetails.statusOf;
import static org.assertj.core.api.BDDAssertions.then;

/** test that the {@link WunderBarApiConsumer} with <code>level = AUTO</code> is inherited */
class ProductResolverUnitTest extends ProductResolverTest {
    /**
     * Mockito relies on <code>equals</code> being overloaded, but we want to test ITs with our own deep equals.
     * We make this a non-test by 'removing' the <code>Test</code> annotation.
     */
    @Override void shouldUpdateProduct() {}

    @Override void verifyBaseUri(URI baseUri, Technology technology) {then(baseUri).isNull();}

    @Override protected void thenGraphQlError(Throwable throwable, String errorCode, String message) {
        then(throwable.getMessage()).isEqualTo(message);
    }

    @Override protected void thenRestError(Throwable actual, StatusType status, String typeSuffix, String detail) {
        var actualStatus = statusOf(actual);
        if (actualStatus != null) then(actualStatus).isEqualTo(status);
        then(actual.getMessage()).isEqualTo(detail);
    }
}

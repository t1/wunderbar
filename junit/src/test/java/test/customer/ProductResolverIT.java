package test.customer;

import com.github.t1.wunderbar.junit.Service;
import com.github.t1.wunderbar.junit.WunderBarCustomerExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.github.t1.wunderbar.junit.ExpectedResponseBuilder.given;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarCustomerExtension
class ProductResolverIT extends ProductResolverTest {
    @Disabled("https://github.com/smallrye/smallrye-graphql/issues/624")
    @Override @Test void shouldResolveNamedProductMethod() {}

    /** Only the INTEGRATION level has to recognize the technology */
    @Nested class UnrecognizableTechnologies {
        @Service UnrecognizableTechnologyService unrecognizableTechnologyService;

        @Test void shouldFailToRecognizeTechnology() {
            var throwable = catchThrowable(() -> given(unrecognizableTechnologyService.call()).willReturn(null));

            then(throwable).hasMessage("no technology recognized on " + UnrecognizableTechnologyService.class);
        }
    }

    interface UnrecognizableTechnologyService {
        Object call();
    }
}

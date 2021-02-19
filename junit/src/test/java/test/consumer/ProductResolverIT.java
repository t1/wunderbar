package test.consumer;

import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.WunderBarConsumerExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import test.consumer.ProductResolver.Item;
import test.consumer.ProductResolver.Product;

import static com.github.t1.wunderbar.junit.consumer.Level.INTEGRATION;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarConsumerExtension
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

    @WunderBarConsumerExtension(endpoint = "{testEndpoint()}", level = INTEGRATION)
    @Nested class EndpointFunction {
        boolean endpointCalled = false;

        @SuppressWarnings("unused")
        public String testEndpoint() {
            endpointCalled = true;
            return "some-endpoint";
        }

        @Test void shouldResolveProductFromFunctionEndpoint() {
            var givenProduct = Product.builder().id("x").name("some-product-name").build();
            given(products.product(givenProduct.getId())).willReturn(givenProduct);

            var resolvedProduct = resolver.product(Item.builder().productId(givenProduct.getId()).build());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
            then(endpointCalled).as("endpoint function called").isTrue();
        }
    }
}

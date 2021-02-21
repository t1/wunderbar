package test.consumer;

import org.junit.jupiter.api.Test;
import test.consumer.ProductResolver.Products;

import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static org.mockito.Mockito.mock;

// @WunderBarConsumerExtension
class InvalidConsumerTest {
    // we can't check the exception thrown for the missing WunderBarConsumerExtension annotation
    // we can't check the exception thrown for the missing Service annotation
    private static final boolean DISABLED = true;

    Products products = mock(Products.class); // the mock is just so that the given gets called

    @Test void shouldFailWithoutServiceField() {
        if (DISABLED) return;
        given(products.product("x")).willThrow(new RuntimeException("foo"));
    }
}

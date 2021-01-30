package test;

import com.github.t1.bar.junit.BarExtension;
import com.github.t1.bar.junit.JUnitBarException;
import com.github.t1.bar.junit.SUT;
import com.github.t1.bar.junit.Service;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import test.ProductResolver.Item;
import test.ProductResolver.Product;
import test.ProductResolver.Products;

import static com.github.t1.bar.junit.BarOngoingStubbing.given;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;

@BarExtension
class ProductResolverTest {
    @Service Products products;
    @SUT ProductResolver resolver;

    @Test void shouldResolveProduct() {
        Product givenProduct = Product.builder().id("x").name("some-product-name").build();
        given(products.product(givenProduct.getId())).willReturn(givenProduct);

        Product resolvedProduct = resolver.product(Item.builder().productId(givenProduct.getId()).build());

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
    }

    @Test void shouldResolveTwoProducts() {
        Product givenProductA = Product.builder().id("a").name("some-product-a").build();
        Product givenProductB = Product.builder().id("b").name("some-product-b").build();
        given(products.product(givenProductA.getId())).willReturn(givenProductA);
        given(products.product(givenProductB.getId())).willReturn(givenProductB);

        Product resolvedProductA = resolver.product(Item.builder().productId(givenProductA.getId()).build());
        Product resolvedProductB = resolver.product(Item.builder().productId(givenProductB.getId()).build());

        then(resolvedProductA).usingRecursiveComparison().isEqualTo(givenProductA);
        then(resolvedProductB).usingRecursiveComparison().isEqualTo(givenProductB);
    }

    @Test void shouldFailToResolveUnknownProduct() {
        given(products.product("x")).willThrow(new RuntimeException("product x not found"));

        Throwable throwable = catchThrowable(() -> resolver.product(Item.builder().productId("x").build()));

        then(throwable).hasMessage("product x not found");
    }

    @Nested class BarExceptionTests {
        // TODO how can I test that this throws an exception in afterEach?
        //  @Test void shouldFailUnfinishedStubbing() {
        //     given(products.product("x"));
        // }

        @Test void shouldFailToCallGivenWithoutCallToMockNull() {
            Throwable throwable = catchThrowable(() -> given(null).willReturn(null));

            then(throwable).isInstanceOf(JUnitBarException.class)
                .hasMessage("call `given` only on the response object of a mock (invocation)");
        }

        @Test void shouldFailToCallGivenWithoutCallToMockNonNull() {
            @SuppressWarnings("ConstantConditions")
            Throwable throwable = catchThrowable(() -> given(1L).willReturn(null));

            then(throwable).isInstanceOf(JUnitBarException.class)
                .hasMessage("call `given` only once and only on the response object of a mock (null)");
        }

        @Test void shouldFailToCallTheSameGivenTwice() {
            Product givenProduct = Product.builder().id("x").build();
            given(products.product(givenProduct.getId())).willReturn(givenProduct);

            Throwable throwable = catchThrowable(() -> given(products.product(givenProduct.getId())).willReturn(givenProduct));

            then(throwable).isInstanceOf(JUnitBarException.class)
                .hasMessage("call `given` only once and only on the response object of a mock (null)");
        }
    }

    @Nested class NestedMock {
        @Service Products nestedProducts;

        @Test void shouldFindNestedMock() {
            Product givenProduct = Product.builder().id("y").build();
            given(nestedProducts.product(givenProduct.getId())).willReturn(givenProduct);

            Product resolvedProduct = resolver.product(Item.builder().productId(givenProduct.getId()).build());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
        }
    }

    @Nested class NestedSUT {
        @SUT ProductResolver nestedResolver;

        @Test void shouldFindNestedMock() {
            Product givenProduct = Product.builder().id("z").build();
            given(products.product(givenProduct.getId())).willReturn(givenProduct);

            Product resolvedProduct = nestedResolver.product(Item.builder().productId(givenProduct.getId()).build());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
        }
    }
}

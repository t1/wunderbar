package test;

import com.github.t1.wunderbar.junit.JUnitWunderBarException;
import com.github.t1.wunderbar.junit.Service;
import com.github.t1.wunderbar.junit.SystemUnderTest;
import com.github.t1.wunderbar.junit.WunderBarExtension;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientException;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import test.ProductResolver.Item;
import test.ProductResolver.Product;
import test.ProductResolver.Products;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;

import static com.github.t1.wunderbar.junit.OngoingStubbing.given;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarExtension
class ProductResolverTest {
    @Service Products products;
    @SystemUnderTest ProductResolver resolver;

    @Test void shouldResolveProduct() {
        var givenProduct = Product.builder().id("x").name("some-product-name").build();
        given(products.product(givenProduct.getId())).willReturn(givenProduct);

        var resolvedProduct = resolver.product(Item.builder().productId(givenProduct.getId()).build());

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
    }

    @Test void shouldResolveTwoProducts() {
        var givenProductA = Product.builder().id("a").name("some-product-a").build();
        var givenProductB = Product.builder().id("b").name("some-product-b").build();
        given(products.product(givenProductA.getId())).willReturn(givenProductA);
        given(products.product(givenProductB.getId())).willReturn(givenProductB);

        var resolvedProductA = resolver.product(Item.builder().productId(givenProductA.getId()).build());
        var resolvedProductB = resolver.product(Item.builder().productId(givenProductB.getId()).build());

        then(resolvedProductA).usingRecursiveComparison().isEqualTo(givenProductA);
        then(resolvedProductB).usingRecursiveComparison().isEqualTo(givenProductB);
    }

    @Test void shouldFailToResolveUnknownProduct() {
        given(products.product("x")).willThrow(new RuntimeException("product x not found"));
        var item = Item.builder().productId("x").build();

        var throwable = catchThrowableOfType(() -> resolver.product(item), GraphQlClientException.class);

        then(throwable.getErrors()).hasSize(1);
        var error = throwable.getErrors().get(0);
        then(error.getMessage()).isEqualTo("product x not found");
    }

    @Nested class StubbingFailures {
        // TODO how can I test that this throws an exception in afterEach?
        //  @Test void shouldFailUnfinishedStubbing() {
        //     given(products.product("x"));
        // }

        @Test void shouldFailToCallGivenWithoutCallToProxyNull() {
            var throwable = catchThrowable(() -> given(null).willReturn(null));

            then(throwable).isInstanceOf(JUnitWunderBarException.class)
                .hasMessage("call `given` only on the response object of a proxy (invocation)");
        }

        @Test void shouldFailToCallGivenWithoutCallToProxyNonNull() {
            @SuppressWarnings("ConstantConditions")
            var throwable = catchThrowable(() -> given(1L).willReturn(null));

            then(throwable).isInstanceOf(JUnitWunderBarException.class)
                .hasMessage("call `given` only once and only on the response object of a proxy (null)");
        }

        @Test void shouldFailToCallTheSameGivenTwice() {
            var givenProduct = Product.builder().id("x").build();
            given(products.product(givenProduct.getId())).willReturn(givenProduct);

            var throwable = catchThrowable(() -> given(products.product(givenProduct.getId())).willReturn(givenProduct));

            then(throwable).isInstanceOf(JUnitWunderBarException.class)
                .hasMessage("call `given` only once and only on the response object of a proxy (null)");
        }
    }

    @Nested class OtherTechnologies {
        @Service UnrecognizableTechnologyService unrecognizableTechnologyService;
        @Service RestService restService;

        @Test void shouldFailToRecognizeTechnology() {
            var throwable = catchThrowable(() -> given(unrecognizableTechnologyService.call()).willReturn(null));

            then(throwable).isInstanceOf(JUnitWunderBarException.class)
                .hasMessage("no technology recognized on " + UnrecognizableTechnologyService.class);
        }

        @Test void shouldCallRestService() {
            var givenProduct = Product.builder().id("r").name("some-product-name").build();
            given(restService.getProduct(givenProduct.id)).willReturn(givenProduct);

            var response = restService.getProduct(givenProduct.id);

            then(response).usingRecursiveComparison().isEqualTo(givenProduct);
        }

        @Test void shouldFailToCallFailingRestService() {
            var productId = "rx";
            given(restService.getProduct(productId)).willThrow(new ForbiddenException());

            var throwable = catchThrowableOfType(() -> restService.getProduct(productId), WebApplicationException.class);

            then(throwable.getResponse().getStatusInfo()).isEqualTo(FORBIDDEN);
        }
    }

    interface UnrecognizableTechnologyService {
        Object call();
    }

    @RegisterRestClient(baseUri = "dummy") @Path("/hello")
    public // RestEasy MP Rest Client requires the interface to be `public`
    interface RestService {
        @Path("/{productId}")
        @GET Product getProduct(@PathParam("productId") String productId);
    }

    @Nested class NestedService {
        @Service Products nestedProducts;

        @Test void shouldFindNestedService() {
            var givenProduct = Product.builder().id("y").build();
            given(nestedProducts.product(givenProduct.getId())).willReturn(givenProduct);

            var resolvedProduct = resolver.product(Item.builder().productId(givenProduct.getId()).build());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
        }
    }

    @Nested class NestedSystemUnderTest {
        @SystemUnderTest ProductResolver nestedResolver;

        @Test void shouldFindNestedProxy() {
            var givenProduct = Product.builder().id("z").build();
            given(products.product(givenProduct.getId())).willReturn(givenProduct);

            var resolvedProduct = nestedResolver.product(Item.builder().productId(givenProduct.getId()).build());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
        }
    }
}

package test.consumer;

import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.WunderBarConsumerExtension;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientApi;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientException;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import test.consumer.ProductResolver.Item;
import test.consumer.ProductResolver.Product;
import test.consumer.ProductResolver.Products;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import static com.github.t1.wunderbar.junit.consumer.ExpectedResponseBuilder.given;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarConsumerExtension
abstract class ProductResolverTest {
    @Service Products products;
    @Service NamedProducts namedProducts;
    @Service ProductsGetter productsGetter;
    @SystemUnderTest ProductResolver resolver;

    @GraphQlClientApi
    interface NamedProducts {
        @Name("p")
        Product productById(String id);
    }

    @GraphQlClientApi
    interface ProductsGetter {
        Product getProduct(String id);
    }

    @Test void shouldResolveProduct() {
        var givenProduct = Product.builder().id("x").name("some-product-name").build();
        given(products.product(givenProduct.getId())).willReturn(givenProduct);

        var resolvedProduct = resolver.product(Item.builder().productId(givenProduct.getId()).build());

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
    }

    @Test void shouldResolveNamedProductMethod() {
        var givenProduct = Product.builder().id("x").name("some-product-name").build();
        given(namedProducts.productById(givenProduct.getId())).willReturn(givenProduct);

        var resolvedProduct = namedProducts.productById(givenProduct.getId());

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
    }

    @Test void shouldResolveProductGetter() {
        var givenProduct = Product.builder().id("x").name("some-product-name").build();
        given(productsGetter.getProduct(givenProduct.getId())).willReturn(givenProduct);

        var resolvedProduct = productsGetter.getProduct(givenProduct.getId());

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
    }

    @DisplayName("test two calls in one test")
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

        var throwable = catchThrowable(() -> resolver.product(item));

        failsWith(throwable, "product x not found", NOT_FOUND);
    }

    void failsWith(Throwable throwable, String message, @SuppressWarnings("SameParameterValue") Status status) {
        then(throwable).isNotNull();
        if (throwable instanceof GraphQlClientException) {
            GraphQlClientException e = (GraphQlClientException) throwable;
            then(e.getErrors()).hasSize(1);
            var error = e.getErrors().get(0);
            then(error.getMessage()).isEqualTo(message);
        } else if (throwable instanceof WebApplicationException) {
            var response = ((WebApplicationException) throwable).getResponse();
            then(response.getStatusInfo()).isEqualTo(status);
            if (!throwable.getMessage().equals(message)) // i.e. not level=UNIT
                then(response.readEntity(String.class)).contains("\"detail\": \"" + message + "\"");
        } else {
            then(throwable.getMessage()).isEqualTo(message);
        }
    }

    @Nested class Rest {
        @Service RestService restService;

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

        @Test void shouldFailToResolveUnknownProduct() {
            given(restService.getProduct("y")).willThrow(new NotFoundException("product y not found"));

            var throwable = catchThrowableOfType(() -> restService.getProduct("y"), WebApplicationException.class);

            failsWith(throwable, "product y not found", NOT_FOUND);
        }
    }

    @RegisterRestClient(baseUri = "dummy") @Path("/hello")
    public // RestEasy MP Rest Client requires the interface to be `public`
    interface RestService {
        @Path("/{productId}")
        @GET Product getProduct(@PathParam("productId") String productId);
    }


    @DisplayName("stubbing failures")
    @Nested class StubbingFailures {
        // TODO how can I test that this throws an exception in afterEach?
        //  @Test void shouldFailUnfinishedStubbing() {
        //     given(products.product("x"));
        // }

        @Test void shouldFailToCallGivenWithoutCallToProxyNull() {
            var throwable = catchThrowable(() -> given(null).willReturn(null));

            then(throwable).hasMessage("Stubbing mismatch: call `given` exactly once on the response object of a proxy call");
        }

        @Test void shouldFailToCallGivenWithoutCallToProxyNonNull() {
            var throwable = catchThrowable(() -> given(1L).willReturn(null));

            then(throwable).hasMessage("Stubbing mismatch: call `given` exactly once on the response object of a proxy call");
        }

        @Test void shouldFailToCallTheSameExpectedResponseBuilderTwice() {
            var givenProduct = Product.builder().id("x").build();
            var expectedResponseBuilder = given(products.product(givenProduct.getId()));
            expectedResponseBuilder.willReturn(givenProduct);

            var throwable = catchThrowable(() -> expectedResponseBuilder.willReturn(givenProduct));

            then(throwable).hasMessage("Stubbing mismatch: call `given` exactly once on the response object of a proxy call");
        }
    }

    @DisplayName("nested/service")
    @Nested class NestedService {
        @Service Products nestedProducts;

        @Test void shouldFindNestedService() {
            var givenProduct = Product.builder().id("y").build();
            given(nestedProducts.product(givenProduct.getId())).willReturn(givenProduct);

            var resolvedProduct = resolver.product(Item.builder().productId(givenProduct.getId()).build());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
        }
    }

    @DisplayName("nested/sut")
    @Nested class NestedSystemUnderTest {
        @SystemUnderTest ProductResolver nestedResolver;

        @Test void shouldFindNestedProxy() {
            var givenProduct = Product.builder().id("z").build();
            given(products.product(givenProduct.getId())).willReturn(givenProduct);

            var resolvedProduct = nestedResolver.product(Item.builder().productId(givenProduct.getId()).build());

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
        }
    }

    @Nested class ReturnTypes {
        @Service ReturnTypesService service;

        @Test void shouldGetBoolean() {
            given(service.getBoolean()).willReturn(true);
            then(service.getBoolean()).isTrue();
        }

        @Test void shouldGetByte() {
            given(service.getByte()).willReturn((byte) 0x12);
            then(service.getByte()).isEqualTo((byte) 0x12);
        }

        @Test void shouldGetChar() {
            given(service.getChar()).willReturn('c');
            then(service.getChar()).isEqualTo('c');
        }

        @Test void shouldGetShort() {
            given(service.getShort()).willReturn((short) 0x12);
            then(service.getShort()).isEqualTo((short) 0x12);
        }

        @Test void shouldGetInt() {
            given(service.getInt()).willReturn(0x12);
            then(service.getInt()).isEqualTo(0x12);
        }

        @Test void shouldGetLong() {
            given(service.getLong()).willReturn(0x12L);
            then(service.getLong()).isEqualTo(0x12L);
        }

        @Test void shouldGetFloat() {
            given(service.getFloat()).willReturn(1.2f);
            then(service.getFloat()).isEqualTo(1.2f);
        }

        @Test void shouldGetDouble() {
            given(service.getDouble()).willReturn(1.2d);
            then(service.getDouble()).isEqualTo(1.2d);
        }
    }

    @GraphQlClientApi
    public interface ReturnTypesService {
        boolean getBoolean();

        byte getByte();

        char getChar();

        short getShort();

        int getInt();

        long getLong();

        float getFloat();

        double getDouble();
    }
}

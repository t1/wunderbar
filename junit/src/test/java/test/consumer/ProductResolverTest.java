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
import java.io.Closeable;

import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
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

        var resolvedProduct = resolver.product(new Item(givenProduct.getId()));

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

        var resolvedProductA = resolver.product(new Item(givenProductA.getId()));
        var resolvedProductB = resolver.product(new Item(givenProductB.getId()));

        then(resolvedProductA).usingRecursiveComparison().isEqualTo(givenProductA);
        then(resolvedProductB).usingRecursiveComparison().isEqualTo(givenProductB);
    }

    @Test void shouldFailToResolveUnknownProduct() {
        given(products.product("x")).willThrow(new ProductNotFoundException("x"));

        var throwable = catchThrowable(() -> resolver.product(new Item("x")));

        failsWith(throwable, "product-not-found", "product x not found", NOT_FOUND);
    }

    static class ProductNotFoundException extends RuntimeException {
        ProductNotFoundException(String id) { super("product " + id + " not found"); }
    }

    void failsWith(Throwable throwable, String code, String message, Status status) {
        then(throwable).isNotNull();
        if (throwable instanceof GraphQlClientException) {
            GraphQlClientException e = (GraphQlClientException) throwable;
            then(e.getErrors()).hasSize(1);
            var error = e.getErrors().get(0);
            then(error.getMessage()).isEqualTo(message);
            then(error.getErrorCode()).isEqualTo(code);
        } else if (throwable instanceof WebApplicationException) {
            var response = ((WebApplicationException) throwable).getResponse();
            then(response.getStatusInfo()).isEqualTo(status);
            if (!throwable.getMessage().equals(message)) { // i.e. not level=UNIT
                var body = response.readEntity(String.class);
                then(body).contains("\"detail\": \"" + message + "\"");
                then(body).contains("\"type\": \"urn:problem-type:" + code + "\"");
            }
        } else /* level=UNIT */ {
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
            var productId = "ry";
            given(restService.getProduct(productId)).willThrow(new IllegalStateException("some internal error"));

            var throwable = catchThrowable(() -> restService.getProduct(productId));

            failsWith(throwable, "illegal-state", "some internal error", INTERNAL_SERVER_ERROR);
        }

        @Test void shouldFailToCallForbiddenRestService() {
            var productId = "rx";
            given(restService.getProduct(productId)).willThrow(new ForbiddenException());

            var throwable = catchThrowableOfType(() -> restService.getProduct(productId), WebApplicationException.class);

            // no response body, so failsWith doesn't work
            then(throwable.getResponse().getStatusInfo()).isEqualTo(FORBIDDEN);
        }

        @Test void shouldFailToResolveUnknownProduct() {
            given(restService.getProduct("y")).willThrow(new NotFoundException("product y not found"));

            var throwable = catchThrowableOfType(() -> restService.getProduct("y"), WebApplicationException.class);

            failsWith(throwable, "not-found", "product y not found", NOT_FOUND);
        }
    }

    @RegisterRestClient(baseUri = "dummy") @Path("/products")
    public // RestEasy MP Rest Client requires the interface to be `public`
    interface RestService extends Closeable {
        @Path("/{productId}")
        @GET Product getProduct(@PathParam("productId") String productId);
    }


    @DisplayName("stubbing failures")
    @Nested class StubbingFailures {
        // how can I test that this throws an "unfinished stubbing" exception in afterEach?
        //x  @Test void shouldFailUnfinishedStubbing() {
        //x     given(products.product("x"));
        //x }

        @Test void shouldFailToCallGivenWithoutCallToProxyNull() {
            var throwable = catchThrowable(() -> given(null).willThrow(new RuntimeException("unreachable")));

            then(throwable).hasMessage("Stubbing mismatch: call `given` exactly once on the response object of a proxy call");
        }

        @Test void shouldFailToCallGivenWithoutCallToProxyNonNull() {
            var throwable = catchThrowable(() -> given(1L).willThrow(new RuntimeException("unreachable")));

            then(throwable).hasMessage("Stubbing mismatch: call `given` exactly once on the response object of a proxy call");
        }

        @Test void shouldFailToCallTheSameExpectedResponseBuilderTwice() {
            var givenProduct = Product.builder().id("x").build();
            var stub = given(products.product(givenProduct.getId()));
            stub.willReturn(givenProduct);

            var throwable = catchThrowable(() -> stub.willReturn(givenProduct));

            then(throwable).hasMessage("Stubbing mismatch: call `given` exactly once on the response object of a proxy call");
        }
    }

    @DisplayName("nested/service")
    @Nested class NestedService {
        @Service Products nestedProducts;

        @Test void shouldFindNestedService() {
            var givenProduct = Product.builder().id("y").build();
            given(nestedProducts.product(givenProduct.getId())).willReturn(givenProduct);

            var resolvedProduct = resolver.product(new Item(givenProduct.getId()));

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
        }
    }

    @DisplayName("nested/sut")
    @Nested class NestedSystemUnderTest {
        @SystemUnderTest ProductResolver nestedResolver;

        @Test void shouldFindNestedProxy() {
            var givenProduct = Product.builder().id("z").build();
            given(products.product(givenProduct.getId())).willReturn(givenProduct);

            var resolvedProduct = nestedResolver.product(new Item(givenProduct.getId()));

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
    interface ReturnTypesService {
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

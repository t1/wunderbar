package test.consumer;

import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import io.smallrye.graphql.client.GraphQLClientException;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;

import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.baseUri;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.createService;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;
import static test.consumer.TestData.someProduct;

@WunderBarApiConsumer
abstract class ProductResolverTest {

    @Service Products products;
    @Service NamedProducts namedProducts;
    @Service ProductsGetter productsGetter;
    @SystemUnderTest ProductResolver resolver;

    @GraphQLClientApi
    interface NamedProducts {
        @Name("p")
        Product productById(String id);
    }

    @GraphQLClientApi
    interface ProductsGetter {
        Product getProduct(String id);
    }

    @Nested class UnrecognizableTechnologies {
        @Test void shouldFailToRecognizeTechnology() {
            var throwable = catchThrowable(() -> createService(UnrecognizableTechnologyService.class));

            then(throwable).hasMessage("no technology recognized on " + UnrecognizableTechnologyService.class);
        }
    }

    interface UnrecognizableTechnologyService {
        @SuppressWarnings("unused")
        Object call();
    }

    Product product = someProduct();

    @Test void shouldResolveProduct() {
        given(products.product(product.getId())).willReturn(product);
        given(products.product("not-actually-called")).willReturn(Product.builder().id("unreachable").build());

        var resolvedProduct = resolver.product(new Item(product.getId()));

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product);
    }

    @Test void shouldResolveNamedProductMethod() {
        given(namedProducts.productById(product.getId())).willReturn(product);

        var resolvedProduct = namedProducts.productById(product.getId());

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product);
    }

    @Test void shouldResolveProductGetter() {
        given(productsGetter.getProduct(product.getId())).willReturn(product);

        var resolvedProduct = productsGetter.getProduct(product.getId());

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product);
    }

    @Test void shouldUpdateProduct() {
        given(products.product(product.getId())).willReturn(product);
        given(products.patch(new Product().withId(product.getId()).withPrice(12_99))).willReturn(product.withPrice(12_99));

        var preCheck = resolver.product(new Item(product.getId()));
        var resolvedProduct = resolver.productWithPriceUpdate(new Item(product.getId()), 12_99);

        then(preCheck).usingRecursiveComparison().isEqualTo(product);
        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product.withPrice(12_99));
    }

    @Test void shouldFailToResolveUnknownProduct() {
        given(products.product("x")).willThrow(new ProductNotFoundException("x"));

        var throwable = catchThrowable(() -> resolver.product(new Item("x")));

        failsWith(throwable, "product-not-found", "product x not found", NOT_FOUND);
    }

    static class ProductNotFoundException extends RuntimeException {
        ProductNotFoundException(String id) {super("product " + id + " not found");}
    }

    void failsWith(Throwable throwable, String code, String message, Status status) {
        then(throwable).isNotNull();
        if (throwable instanceof GraphQLClientException) {
            GraphQLClientException e = (GraphQLClientException) throwable;
            then(e.getErrors()).hasSize(1);
            var error = e.getErrors().get(0);
            then(error.getMessage()).isEqualTo(message);
            then(error.getExtensions().get("code")).isEqualTo(code); // TODO simplify after #1224 is merged
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

    void verifyBaseUri(URI baseUri) {then(baseUri.toString()).startsWith("http://localhost:");}

    @Nested class StaticMethods {
        @Test void shouldGetBaseUri() {
            var baseUri = baseUri(products);
            System.out.println("actual service uri: " + baseUri);
            verifyBaseUri(baseUri);
        }

        @Test void shouldFailToGetBaseUriFromNonProxy() {
            var throwable = catchThrowable(() -> baseUri(resolver));

            then(throwable).isInstanceOf(IllegalArgumentException.class).hasMessage("not a proxy instance");
        }

        @Test void shouldFailToGetBaseUriFromNonServiceProxy() {
            var nonServiceProxy = Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[]{Runnable.class},
                (Object proxy, Method method, Object[] args) -> {throw new RuntimeException("unexpected");});

            var throwable = catchThrowable(() -> baseUri(nonServiceProxy));

            then(throwable).isInstanceOf(IllegalArgumentException.class).hasMessage("not a service proxy instance");
        }

        @Test void shouldFailToGetBaseUriFromNonServiceProxy2() {
            var nonServiceProxy = Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[]{Runnable.class}, this::dummyHandler);

            var throwable = catchThrowable(() -> baseUri(nonServiceProxy));

            then(throwable).isInstanceOf(IllegalArgumentException.class).hasMessage("not a service proxy instance");
        }

        private Object dummyHandler(Object o, Method method, Object[] objects) {throw new RuntimeException("unexpected");}

        @Test void shouldManuallyBuildProxy() {
            var proxy = createService(Products.class);
            var givenProduct = Product.builder().id("proxy").name("some-product-name").build();
            given(proxy.product(givenProduct.getId())).willReturn(givenProduct);
            resolver.products = proxy;

            var resolvedProduct = resolver.product(new Item(givenProduct.getId()));

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
        }
    }

    @Nested class Rest {
        @Service RestService restService;

        @Test void shouldGetProduct() {
            var givenProduct = Product.builder().id("r").name("some-product-name").build();
            given(restService.getProduct(givenProduct.id)).willReturn(givenProduct);

            var response = restService.getProduct(givenProduct.id);

            then(response).usingRecursiveComparison().isEqualTo(givenProduct);
        }

        @Test void shouldGetTwoProducts() {
            var givenProduct1 = Product.builder().id("r1").name("some-product-name1").build();
            var givenProduct2 = Product.builder().id("r2").name("some-product-name2").build();
            given(restService.getProduct(givenProduct1.id)).willReturn(givenProduct1);
            given(restService.getProduct(givenProduct2.id)).willReturn(givenProduct2);

            var response1 = restService.getProduct(givenProduct1.id);
            var response2 = restService.getProduct(givenProduct2.id);

            then(response1).usingRecursiveComparison().isEqualTo(givenProduct1);
            then(response2).usingRecursiveComparison().isEqualTo(givenProduct2);
        }

        @Test void shouldFailToGetFailingProduct() {
            var productId = "ry";
            given(restService.getProduct(productId)).willThrow(new IllegalStateException("some internal error"));

            var throwable = catchThrowable(() -> restService.getProduct(productId));

            failsWith(throwable, "illegal-state", "some internal error", INTERNAL_SERVER_ERROR);
        }

        @Test void shouldFailToGetForbiddenProduct() {
            var productId = "rx";
            given(restService.getProduct(productId)).willThrow(new ForbiddenException());

            var throwable = catchThrowableOfType(() -> restService.getProduct(productId), WebApplicationException.class);

            // no response body, so failsWith doesn't work
            then(throwable.getResponse().getStatusInfo()).isEqualTo(FORBIDDEN);
        }

        @Test void shouldFailToGetUnknownProduct() {
            given(restService.getProduct("y")).willThrow(new NotFoundException("product y not found"));

            var throwable = catchThrowableOfType(() -> restService.getProduct("y"), WebApplicationException.class);

            failsWith(throwable, "not-found", "product y not found", NOT_FOUND);
        }

        @Test void shouldPatchProduct() {
            var givenProduct = Product.builder().id("rp").name("some-product-name").price(15_99).build();
            given(restService.patch("rp", new Product().withPrice(12_99))).willReturn(1);
            given(restService.getProduct("rp")).willReturn(givenProduct.withPrice(12_99));

            restService.patch("rp", new Product().withPrice(12_99));
            var updated = restService.getProduct("rp");

            then(updated).usingRecursiveComparison().isEqualTo(givenProduct.withPrice(12_99));
        }
    }

    @RegisterRestClient(baseUri = "dummy") @Path("/products")
    public // RestEasy MP Rest Client requires the interface to be `public`
    interface RestService extends Closeable {
        @Path("/{productId}")
        @GET Product getProduct(@PathParam("productId") String productId);

        @Path("/{productId}")
        @POST
        int patch(@PathParam("productId") String id, Product patch);
    }


    @DisplayName("stubbing failures")
    @Nested class StubbingFailures {
        //  @Test // we can't test that this throws an "unfinished stubbing" exception in afterEach
        @SuppressWarnings("unused")
        void shouldFailUnfinishedStubbing() {
            given(products.product("x"));
        }

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

    @GraphQLClientApi
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

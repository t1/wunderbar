package test.consumer;

import com.github.t1.wunderbar.junit.Register;
import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.consumer.BarWriter;
import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.Some;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.Technology;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import test.consumer.ProductResolver.Item;
import test.consumer.ProductResolver.NamedProducts;
import test.consumer.ProductResolver.Product;
import test.consumer.ProductResolver.Products;
import test.consumer.ProductResolver.ProductsGetter;
import test.consumer.ProductsGateway.ProductsRestClient;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response.StatusType;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;

import static com.github.t1.wunderbar.junit.assertions.GraphQLClientExceptionAssert.GRAPHQL_CLIENT_EXCEPTION;
import static com.github.t1.wunderbar.junit.assertions.ProblemDetailsAssert.PROBLEM_DETAILS;
import static com.github.t1.wunderbar.junit.assertions.WunderBarBDDAssertions.then;
import static com.github.t1.wunderbar.junit.consumer.Service.DEFAULT_ENDPOINT;
import static com.github.t1.wunderbar.junit.consumer.Technology.GRAPHQL;
import static com.github.t1.wunderbar.junit.consumer.Technology.REST;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.baseUri;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.createProxy;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.createService;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.once;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.catchThrowable;

@WunderBarApiConsumer
abstract class ProductResolverTest {
    @Service(endpoint = "{endpoint()}") Products products;
    @Service(endpoint = "{endpoint()}") NamedProducts namedProducts;
    @Service(endpoint = "{endpoint()}") ProductsGetter productsGetter;
    @SystemUnderTest ProductResolver resolver;

    String endpoint() {return DEFAULT_ENDPOINT;}

    @Register SomeProduct productGenerator;
    @Some Product product;
    Item item;

    @BeforeEach void setUpVariables() {item = new Item(product.id);}

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

    @Test void shouldResolveProduct() {
        given(products.product(product.id)).returns(product);

        var resolvedProduct = resolver.product(item);

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product);
    }

    @Test void shouldResolveProductWithoutRecording(BarWriter bar) {
        var before = bar.counter().get();
        given(products.product(product.id)).withoutRecording().returns(product);

        var resolvedProduct = resolver.product(item);

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product);
        then(bar.counter().get()).isEqualTo(before);
    }

    @Test void shouldResolveProductWithWillReturn() {
        given(products.product(product.id)).willReturn(product);

        var resolvedProduct = resolver.product(item);

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product);
    }

    @Test void shouldFailToResolveProductTwice() {
        given(products.product(product.id)).returns(once(), product);
        given(products.product("not-actually-called")).returns(Product.builder().id("unreachable").build());

        then(resolver.product(item)).as("first call").usingRecursiveComparison().isEqualTo(product);

        var throwable = catchThrowable(() -> resolver.product(item));

        thenFailedDepletion(throwable);
    }

    protected void thenFailedDepletion(Throwable throwable) {
        then(throwable).as("second call")
            .isInstanceOf(WunderBarException.class)
            .hasMessage("expectation is depleted [Depletion(maxCallCount=1)] on call #2");
    }

    @Test void shouldResolveNamedProductMethod() {
        given(namedProducts.productById(product.id)).returns(product);

        var resolvedProduct = resolver.namedProduct(item);

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product);
    }

    @Test void shouldResolveProductGetter() {
        given(productsGetter.getProduct(product.id)).returns(product);

        var resolvedProduct = resolver.productGetter(item);

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product);
    }

    @Test void shouldUpdateProduct(@Some int newPrice) {
        given(products.product(product.id)).returns(product);
        given(products.patch(new Product().withId(product.id).withPrice(newPrice))).returns(product.withPrice(newPrice));
        var preCheck = resolver.product(item);
        then(preCheck).usingRecursiveComparison().isEqualTo(product);

        var resolvedProduct = resolver.productWithPriceUpdate(item, newPrice);

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product.withPrice(newPrice));
    }

    @Test void shouldFailToResolveUnknownProduct() {
        given(products.product(product.id)).willThrow(new ProductNotFoundException(product.id));

        var throwable = catchThrowable(() -> resolver.product(item));

        thenGraphQlError(throwable, "product-not-found", "product " + product.id + " not found");
    }

    @Test void shouldFailToResolveSameUnknownProductTwice() {
        given(products.product(product.id)).willThrow(once(), new ProductNotFoundException(product.id));

        thenGraphQlError(catchThrowable(() -> resolver.product(item)), "product-not-found", "product " + product.id + " not found");

        var throwable = catchThrowable(() -> resolver.product(item));

        thenFailedDepletion(throwable);
    }

    @Test void shouldFailToResolveForbiddenProduct() {
        given(products.product(product.id)).willThrow(new ProductForbiddenException(product.id));

        var throwable = catchThrowable(() -> resolver.product(item));

        thenGraphQlError(throwable, "product-forbidden", "product " + product.id + " is forbidden");
    }

    protected void thenGraphQlError(Throwable throwable, String errorCode, String message) {
        then(throwable).asInstanceOf(GRAPHQL_CLIENT_EXCEPTION)
            .hasErrorCode(errorCode)
            .withMessage(message);
    }

    abstract void verifyBaseUri(URI baseUri, Technology technology);

    @Nested class StaticMethods {
        @Test void shouldGetBaseUri() {
            var baseUri = baseUri(products);

            verifyBaseUri(baseUri, GRAPHQL);
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
            var proxy = createProxy(Products.class, Service.DEFAULT.withEndpoint(endpoint()));
            var givenProduct = Product.builder().id("proxy").name("some-product-name").build();
            given(proxy.getStubbingProxy().product(givenProduct.getId())).returns(givenProduct);
            resolver.products = proxy.getSutProxy();

            var resolvedProduct = resolver.product(new Item(givenProduct.getId()));

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
        }
    }

    @Nested class Rest {
        @Service(endpoint = "{endpoint()}") ProductsRestClient restService;
        @SystemUnderTest ProductsGateway gateway;

        @Test void shouldGetBaseUri() {
            var baseUri = baseUri(restService);

            verifyBaseUri(baseUri, REST);
        }

        @Test void shouldGetProduct() {
            given(restService.product(product.id)).returns(product);

            var response = gateway.product(item);

            then(response).usingRecursiveComparison().isEqualTo(product);
        }

        @Test void shouldGetTwoProducts() {
            var givenProduct1 = Product.builder().id("r1").name("some-product-name1").build();
            var givenProduct2 = Product.builder().id("r2").name("some-product-name2").build();
            given(restService.product(givenProduct1.id)).returns(givenProduct1);
            given(restService.product(givenProduct2.id)).returns(givenProduct2);

            var response1 = gateway.product(new Item(givenProduct1.id));
            var response2 = gateway.product(new Item(givenProduct2.id));

            then(response1).usingRecursiveComparison().isEqualTo(givenProduct1);
            then(response2).usingRecursiveComparison().isEqualTo(givenProduct2);
        }

        @Test void shouldFailToGetFailingProduct() {
            given(restService.product(product.id)).willThrow(new IllegalStateException("some internal error"));

            var throwable = catchThrowable(() -> gateway.product(item));

            thenRestError(throwable, INTERNAL_SERVER_ERROR, "illegal-state", "some internal error");
        }

        @Test void shouldFailToGetForbiddenProduct() {
            given(restService.product(product.id)).willThrow(new ForbiddenException("product " + product.id + " is forbidden"));

            var throwable = catchThrowable(() -> gateway.product(item));

            thenRestError(throwable, FORBIDDEN, "forbidden", "product " + product.id + " is forbidden");
        }

        @Test void shouldFailToGetUnknownProduct() {
            given(restService.product(product.id)).willThrow(new NotFoundException("product " + product.id + " not found"));

            var throwable = catchThrowable(() -> gateway.product(item));

            thenRestError(throwable, NOT_FOUND, "not-found", "product " + product.id + " not found");
        }

        @Test void shouldPatchProduct(@Some int newPrice) {
            var patchedProduct = product.withPrice(newPrice);
            var patch = Product.builder().id(product.id).price(newPrice).build();
            given(restService.patch(patch)).returns(patchedProduct);

            var updated = gateway.productWithPriceUpdate(item, newPrice);

            then(updated).usingRecursiveComparison().isEqualTo(patchedProduct);
        }
    }

    protected void thenRestError(Throwable throwable, StatusType status, String typeSuffix, String detail) {
        then(throwable).asInstanceOf(PROBLEM_DETAILS)
            .hasStatus(status)
            .hasType("urn:problem-type:" + typeSuffix)
            .hasDetail(detail);
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
            stub.returns(givenProduct);

            var throwable = catchThrowable(() -> stub.returns(givenProduct));

            then(throwable).hasMessage("Stubbing mismatch: call `given` exactly once on the response object of a proxy call");
        }
    }

    @DisplayName("nested/service")
    @Nested class NestedService {
        @Service(endpoint = "{endpoint()}") Products nestedProducts;

        @Test void shouldFindNestedService() {
            var givenProduct = Product.builder().id("y").build();
            given(nestedProducts.product(givenProduct.getId())).returns(givenProduct);

            var resolvedProduct = resolver.product(new Item(givenProduct.getId()));

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
        }
    }

    @DisplayName("nested/sut")
    @Nested class NestedSystemUnderTest {
        @SystemUnderTest ProductResolver nestedResolver;

        @Test void shouldFindNestedProxy() {
            var givenProduct = Product.builder().id("z").build();
            given(products.product(givenProduct.getId())).returns(givenProduct);

            var resolvedProduct = nestedResolver.product(new Item(givenProduct.getId()));

            then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
        }
    }

    @Nested class ReturnTypes {
        ReturnTypesService stub;
        ReturnTypesService service;

        @BeforeEach
        void setUp() {
            var proxy = createProxy(ReturnTypesService.class, Service.DEFAULT.withEndpoint(endpoint()));
            stub = proxy.getStubbingProxy();
            service = proxy.getSutProxy();
        }

        @Test void shouldGetBoolean() {
            given(stub.getBoolean()).returns(true);
            then(service.getBoolean()).isTrue();
        }

        @Test void shouldGetByte() {
            given(stub.getByte()).returns((byte) 0x12);
            then(service.getByte()).isEqualTo((byte) 0x12);
        }

        @Test void shouldGetChar() {
            given(stub.getChar()).returns('c');
            then(service.getChar()).isEqualTo('c');
        }

        @Test void shouldGetShort() {
            given(stub.getShort()).returns((short) 0x12);
            then(service.getShort()).isEqualTo((short) 0x12);
        }

        @Test void shouldGetInt() {
            given(stub.getInt()).returns(0x12);
            then(service.getInt()).isEqualTo(0x12);
        }

        @Test void shouldGetLong() {
            given(stub.getLong()).returns(0x12L);
            then(service.getLong()).isEqualTo(0x12L);
        }

        @Test void shouldGetFloat() {
            given(stub.getFloat()).returns(1.2f);
            then(service.getFloat()).isEqualTo(1.2f);
        }

        @Test void shouldGetDouble() {
            given(stub.getDouble()).returns(1.2d);
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

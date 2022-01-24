package test.consumer;

import com.github.t1.wunderbar.junit.consumer.Service;
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
import static com.github.t1.wunderbar.junit.consumer.Service.DEFAULT_ENDPOINT;
import static com.github.t1.wunderbar.junit.consumer.Technology.GRAPHQL;
import static com.github.t1.wunderbar.junit.consumer.Technology.REST;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.baseUri;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.createProxy;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.createService;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static com.github.t1.wunderbar.junit.provider.WunderBarBDDAssertions.then;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.catchThrowable;
import static test.consumer.TestData.someInt;
import static test.consumer.TestData.someProduct;

@WunderBarApiConsumer
abstract class ProductResolverTest {
    @Service(endpoint = "{endpoint()}") Products products;
    @Service(endpoint = "{endpoint()}") NamedProducts namedProducts;
    @Service(endpoint = "{endpoint()}") ProductsGetter productsGetter;
    @SystemUnderTest ProductResolver resolver;

    String endpoint() {return DEFAULT_ENDPOINT;}

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
    String productId = product.getId();
    Item item = new Item(productId);

    @Test void shouldResolveProduct() {
        given(products.product(productId)).willReturn(product);
        given(products.product("not-actually-called")).willReturn(Product.builder().id("unreachable").build());

        var resolvedProduct = resolver.product(item);

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product);
    }

    @Test void shouldResolveNamedProductMethod() {
        given(namedProducts.productById(productId)).willReturn(product);

        var resolvedProduct = resolver.namedProduct(item);

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product);
    }

    @Test void shouldResolveProductGetter() {
        given(productsGetter.getProduct(productId)).willReturn(product);

        var resolvedProduct = resolver.productGetter(item);

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product);
    }

    @Test void shouldUpdateProduct() {
        var newPrice = someInt();
        given(products.product(productId)).willReturn(product);
        given(products.patch(new Product().withId(productId).withPrice(newPrice))).willReturn(product.withPrice(newPrice));
        var preCheck = resolver.product(item);
        then(preCheck).usingRecursiveComparison().isEqualTo(product);

        var resolvedProduct = resolver.productWithPriceUpdate(item, newPrice);

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product.withPrice(newPrice));
    }

    @Test void shouldFailToResolveUnknownProduct() {
        given(products.product(productId)).willThrow(new ProductNotFoundException(productId));

        var throwable = catchThrowable(() -> resolver.product(item));

        thenGraphQlError(throwable, "product-not-found", "product " + productId + " not found");
    }

    @Test void shouldFailToResolveForbiddenProduct() {
        given(products.product(productId)).willThrow(new ProductForbiddenException(productId));

        var throwable = catchThrowable(() -> resolver.product(item));

        thenGraphQlError(throwable, "product-forbidden", "product " + productId + " is forbidden");
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
            given(proxy.getStubbingProxy().product(givenProduct.getId())).willReturn(givenProduct);
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
            given(restService.product(productId)).willReturn(product);

            var response = gateway.product(item);

            then(response).usingRecursiveComparison().isEqualTo(product);
        }

        @Test void shouldGetTwoProducts() {
            var givenProduct1 = Product.builder().id("r1").name("some-product-name1").build();
            var givenProduct2 = Product.builder().id("r2").name("some-product-name2").build();
            given(restService.product(givenProduct1.id)).willReturn(givenProduct1);
            given(restService.product(givenProduct2.id)).willReturn(givenProduct2);

            var response1 = gateway.product(new Item(givenProduct1.id));
            var response2 = gateway.product(new Item(givenProduct2.id));

            then(response1).usingRecursiveComparison().isEqualTo(givenProduct1);
            then(response2).usingRecursiveComparison().isEqualTo(givenProduct2);
        }

        @Test void shouldFailToGetFailingProduct() {
            given(restService.product(productId)).willThrow(new IllegalStateException("some internal error"));

            var throwable = catchThrowable(() -> gateway.product(item));

            thenRestError(throwable, INTERNAL_SERVER_ERROR, "illegal-state", "some internal error");
        }

        @Test void shouldFailToGetForbiddenProduct() {
            given(restService.product(productId)).willThrow(new ForbiddenException("product " + productId + " is forbidden"));

            var throwable = catchThrowable(() -> gateway.product(item));

            thenRestError(throwable, FORBIDDEN, "forbidden", "product " + productId + " is forbidden");
        }

        @Test void shouldFailToGetUnknownProduct() {
            given(restService.product(productId)).willThrow(new NotFoundException("product " + productId + " not found"));

            var throwable = catchThrowable(() -> gateway.product(item));

            thenRestError(throwable, NOT_FOUND, "not-found", "product " + productId + " not found");
        }

        @Test void shouldPatchProduct() {
            int newPrice = someInt();
            var patchedProduct = product.withPrice(newPrice);
            var patch = Product.builder().id(productId).price(newPrice).build();
            given(restService.patch(patch)).willReturn(patchedProduct);

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
            stub.willReturn(givenProduct);

            var throwable = catchThrowable(() -> stub.willReturn(givenProduct));

            then(throwable).hasMessage("Stubbing mismatch: call `given` exactly once on the response object of a proxy call");
        }
    }

    @DisplayName("nested/service")
    @Nested class NestedService {
        @Service(endpoint = "{endpoint()}") Products nestedProducts;

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
        ReturnTypesService stub;
        ReturnTypesService service;

        @BeforeEach
        void setUp() {
            var proxy = createProxy(ReturnTypesService.class, Service.DEFAULT.withEndpoint(endpoint()));
            stub = proxy.getStubbingProxy();
            service = proxy.getSutProxy();
        }

        @Test void shouldGetBoolean() {
            given(stub.getBoolean()).willReturn(true);
            then(service.getBoolean()).isTrue();
        }

        @Test void shouldGetByte() {
            given(stub.getByte()).willReturn((byte) 0x12);
            then(service.getByte()).isEqualTo((byte) 0x12);
        }

        @Test void shouldGetChar() {
            given(stub.getChar()).willReturn('c');
            then(service.getChar()).isEqualTo('c');
        }

        @Test void shouldGetShort() {
            given(stub.getShort()).willReturn((short) 0x12);
            then(service.getShort()).isEqualTo((short) 0x12);
        }

        @Test void shouldGetInt() {
            given(stub.getInt()).willReturn(0x12);
            then(service.getInt()).isEqualTo(0x12);
        }

        @Test void shouldGetLong() {
            given(stub.getLong()).willReturn(0x12L);
            then(service.getLong()).isEqualTo(0x12L);
        }

        @Test void shouldGetFloat() {
            given(stub.getFloat()).willReturn(1.2f);
            then(service.getFloat()).isEqualTo(1.2f);
        }

        @Test void shouldGetDouble() {
            given(stub.getDouble()).willReturn(1.2d);
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

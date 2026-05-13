package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.common.Internal;
import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.consumer.Service.Literal;

import java.lang.reflect.InvocationHandler;
import java.net.URI;
import java.util.Objects;

/// Static methods for building expectations, etc.
///
/// See also: [given], [createService], [baseUri]
public class WunderbarExpectationBuilder<T> {
    private WunderbarExpectationBuilder() {}

    /// Starts to specify the behavior of the [Service] API that the test expects.
    /// The parameter is the result of a call to your API interface;
    /// call [returns] (a.k.a. [willReturn]) or [willThrow] on the result of this method; e.g.:
    /// ```
    /// given(api.findProduct(ID)).returns(PRODUCT);
    /// ```
    public static <T> WunderbarExpectationBuilder<T> given(T dummyValue) {
        if (buildingExpectation == null || !Objects.equals(dummyValue, buildingExpectation.nullValue()))
            throw new StubbingMismatchException();
        return new WunderbarExpectationBuilder<>();
    }

    public @Internal static WunderBarExpectation buildingExpectation;


    public static Depletion always() {return times(Integer.MAX_VALUE);}

    public static Depletion once() {return times(1);}

    public static Depletion times(int maxValue) {return Depletion.builder().maxCallCount(maxValue).build();}


    /// Specifies that the API returns this object as a response.
    public void willReturn(T response) {returns(response);}

    /// Specifies that the API returns this object as a response.
    public void returns(T response) {returns(always(), response);}

    /// Specifies that the API returns this object as a response with a custom depletion policy.
    public void returns(Depletion depletion, T response) {
        if (buildingExpectation == null) throw new StubbingMismatchException();
        try {
            buildingExpectation.returns(depletion, response);
        } finally {
            buildingExpectation = null;
        }
    }

    /// Specifies that the API throws this exception as a response, e.g.:
    /// ```
    /// given(api.findProduct(ID)).willThrow(new NotFoundException("product ID not found"));
    /// ```
    ///
    /// Note that your [SystemUnderTest] may need to handle specific (mainly business) errors, e.g. to distinguish
    /// a `404 Not Found` for technical reasons from when a specific product id is not found.
    /// It can do so by checking the **`code`** that is returned for [integration][Level.INTEGRATION] tests:
    /// - If the API is a REST service, the mock service returns the status code of a `WebApplicationException`
    ///   (if it is one) and a [RFC-7807](https://datatracker.ietf.org/doc/html/rfc7807) style body with:
    ///     - a `detail` field containing the exception message,
    ///     - a `title` field containing the exception class name, and
    ///     - a `type` field containing the **`code`** derived from the exception type name.
    /// - If the API is a GraphQL service, the mock service returns an `error` with:
    ///     - a `message` field containing the exception message and
    ///     - a **`code`** extension field containing the code derived from the exception type name.
    ///
    /// The **`code`** is derived from the simple name of the exception without the `Exception` suffix,
    /// by converting camel case to kebab case, e.g. `ProductNotFoundException` becomes `product-not-found`.
    /// These are also important requirements for the service to implement.
    public void willThrow(Exception exception) {willThrow(always(), exception);}

    /// Specifies that the API throws this exception as a response, e.g.:
    /// ```
    /// given(api.findProduct(ID)).willThrow(new NotFoundException("product ID not found"));
    /// ```
    ///
    /// Note that your [SystemUnderTest] may need to handle specific (mainly business) errors, e.g. to distinguish
    /// a `404 Not Found` for technical reasons from when a specific product id is not found.
    /// It can do so by checking the **`code`** that is returned for [integration][Level.INTEGRATION] tests:
    /// - If the API is a REST service, the mock service returns the status code of a `WebApplicationException`
    ///   (if it is one) and a [RFC-7807](https://datatracker.ietf.org/doc/html/rfc7807) style body with:
    ///     - a `detail` field containing the exception message,
    ///     - a `title` field containing the exception class name, and
    ///     - a `type` field containing the **`code`** derived from the exception type name.
    /// - If the API is a GraphQL service, the mock service returns an `error` with:
    ///     - a `message` field containing the exception message and
    ///     - a **`code`** extension field containing the code derived from the exception type name.
    ///
    /// The **`code`** is derived from the simple name of the exception without the `Exception` suffix,
    /// by converting camel case to kebab case, e.g. `ProductNotFoundException` becomes `product-not-found`.
    /// These are also important requirements for the service to implement.
    public void willThrow(Depletion depletion, Exception exception) {
        if (buildingExpectation == null) throw new StubbingMismatchException();
        try {
            if (exception == null) throw new WunderBarException("can't throw null from an expectation");
            buildingExpectation.willThrow(depletion, exception);
        } finally {
            buildingExpectation = null;
        }
    }

    /// Disables recording for one stub call, e.g. when testing the error handling in your consumer code,
    /// so this interaction is not a specified behavior of the API provider.
    public WunderbarExpectationBuilder<T> withoutRecording() {
        if (buildingExpectation == null) throw new StubbingMismatchException();
        buildingExpectation.setRecording(false);
        return this;
    }

    private static class StubbingMismatchException extends WunderBarException {
        private StubbingMismatchException() {super("Stubbing mismatch: call `given` exactly once on the response object of a proxy call");}
    }


    /// Creates an instance of the service, which normally is done via the [@Service][Service] annotation.
    public static <T> T createService(Class<T> type) {return createService(type, Service.DEFAULT);}

    /// Creates an instance of the service, which normally is done via the [@Service][Service] annotation.
    public static <T> T createService(Class<T> type, Service.Literal service) {
        return createProxy(type, service).getStubbingProxy();
    }

    public static <T> ProxyFactory<T> createProxy(Class<T> type, Literal service) {
        var extension = WunderBarApiConsumerJUnitExtension.INSTANCE;
        if (extension == null) throw new WunderBarException(WunderBarApiConsumer.class.getSimpleName() + " not found");
        return extension.createProxy(type, service);
    }


    /// Return the base uri of the service proxy injected, or `null` if it's a unit test.
    ///
    /// Throws `IllegalArgumentException` if the argument is not a service proxy instance
    public static URI baseUri(Object proxyInstance) {
        var invocationHandler = java.lang.reflect.Proxy.getInvocationHandler(proxyInstance);
        return getProxy(invocationHandler).getExpectations().baseUri();
    }

    @SuppressWarnings("unchecked")
    private static <T> Proxy<T> getProxy(InvocationHandler invocationHandler) {
        try {
            return (Proxy<T>) ((Proxy.ProxyInvocationHandler) invocationHandler).proxy;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("not a service proxy instance", e);
        } catch (RuntimeException e) {
            throw new RuntimeException("can't determine service proxy instance", e);
        }
    }
}

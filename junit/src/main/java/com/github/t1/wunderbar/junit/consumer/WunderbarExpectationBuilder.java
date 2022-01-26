package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.common.Internal;
import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.consumer.Service.Literal;

import java.lang.reflect.InvocationHandler;
import java.net.URI;
import java.util.Objects;
import java.util.function.Consumer;

import static com.github.t1.wunderbar.common.Utils.getField;

/**
 * Static methods for building expectations, etc.
 *
 * @see #given
 * @see #createService(Class)
 * @see #baseUri(Object)
 */
public class WunderbarExpectationBuilder<T> {
    private WunderbarExpectationBuilder() {}

    /**
     * Starts to specify the behavior of the {@link Service} API that the test expects.
     * The parameter is the result of a call to your API interface;
     * call {@link #returns} (a.k.a. {@link #willReturn}) or {@link #willThrow} on the result of this method; e.g.:
     * <pre><code>given(api.findProduct(ID)).returns(PRODUCT);</code></pre>
     */
    public static <T> WunderbarExpectationBuilder<T> given(T dummyValue) {
        if (buildingExpectation == null || !Objects.equals(dummyValue, buildingExpectation.nullValue()))
            throw new StubbingMismatchException();
        return new WunderbarExpectationBuilder<>();
    }

    public @Internal static WunderBarExpectation buildingExpectation;

    /** use {@link #baseUri(Object)} instead */
    @Deprecated(forRemoval = true)
    public WunderbarExpectationBuilder<T> whileSettingBaseUri(Consumer<URI> setter) {
        if (buildingExpectation == null) throw new StubbingMismatchException();
        setter.accept(buildingExpectation.baseUri());
        return this;
    }


    public static Depletion always() {return times(Integer.MAX_VALUE);}

    public static Depletion once() {return times(1);}

    public static Depletion times(int maxValue) {return Depletion.builder().maxCallCount(maxValue).build();}


    /** Specifies that the API returns this object as a response. */
    public void willReturn(T response) {returns(response);}

    /** Specifies that the API returns this object as a response. */
    public void returns(T response) {returns(always(), response);}

    /** Specifies that the API returns this object as a response. */
    public void returns(Depletion depletion, T response) {
        if (buildingExpectation == null) throw new StubbingMismatchException();
        try {
            if (response == null) throw new WunderBarException("can't return null from an expectation");
            buildingExpectation.returns(depletion, response);
        } finally {
            buildingExpectation = null;
        }
    }

    /**
     * Specifies that the API returns this error as a response, e.g.:
     * <pre><code>given(api.findProduct(ID)).willThrow(new NotFoundException("product ID not found"));</code></pre>
     * <p>
     * Note that your {@link SystemUnderTest} may need to handle specific (mainly business) errors, e.g. to distinguish
     * a <code>404 Not Found</code> for technical reasons from when a specific product id is not found.
     * It can do so by checking the <code><b>code</b></code> that is returned for {@link Level#INTEGRATION integration} tests:
     * <ul>
     * <li>If the API is a REST service, the mock service returns the status code of a <code>WebApplicationException</code>
     * (if it is one) and a <a href="https://datatracker.ietf.org/doc/html/rfc7807">RFC-7807</a> style body with:
     *     <ul>
     *     <li>a <code>detail</code> field containing the exception message,
     *     <li>a <code>title</code> field containing the exception class name, and
     *     <li>a <code>type</code> field containing the <code><b>code</b></code> derived from the exception type name.
     *     </ul>
     * <li>If the API is a GraphQL service, the mock service returns an <code>error</code> with:
     *     <ul>
     *     <li>a <code>message</code> field containing the exception message and
     *     <li>a <code><b>code</b></code> extension field containing the code derived from the exception type name.
     *     </ul>
     * </ul>
     * The <code><b>code</b></code> is derived from the simple name of the exception without the <code>Exception</code> suffix,
     * by converting camel case to kebab case, e.g. <code>ProductNotFoundException</code> becomes <code>product-not-found</code>.
     * These are also important requirements for the service to implement.
     */
    public void willThrow(Exception exception) {willThrow(always(), exception);}

    /**
     * Specifies that the API returns this error as a response, e.g.:
     * <pre><code>given(api.findProduct(ID)).willThrow(new NotFoundException("product ID not found"));</code></pre>
     * <p>
     * Note that your {@link SystemUnderTest} may need to handle specific (mainly business) errors, e.g. to distinguish
     * a <code>404 Not Found</code> for technical reasons from when a specific product id is not found.
     * It can do so by checking the <code><b>code</b></code> that is returned for {@link Level#INTEGRATION integration} tests:
     * <ul>
     * <li>If the API is a REST service, the mock service returns the status code of a <code>WebApplicationException</code>
     * (if it is one) and a <a href="https://datatracker.ietf.org/doc/html/rfc7807">RFC-7807</a> style body with:
     *     <ul>
     *     <li>a <code>detail</code> field containing the exception message,
     *     <li>a <code>title</code> field containing the exception class name, and
     *     <li>a <code>type</code> field containing the <code><b>code</b></code> derived from the exception type name.
     *     </ul>
     * <li>If the API is a GraphQL service, the mock service returns an <code>error</code> with:
     *     <ul>
     *     <li>a <code>message</code> field containing the exception message and
     *     <li>a <code><b>code</b></code> extension field containing the code derived from the exception type name.
     *     </ul>
     * </ul>
     * The <code><b>code</b></code> is derived from the simple name of the exception without the <code>Exception</code> suffix,
     * by converting camel case to kebab case, e.g. <code>ProductNotFoundException</code> becomes <code>product-not-found</code>.
     * These are also important requirements for the service to implement.
     */
    public void willThrow(Depletion depletion, Exception exception) {
        if (buildingExpectation == null) throw new StubbingMismatchException();
        try {
            if (exception == null) throw new WunderBarException("can't throw null from an expectation");
            buildingExpectation.willThrow(depletion, exception);
        } finally {
            buildingExpectation = null;
        }
    }

    private static class StubbingMismatchException extends WunderBarException {
        private StubbingMismatchException() {super("Stubbing mismatch: call `given` exactly once on the response object of a proxy call");}
    }


    /**
     * Creates an instance of the service, which normally is done via the {@link Service @Service} annotation.
     */
    public static <T> T createService(Class<T> type) {return createService(type, Service.DEFAULT);}

    /**
     * Creates an instance of the service, which normally is done via the {@link Service @Service} annotation.
     */
    public static <T> T createService(Class<T> type, Service.Literal service) {
        return createProxy(type, service).getStubbingProxy();
    }

    public static <T> ProxyFactory<T> createProxy(Class<T> type, Literal service) {
        var extension = WunderBarApiConsumerJUnitExtension.INSTANCE;
        if (extension == null) throw new WunderBarException(WunderBarApiConsumer.class.getSimpleName() + " not found");
        return extension.createProxy(type, service);
    }


    /**
     * Return the base uri of the service proxy injected, or <code>null</code> if it's a unit test.
     *
     * @throws IllegalArgumentException if the argument is not a service proxy instance
     */
    public static URI baseUri(Object proxyInstance) {
        var invocationHandler = java.lang.reflect.Proxy.getInvocationHandler(proxyInstance);
        return getProxy(invocationHandler).getExpectations().baseUri();
    }

    // this is an ugly hack, but I currently don't have a better idea
    private static <T> Proxy<T> getProxy(InvocationHandler invocationHandler) {
        try {
            return getField(invocationHandler, "arg$1");
        } catch (Exception e) { // work around SneakyThrows
            //noinspection ConstantConditions
            if (e instanceof NoSuchFieldException || e instanceof ClassCastException)
                throw new IllegalArgumentException("not a service proxy instance", e);
            throw new RuntimeException("can't determine service proxy instance", e);
        }
    }
}

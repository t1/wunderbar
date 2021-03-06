package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.junit.WunderBarException;

import java.util.Objects;

/** @see #given */
public class WunderbarExpectationBuilder<T> {
    /**
     * Starts to specify the behavior of the {@link Service} API that the test expects.
     * The parameter is the result of a call to your API interface;
     * call {@link #willReturn} or {@link #willThrow} on the result of this method; e.g.:
     * <pre><code>given(api.findProduct(ID)).willReturn(PRODUCT);</code></pre>
     */
    public static <T> WunderbarExpectationBuilder<T> given(T dummyValue) {
        if (buildingExpectation == null || !Objects.equals(dummyValue, buildingExpectation.nullValue()))
            throw new WunderbarExpectationBuilderException();
        return new WunderbarExpectationBuilder<>();
    }

    public @Internal static WunderBarExpectation buildingExpectation;

    /** Specifies that the API returns this object as a response. */
    public void willReturn(T response) {
        if (buildingExpectation == null) throw new WunderbarExpectationBuilderException();
        try {
            if (response == null) throw new WunderBarException("can't return null from an expectation");
            buildingExpectation.willReturn(response);
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
     * (if it is one) and a <a href="https://tools.ietf.org/html/rfc7807">rfc-7807</a> style body with:
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
    public void willThrow(Exception exception) {
        if (buildingExpectation == null) throw new WunderbarExpectationBuilderException();
        try {
            if (exception == null) throw new WunderBarException("can't throw null from an expectation");
            buildingExpectation.willThrow(exception);
        } finally {
            buildingExpectation = null;
        }
    }

    static class WunderbarExpectationBuilderException extends WunderBarException {
        WunderbarExpectationBuilderException() { super("Stubbing mismatch: call `given` exactly once on the response object of a proxy call"); }
    }
}

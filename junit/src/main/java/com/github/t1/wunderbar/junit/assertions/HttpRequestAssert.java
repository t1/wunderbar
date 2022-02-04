package com.github.t1.wunderbar.junit.assertions;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import org.assertj.core.api.AbstractAssert;

import static com.github.t1.wunderbar.junit.assertions.WunderBarBDDAssertions.then;

/**
 * AssertJ assertion on {@link HttpRequest}
 */
@SuppressWarnings("UnusedReturnValue")
public class HttpRequestAssert<SELF extends HttpRequestAssert<SELF, ACTUAL>, ACTUAL extends HttpRequest>
    extends AbstractAssert<SELF, ACTUAL> {
    public HttpRequestAssert(ACTUAL actual) {this(actual, HttpRequestAssert.class);}

    protected HttpRequestAssert(ACTUAL actual, Class<?> selfType) {super(actual, selfType);}

    public HttpRequestAssert<SELF, ACTUAL> isGET() {return hasMethod("GET");}

    public HttpRequestAssert<SELF, ACTUAL> isPOST() {return hasMethod("POST");}

    public HttpRequestAssert<SELF, ACTUAL> hasMethod(String expected) {
        then(actual.getMethod()).as("http request method").isEqualTo(expected);
        return this;
    }

    public HttpRequestAssert<SELF, ACTUAL> hasUriEndingWith(String expected) {
        then(actual.uri()).as("http request uri").endsWith(expected);
        return this;
    }

    public HttpRequestAssert<SELF, ACTUAL> isGraphQL() {return isPOST().hasUriEndingWith("/graphql");}

    public HttpRequestAssert<SELF, ACTUAL> hasQuery(String expected) {
        isGraphQL();
        then(actual.get("/query")).as("GraphQL query").isJsonString().isEqualTo(expected);
        return this;
    }

    public HttpRequestAssert<SELF, ACTUAL> hasVariable(String name, String value) {
        isGraphQL();
        then(actual.get("/variables/" + name)).as("GraphQL variable '%s'", name).isJsonString().isEqualTo(value);
        return this;
    }

    public HttpRequestAssert<SELF, ACTUAL> hasVariable(String name) {
        isGraphQL();
        then(actual.get("/variables/" + name)).as("GraphQL variable '%s'", name).isNotNull();
        return this;
    }
}

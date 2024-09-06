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

    public SELF isGET() {return hasMethod("GET");}

    public SELF isPOST() {return hasMethod("POST");}

    public SELF hasMethod(String expected) {
        then(actual.getMethod()).as("http request method").isEqualTo(expected);
        return myself;
    }

    public SELF hasUriEndingWith(String expected) {
        then(actual.uri()).as("http request uri").endsWith(expected);
        return myself;
    }

    public SELF isGraphQL() {return isPOST().hasUriEndingWith("/graphql");}

    public SELF hasQuery(String expected) {
        isGraphQL();
        then(actual.get("/query")).as("GraphQL query").isJsonString().isEqualTo(expected);
        return myself;
    }

    public SELF hasVariable(String name, String value) {
        isGraphQL();
        then(actual.get("/variables/" + name)).as("GraphQL variable '%s'", name).isJsonString().isEqualTo(value);
        return myself;
    }

    public SELF hasVariable(String name) {
        isGraphQL();
        then(actual.get("/variables/" + name)).as("GraphQL variable '%s'", name).isNotNull();
        return myself;
    }
}

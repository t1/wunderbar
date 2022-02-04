package com.github.t1.wunderbar.junit.assertions;

import com.github.t1.wunderbar.junit.assertions.JsonValueAssert.JsonObjectAssert;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import org.assertj.core.api.AbstractAssert;

import static com.github.t1.wunderbar.junit.assertions.WunderBarBDDAssertions.then;

/**
 * AssertJ assertion on {@link HttpRequest}
 */
@SuppressWarnings("UnusedReturnValue")
public class HttpResponseAssert<SELF extends HttpResponseAssert<SELF, ACTUAL>, ACTUAL extends HttpResponse>
    extends AbstractAssert<SELF, ACTUAL> {
    public HttpResponseAssert(ACTUAL actual) {this(actual, HttpResponseAssert.class);}

    protected HttpResponseAssert(ACTUAL actual, Class<?> selfType) {super(actual, selfType);}

    public JsonObjectAssert<?, ?> isJsonObject() {
        return then(actual.jsonValue()).isObject();
    }

    public HttpResponseAssert<SELF, ACTUAL> hasJson(String name, String expected) {
        isJsonObject().hasString(name, expected);
        return this;
    }
}

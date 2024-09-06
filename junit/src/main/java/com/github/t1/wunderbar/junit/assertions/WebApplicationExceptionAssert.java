package com.github.t1.wunderbar.junit.assertions;

import jakarta.json.JsonObject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.StatusType;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.InstanceOfAssertFactory;

import static com.github.t1.wunderbar.junit.assertions.WunderBarBDDAssertions.then;
import static com.github.t1.wunderbar.junit.http.HttpUtils.readJson;

/**
 * Assertions about a {@link WebApplicationException} that contains a
 * <a href="https://datatracker.ietf.org/doc/html/rfc7807">RFC-7807 Problems Detail</a> body.
 */
@SuppressWarnings("UnusedReturnValue")
public class WebApplicationExceptionAssert<SELF extends WebApplicationExceptionAssert<SELF, ACTUAL>, ACTUAL extends WebApplicationException>
        extends AbstractAssert<SELF, ACTUAL> {
    public static final InstanceOfAssertFactory<WebApplicationException, WebApplicationExceptionAssert<?, ?>> WEB_APPLICATION_EXCEPTION
            = new InstanceOfAssertFactory<>(WebApplicationException.class, WebApplicationExceptionAssert::new);

    protected WebApplicationExceptionAssert(ACTUAL actual) {this(actual, WebApplicationExceptionAssert.class);}

    protected WebApplicationExceptionAssert(ACTUAL actual, Class<?> selfType) {super(actual, selfType);}

    private JsonObject body;

    public SELF hasStatus(StatusType expected) {
        then(actual.getResponse().getStatusInfo()).isEqualTo(expected);
        return myself;
    }

    public SELF hasType(String expected) {
        then(body()).hasString("type", expected);
        return myself;
    }

    public SELF hasTitle(String expected) {
        then(body()).hasString("title", expected);
        return myself;
    }

    public SELF hasDetail(String expected) {
        then(body()).hasString("detail", expected);
        return myself;
    }

    private JsonObject body() {
        if (body == null) {
            actual.getResponse().bufferEntity();
            var jsonString = actual.getResponse().readEntity(String.class);
            var jsonValue = readJson(jsonString);
            then(jsonValue).isObject();
            body = jsonValue.asJsonObject();
        }
        return body;
    }
}

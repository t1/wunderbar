package com.github.t1.wunderbar.junit.assertions;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.InstanceOfAssertFactory;

import javax.json.JsonObject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.StatusType;

import static com.github.t1.wunderbar.junit.http.HttpUtils.readJson;
import static com.github.t1.wunderbar.junit.provider.WunderBarBDDAssertions.then;

/**
 * Assertions about a {@link WebApplicationException} that contains a
 * <a href="https://datatracker.ietf.org/doc/html/rfc7807">RFC-7807 Problems Detail</a> body.
 */
@SuppressWarnings("UnusedReturnValue")
public class ProblemDetailsAssert<SELF extends ProblemDetailsAssert<SELF, ACTUAL>, ACTUAL extends WebApplicationException>
    extends AbstractAssert<SELF, ACTUAL> {
    public static final InstanceOfAssertFactory<WebApplicationException, ProblemDetailsAssert<?, ?>> PROBLEM_DETAILS
        = new InstanceOfAssertFactory<>(WebApplicationException.class, ProblemDetailsAssert::new);

    protected ProblemDetailsAssert(ACTUAL actual) {this(actual, ProblemDetailsAssert.class);}

    protected ProblemDetailsAssert(ACTUAL actual, Class<?> selfType) {super(actual, selfType);}

    private JsonObject body;

    public SELF hasStatus(StatusType expected) {
        then(actual.getResponse().getStatusInfo()).isEqualTo(expected);
        return myself;
    }

    public SELF hasType(String expected) {
        then(body()).hasString("type", expected);
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

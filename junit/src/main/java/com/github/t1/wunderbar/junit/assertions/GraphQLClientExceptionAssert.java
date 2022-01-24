package com.github.t1.wunderbar.junit.assertions;

import io.smallrye.graphql.client.GraphQLClientException;
import io.smallrye.graphql.client.GraphQLError;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.InstanceOfAssertFactory;

import java.util.stream.Stream;

public class GraphQLClientExceptionAssert<SELF extends GraphQLClientExceptionAssert<SELF, ACTUAL>, ACTUAL extends GraphQLClientException>
    extends AbstractThrowableAssert<SELF, ACTUAL> {
    public static final InstanceOfAssertFactory<GraphQLClientException, GraphQLClientExceptionAssert<?, ?>> GRAPHQL_CLIENT_EXCEPTION
        = new InstanceOfAssertFactory<>(GraphQLClientException.class, GraphQLClientExceptionAssert::new);

    public GraphQLClientExceptionAssert(ACTUAL actual) {this(actual, GraphQLClientExceptionAssert.class);}

    public GraphQLClientExceptionAssert(ACTUAL actual, Class<? super GraphQLClientExceptionAssert<?, ?>> selfType) {super(actual, selfType);}

    public GraphQLErrorAssert<?, ?> hasErrorCode(String errorCode) {
        GraphQLError error = errors()
            .filter(e -> errorCode.equals(e.getCode()))
            .findAny().orElseThrow(() -> errorNotFound(errorCode));
        return new GraphQLErrorAssert<>(error);
    }

    private AssertionError errorNotFound(String errorCode) {
        return new AssertionError("no error with code " + errorCode + " found in " + actual);
    }

    private Stream<GraphQLError> errors() {
        if (actual == null || actual.getErrors() == null || actual.getErrors().isEmpty())
            throw new AssertionError("no errors found");
        return actual.getErrors().stream();
    }
}

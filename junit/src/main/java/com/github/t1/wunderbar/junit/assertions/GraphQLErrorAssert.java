package com.github.t1.wunderbar.junit.assertions;

import io.smallrye.graphql.client.GraphQLError;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.InstanceOfAssertFactory;

import static org.assertj.core.api.BDDAssertions.then;

public class GraphQLErrorAssert<SELF extends GraphQLErrorAssert<SELF, ACTUAL>, ACTUAL extends GraphQLError>
    extends AbstractAssert<SELF, ACTUAL> {
    public static final InstanceOfAssertFactory<GraphQLError, GraphQLErrorAssert<?, ?>> GRAPHQL_ERROR
        = new InstanceOfAssertFactory<>(GraphQLError.class, GraphQLErrorAssert::new);

    protected GraphQLErrorAssert(ACTUAL actual) {this(actual, GraphQLErrorAssert.class);}

    protected GraphQLErrorAssert(ACTUAL actual, Class<?> selfType) {super(actual, selfType);}

    public void withMessage(String expected) {
        then(actual.getMessage()).isEqualTo(expected);
    }
}

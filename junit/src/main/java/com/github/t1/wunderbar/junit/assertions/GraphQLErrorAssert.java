package com.github.t1.wunderbar.junit.assertions;

import io.smallrye.graphql.client.GraphQLError;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.InstanceOfAssertFactory;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

public class GraphQLErrorAssert<SELF extends GraphQLErrorAssert<SELF, ACTUAL>, ACTUAL extends GraphQLError>
        extends AbstractAssert<SELF, ACTUAL> {
    public static final InstanceOfAssertFactory<GraphQLError, GraphQLErrorAssert<?, ?>> GRAPHQL_ERROR
            = new InstanceOfAssertFactory<>(GraphQLError.class, GraphQLErrorAssert::new);

    protected GraphQLErrorAssert(ACTUAL actual) {this(actual, GraphQLErrorAssert.class);}

    protected GraphQLErrorAssert(ACTUAL actual, Class<?> selfType) {super(actual, selfType);}

    public AbstractStringAssert<?> withMessageThat() {return then(actual.getMessage()).asInstanceOf(STRING);}

    public GraphQLErrorAssert<SELF, ACTUAL> withMessage(String expected) {
        withMessageThat().isEqualTo(expected);
        return this;
    }

    public GraphQLErrorAssert<SELF, ACTUAL> withMessageContaining(String expected) {
        withMessageThat().contains(expected);
        return this;
    }
}

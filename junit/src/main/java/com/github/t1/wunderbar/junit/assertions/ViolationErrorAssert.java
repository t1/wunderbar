package com.github.t1.wunderbar.junit.assertions;

import io.smallrye.graphql.client.GraphQLError;

import static com.github.t1.wunderbar.junit.assertions.WunderBarBDDAssertions.then;

public class ViolationErrorAssert<SELF extends ViolationErrorAssert<SELF, ACTUAL>, ACTUAL extends GraphQLError>
    extends GraphQLErrorAssert<SELF, ACTUAL> {
    public ViolationErrorAssert(ACTUAL actual) {super(actual);}

    public ViolationErrorAssert(ACTUAL actual, Class<? super GraphQLClientExceptionAssert<?, ?>> selfType) {super(actual, selfType);}

    public SELF withMessage(String expected) {
        withMessageThat().isEqualTo(expected);
        return myself;
    }

    public SELF withPath(Object... expected) {
        then(actual.getPath()).containsExactly(expected);
        return myself;
    }

    public SELF withViolationMessage(String expected) {
        then(actual.getStringExtension("violation.message")).isEqualTo(expected);
        return myself;
    }

    // other extensions to check:
    // violation.propertyPath=["orderw","id"]
    // violation.invalidValue=123456789
    // violation.constraint={
    //   "message":"{javax.validation.constraints.Size.message}"
    //   "min":1
    //   "max":3
    //   "groups":[]
    //   "payload":[]
    // }
}

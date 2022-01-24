package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.junit.assertions.GraphQLClientExceptionAssert;
import com.github.t1.wunderbar.junit.assertions.JsonValueAssert;
import com.github.t1.wunderbar.junit.assertions.JsonValueAssert.JsonObjectAssert;
import io.smallrye.graphql.client.GraphQLClientException;
import org.assertj.core.api.BDDAssertions;

import javax.json.JsonObject;
import javax.json.JsonValue;

/**
 * An extension for {@link BDDAssertions BDD assertj}
 *
 * @see JsonValueAssert
 * @see JsonObjectAssert
 * @see GraphQLClientExceptionAssert
 */
public class WunderBarBDDAssertions extends BDDAssertions {
    public static JsonValueAssert<?, ?> then(JsonValue jsonValue) {return new JsonValueAssert<>(jsonValue);}

    public static JsonObjectAssert<?, ?> then(JsonObject jsonValue) {return new JsonObjectAssert<>(jsonValue);}

    public static GraphQLClientExceptionAssert<?, ?> then(GraphQLClientException exception) {
        return new GraphQLClientExceptionAssert<>(exception);
    }
}

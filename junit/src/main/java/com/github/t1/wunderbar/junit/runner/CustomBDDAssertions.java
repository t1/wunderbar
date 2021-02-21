package com.github.t1.wunderbar.junit.runner;

import org.assertj.core.api.BDDAssertions;

import javax.json.JsonValue;

/**
 * An extension for {@link BDDAssertions BDD assertj} that allows to validate {@link JsonValue}s.
 *
 * @see JsonValueAssert
 */
public class CustomBDDAssertions extends BDDAssertions {
    public static JsonValueAssert then(JsonValue jsonValue) { return new JsonValueAssert(jsonValue); }
}

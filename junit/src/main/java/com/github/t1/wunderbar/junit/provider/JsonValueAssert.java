package com.github.t1.wunderbar.junit.provider;

import jakarta.json.JsonValue;

/**
 * This remains here only for backward compatibility; use {@link com.github.t1.wunderbar.junit.assertions.JsonValueAssert} instead.
 * <p>
 * TODO 3.0: breaking change: remove
 */
@Deprecated(forRemoval = true)
public class JsonValueAssert extends com.github.t1.wunderbar.junit.assertions.JsonValueAssert<JsonValueAssert, JsonValue> {
    public JsonValueAssert(JsonValue jsonValue) {super(jsonValue);}
}

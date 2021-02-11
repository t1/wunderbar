package com.github.t1.wunderbar.junit.runner;

import org.assertj.core.api.BDDAssertions;

import javax.json.JsonValue;

public class CustomBDDAssertions extends BDDAssertions {
    public static JsonValueAssert then(JsonValue body) { return new JsonValueAssert(body); }
}

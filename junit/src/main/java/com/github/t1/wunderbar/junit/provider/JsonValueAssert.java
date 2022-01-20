package com.github.t1.wunderbar.junit.provider;

import lombok.RequiredArgsConstructor;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonValue;

import static com.github.t1.wunderbar.common.Utils.jsonNonAddDiff;
import static com.github.t1.wunderbar.junit.provider.CustomBDDAssertions.then;
import static java.util.stream.Collectors.toList;

/**
 * AssertJ assertion on JSON values
 */
@RequiredArgsConstructor
public class JsonValueAssert {
    private final JsonValue actual;

    public void isEqualToIgnoringNewFields(JsonValue expected) {
        then(actual.getValueType())
            .describedAs("value type mismatch\n" +
                         "expected: " + expected + "\n" +
                         "actual  : " + actual)
            .isEqualTo(expected.getValueType());
        var nonAddDescriptions = jsonNonAddDiff(expected, actual)
            .map(patch -> describe(patch.asJsonObject(), expected.asJsonObject()))
            .collect(toList());
        then(nonAddDescriptions)
            .describedAs("json diff (ignoring `add` operations)")
            .isEmpty();
    }

    private String describe(JsonObject patch, JsonStructure expectedRoot) {
        var path = patch.getString("path");
        var pointer = Json.createPointer(path);
        return patch.getString("op") + " " + path + ":\n" +
               "  expected: " + pointer.getValue(expectedRoot) + "\n" +
               "    actual: " + patch.get("value");
    }
}

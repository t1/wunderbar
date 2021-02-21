package com.github.t1.wunderbar.junit.runner;

import lombok.RequiredArgsConstructor;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonValue;

import static com.github.t1.wunderbar.junit.runner.CustomBDDAssertions.then;
import static java.util.stream.Collectors.toList;
import static javax.json.JsonPatch.Operation.ADD;

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
        // TODO diff json array and scalar
        var diff = Json.createDiff(expected.asJsonObject(), actual.asJsonObject()).toJsonArray();
        var nonAdd = diff.stream()
            .filter(operation -> !isAdd(operation))
            .collect(toList());
        then(nonAdd.stream().map(patch -> describe(patch.asJsonObject(), expected.asJsonObject())))
            .describedAs("json diff (ignoring `add` operations)")
            .containsExactly();
    }

    private String describe(JsonObject patch, JsonStructure expectedRoot) {
        var path = patch.getString("path");
        var pointer = Json.createPointer(path);
        return patch.getString("op") + " " + path + ":\n" +
            "  expected: " + pointer.getValue(expectedRoot) + "\n" +
            "    actual: " + patch.get("value");
    }

    private boolean isAdd(JsonValue jsonValue) {
        return jsonValue.asJsonObject().getString("op").equals(ADD.operationName());
    }
}

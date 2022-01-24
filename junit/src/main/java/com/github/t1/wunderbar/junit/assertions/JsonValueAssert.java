package com.github.t1.wunderbar.junit.assertions;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.InstanceOfAssertFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import static com.github.t1.wunderbar.common.Utils.jsonNonAddDiff;
import static com.github.t1.wunderbar.junit.assertions.JsonValueAssert.JsonObjectAssert.JSON_OBJECT;
import static com.github.t1.wunderbar.junit.provider.WunderBarBDDAssertions.then;
import static java.util.stream.Collectors.toList;
import static javax.json.JsonValue.ValueType.OBJECT;
import static javax.json.JsonValue.ValueType.STRING;

/**
 * AssertJ assertion on JSON values
 */
@SuppressWarnings("UnusedReturnValue")
public class JsonValueAssert<SELF extends JsonValueAssert<SELF, ACTUAL>, ACTUAL extends JsonValue>
    extends AbstractAssert<SELF, ACTUAL> {
    public JsonValueAssert(ACTUAL actual) {this(actual, JsonValueAssert.class);}

    protected JsonValueAssert(ACTUAL actual, Class<?> selfType) {super(actual, selfType);}

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

    public JsonObjectAssert<?, ?> asObject() {return isObject().asInstanceOf(JSON_OBJECT);}

    public JsonValueAssert<SELF, ACTUAL> isObject() {return isType(OBJECT);}

    public JsonValueAssert<SELF, ACTUAL> isString() {return isType(STRING);}

    public JsonValueAssert<SELF, ACTUAL> isType(ValueType expected) {
        then(actual.getValueType()).isEqualTo(expected);
        return myself;
    }

    public static class JsonObjectAssert<SELF extends JsonObjectAssert<SELF, ACTUAL>, ACTUAL extends JsonObject>
        extends AbstractAssert<SELF, ACTUAL> {
        public static final InstanceOfAssertFactory<JsonObject, JsonObjectAssert<?, ?>> JSON_OBJECT
            = new InstanceOfAssertFactory<>(JsonObject.class, JsonObjectAssert::new);

        public JsonObjectAssert(ACTUAL actual) {this(actual, JsonObjectAssert.class);}

        protected JsonObjectAssert(ACTUAL actual, Class<?> selfType) {super(actual, selfType);}

        public JsonObjectAssert<SELF, ACTUAL> hasString(String name, String expected) {
            hasField(name, STRING);
            then(actual.getString(name)).isEqualTo(expected);
            return this;
        }

        public JsonObjectAssert<SELF, ACTUAL> hasField(String name) {
            then(actual.containsKey(name)).describedAs("there's a field %s in %s", name, actual.keySet()).isTrue();
            return this;
        }

        public JsonObjectAssert<SELF, ACTUAL> hasField(String name, ValueType type) {
            hasField(name);
            then(actual.getValue("/" + name)).isType(type);
            return this;
        }
    }
}

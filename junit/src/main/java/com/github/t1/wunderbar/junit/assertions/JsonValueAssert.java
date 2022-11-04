package com.github.t1.wunderbar.junit.assertions;

import com.github.t1.wunderbar.junit.http.HttpUtils;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.InstanceOfAssertFactory;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonPointer;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;

import static com.github.t1.wunderbar.common.Utils.nonAddFieldDiff;
import static com.github.t1.wunderbar.junit.assertions.JsonValueAssert.JsonObjectAssert.JSON_OBJECT;
import static com.github.t1.wunderbar.junit.assertions.WunderBarBDDAssertions.then;
import static java.util.stream.Collectors.toList;
import static jakarta.json.JsonValue.ValueType.OBJECT;
import static jakarta.json.JsonValue.ValueType.STRING;

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
        var nonAddFieldDescriptions = nonAddFieldDiff(expected, actual)
            .map(patch -> describe(patch.asJsonObject(), expected.asJsonObject()))
            .collect(toList());
        then(nonAddFieldDescriptions)
            .describedAs("json diff (ignoring new fields)")
            .isEmpty();
    }

    private String describe(JsonObject patch, JsonStructure expectedRoot) {
        var path = patch.getString("path");
        var pointer = Json.createPointer(path);
        return patch.getString("op") + " " + path + ":\n" +
               "  expected: " + pointer.getValue(expectedRoot) + "\n" +
               "    actual: " + patch.get("value");
    }

    public JsonObjectAssert<?, ?> isObject() {return isType(OBJECT).asInstanceOf(JSON_OBJECT);}

    public AbstractStringAssert<?> isJsonString() {
        return isType(STRING).extracting(HttpUtils::jsonString, InstanceOfAssertFactories.STRING);
    }

    public SELF isType(ValueType expected) {
        then(actual.getValueType()).isEqualTo(expected);
        return myself;
    }

    public static class JsonObjectAssert<SELF extends JsonObjectAssert<SELF, ACTUAL>, ACTUAL extends JsonObject>
        extends AbstractAssert<SELF, ACTUAL> {
        public static final InstanceOfAssertFactory<JsonObject, JsonObjectAssert<?, ?>> JSON_OBJECT
            = new InstanceOfAssertFactory<>(JsonObject.class, JsonObjectAssert::new);

        public JsonObjectAssert(ACTUAL actual) {this(actual, JsonObjectAssert.class);}

        protected JsonObjectAssert(ACTUAL actual, Class<?> selfType) {super(actual, selfType);}

        public SELF has(String pointer, String expected) {
            then(actual).at(pointer).isJsonString().isEqualTo(expected);
            return myself;
        }

        public JsonValueAssert<?, ?> at(String pointer) {return at(Json.createPointer(pointer));}

        public JsonValueAssert<?, ?> at(JsonPointer pointer) {return then(pointer.getValue(actual));}

        public SELF hasString(String name, String expected) {
            hasField(name, STRING);
            then(actual.getString(name)).isEqualTo(expected);
            return myself;
        }

        public SELF hasField(String name) {
            then(actual.containsKey(name)).describedAs("expected a field '%s' but has only %s", name, actual.keySet()).isTrue();
            return myself;
        }

        public SELF hasField(String name, ValueType type) {
            hasField(name);
            then(actual.getValue("/" + name)).isType(type);
            return myself;
        }
    }
}

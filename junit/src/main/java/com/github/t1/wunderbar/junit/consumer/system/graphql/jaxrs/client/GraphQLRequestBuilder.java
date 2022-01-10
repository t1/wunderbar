package com.github.t1.wunderbar.junit.consumer.system.graphql.jaxrs.client;

import io.smallrye.graphql.client.impl.typesafe.QueryBuilder;
import io.smallrye.graphql.client.impl.typesafe.reflection.FieldInfo;
import io.smallrye.graphql.client.impl.typesafe.reflection.MethodInvocation;
import io.smallrye.graphql.client.impl.typesafe.reflection.TypeInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class GraphQLRequestBuilder {
    private static final Map<String, String> QUERY_CACHE = new HashMap<>();

    private final MethodInvocation method;

    public String build() {
        JsonObjectBuilder request = Json.createObjectBuilder();
        String query = query();
        log.debug("request graphql: {}", query);
        request.add("query", query);
        request.add("variables", variables());
        request.add("operationName", method.getName());
        String requestString = request.build().toString();
        log.debug("full graphql request: {}", requestString);
        return requestString;
    }

    public String query() {
        return QUERY_CACHE.computeIfAbsent(method.getKey(), key -> new QueryBuilder(method).build());
    }

    public JsonObject variables() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        method.valueParameters().forEach(parameter -> builder.add(parameter.getRawName(), value(parameter.getValue())));
        return builder.build();
    }

    private JsonValue value(Object value) {
        if (value == null)
            return JsonValue.NULL;
        if (value instanceof JsonValue)
            return (JsonValue) value;
        TypeInfo type = TypeInfo.of(value.getClass());
        if (type.isScalar())
            return scalarValue(value);
        if (type.isCollection())
            return arrayValue(value);
        return objectValue(value, type.fields());
    }

    private JsonValue scalarValue(Object value) {
        if (value instanceof String)
            return Json.createValue((String) value);
        if (value instanceof Date)
            return Json.createValue(((Date) value).toInstant().toString());
        if (value instanceof Enum)
            return Json.createValue(((Enum<?>) value).name());
        if (value instanceof Boolean)
            return ((Boolean) value) ? JsonValue.TRUE : JsonValue.FALSE;
        if (value instanceof Byte)
            return Json.createValue((Byte) value);
        if (value instanceof Short)
            return Json.createValue((Short) value);
        if (value instanceof Integer)
            return Json.createValue((Integer) value);
        if (value instanceof Long)
            return Json.createValue((Long) value);
        if (value instanceof Double)
            return Json.createValue((Double) value);
        if (value instanceof Float)
            return Json.createValue((Float) value);
        if (value instanceof BigInteger)
            return Json.createValue((BigInteger) value);
        if (value instanceof BigDecimal)
            return Json.createValue((BigDecimal) value);
        return Json.createValue(value.toString());
    }

    private JsonArray arrayValue(Object value) {
        JsonArrayBuilder array = Json.createArrayBuilder();
        values(value).forEach(item -> array.add(value(item)));
        return array.build();
    }

    private Collection<?> values(Object value) {
        return value.getClass().isArray() ? Arrays.asList((Object[]) value) : (Collection<?>) value;
    }

    private JsonObject objectValue(Object object, Stream<FieldInfo> fields) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        fields.forEach(field -> builder.add(field.getName(), value(field.get(object))));
        return builder.build();
    }
}

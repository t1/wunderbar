package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.common.Internal;
import lombok.NonNull;
import lombok.Value;

import javax.json.JsonValue;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbTypeSerializer;
import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Optional;

import static com.github.t1.wunderbar.junit.http.HttpUtils.readJson;

public @Internal @Value class GeneratedDataPoint {
    static Optional<GeneratedDataPoint> find(List<GeneratedDataPoint> list, Object value) {
        var json = readJson(value);
        return list.stream().filter(item -> item.value.equals(json)).findFirst();
    }

    GeneratedDataPoint(Some some, AnnotatedElement location, @NonNull Object value, @NonNull SomeData generator) {
        this.some = some;
        this.location = location;
        this.value = readJson(value);
        this.rawValue = value;
        this.generator = generator;
    }

    @JsonbTypeSerializer(JsonbSomeSerializer.class)
    Some some;

    @JsonbTypeSerializer(JsonbAnnotatedElementSerializer.class)
    AnnotatedElement location;

    JsonValue value;

    @JsonbTransient Object rawValue;

    @JsonbTransient SomeData generator;

    public String getType() {return rawValue.getClass().getName();}

    @Override public String toString() {
        return value + " -> " + some + ((location == null) ? "" : ":" + location) + " [" + generator + "]";
    }
}

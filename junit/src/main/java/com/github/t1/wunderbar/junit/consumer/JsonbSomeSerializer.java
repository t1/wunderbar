package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.common.Internal;

import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;
import java.util.stream.Stream;

public @Internal class JsonbSomeSerializer implements JsonbSerializer<Some> {
    @Override public void serialize(Some some, JsonGenerator generator, SerializationContext ctx) {
        generator.writeStartObject();
        generator.writeStartArray("tags");
        Stream.of(some.value()).forEach(generator::write);
        generator.writeEnd();
        generator.writeEnd();
    }
}

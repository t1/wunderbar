package com.github.t1.wunderbar.junit.consumer;

import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

import static com.github.t1.wunderbar.common.Utils.name;
import static java.util.Locale.ROOT;

public class JsonbAnnotatedElementSerializer implements JsonbSerializer<AnnotatedElement> {
    @Override public void serialize(AnnotatedElement annotatedElement, JsonGenerator generator, SerializationContext ctx) {
        if (annotatedElement == null) generator.writeNull();
        else generator.write(annotatedElement.getClass().getSimpleName().toLowerCase(ROOT) + " "
                             + "[" + name(annotatedElement) + "] "
                             + "@" + container(annotatedElement));
    }

    private static String container(AnnotatedElement location) {
        if (location instanceof Field) return "class " + ((Field) location).getDeclaringClass().getName();
        if (location instanceof Parameter) {
            var executable = ((Parameter) location).getDeclaringExecutable();
            return "method " + executable.getDeclaringClass().getName() + "#" + executable.getName();
        }
        return "";
    }
}

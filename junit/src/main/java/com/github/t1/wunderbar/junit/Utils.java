package com.github.t1.wunderbar.junit;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

@UtilityClass
public class Utils {
    @SneakyThrows(ReflectiveOperationException.class)
    public static Object invoke(Object instance, Method method, Object... args) {
        method.setAccessible(true);
        try {
            return method.invoke(instance, args);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof RuntimeException)
                throw (RuntimeException) e.getTargetException();
            throw e;
        }
    }

    public static String formatJson(String json) {
        var value = Json.createReader(new StringReader(json)).readValue();

        var writer = new StringWriter();
        Json.createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, true))
            .createWriter(writer)
            .write(value);
        return writer.toString().trim() + "\n";
    }
}

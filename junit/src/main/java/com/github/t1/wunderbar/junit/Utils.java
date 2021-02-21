package com.github.t1.wunderbar.junit;

import com.github.t1.wunderbar.junit.consumer.Internal;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import static java.util.Locale.US;
import static java.util.stream.Collectors.toList;

@UtilityClass
public @Internal class Utils {
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
        if (json == null || json.isBlank()) return json;

        var value = Json.createReader(new StringReader(json)).readValue();

        var writer = new StringWriter();
        Json.createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, true))
            .createWriter(writer)
            .write(value);
        return writer.toString().trim() + "\n";
    }

    @SneakyThrows(IOException.class)
    public static void deleteRecursive(Path path) {
        if (Files.exists(path)) {
            var list = Files.walk(path).collect(toList());
            Collections.reverse(list);
            list.forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new RuntimeException("can't delete " + p, e);
                }
            });
        }
    }

    public static String errorCode(Exception exception) {
        var code = camelToKebab(exception.getClass().getSimpleName());
        if (code.endsWith("-exception")) code = code.substring(0, code.length() - 10);
        return code;
    }

    private static String camelToKebab(String in) { return String.join("-", in.split("(?=\\p{javaUpperCase})")).toLowerCase(US); }
}

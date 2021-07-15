package com.github.t1.wunderbar.junit;

import com.github.t1.wunderbar.junit.consumer.Internal;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
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
            if (e.getTargetException() instanceof AssertionError)
                throw (AssertionError) e.getTargetException();
            throw e;
        }
    }

    public static String formatJson(String json) {
        if (json == null || json.isBlank()) return json;

        var value = Json.createReader(new StringReader(json)).readValue();

        return formatJson(value);
    }

    public static String formatJson(JsonValue value) {
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

    @SneakyThrows(ReflectiveOperationException.class)
    public static Object getField(Object instance, Field field) {
        field.setAccessible(true);
        return field.get(instance);
    }

    @SneakyThrows(ReflectiveOperationException.class)
    public static void setField(Object instance, Field field, Object value) {
        field.setAccessible(true);
        field.set(instance, value);
    }

    public static String base64(String string) {
        return Base64.getEncoder().encodeToString(string.getBytes(UTF_8));
    }

    public static String base64decode(String string) {
        return new String(Base64.getDecoder().decode(string.getBytes(UTF_8)), UTF_8);
    }

    /**
     * Like {@link MediaType#isCompatible(MediaType)}, but taking suffixes like <b><code>+json</code></b> into account.
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc6838#section-4.2.8">rfc-6838</a>
     */
    public static boolean isCompatible(MediaType left, MediaType right) {
        if (left == null) return right == null;
        if (right == null) return false;
        if (left.isWildcardType() || right.isWildcardType()) return true;
        if (!left.getType().equalsIgnoreCase(right.getType())) return false;
        if (left.isWildcardSubtype() || right.isWildcardSubtype()) return true;
        return getSubtypeOrSuffix(left).equalsIgnoreCase(getSubtypeOrSuffix(right));
    }

    private static String getSubtypeOrSuffix(MediaType mediaType) {
        var subtype = mediaType.getSubtype();
        return subtype.contains("+") ? subtype.substring(subtype.indexOf('+') + 1) : subtype;
    }
}

package com.github.t1.wunderbar.common;

import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import javax.json.Json;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParsingException;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.US;
import static java.util.stream.Collectors.toList;
import static javax.json.JsonPatch.Operation.ADD;

@UtilityClass
public @Internal class Utils {
    public static final Jsonb JSON = JsonbBuilder.create();
    public static final MediaType PROBLEM_DETAILS_TYPE = MediaType.valueOf("application/problem+json;charset=utf-8");

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

        var value = readJson(json);

        return formatJson(value);
    }

    public static <T> T fromJson(JsonValue jsonValue, Class<T> type) {
        return (jsonValue == null) ? null : JSON.fromJson(jsonValue.toString(), type);
    }

    public static JsonValue readJson(Object object) {return readJson(JSON.toJson(object));}

    public static JsonValue readJson(String json) {
        try {
            return Json.createReader(new StringReader(json)).readValue();
        } catch (JsonParsingException e) {
            var offset = (int) e.getLocation().getStreamOffset();
            throw new RuntimeException("can't parse json:\n" + json.substring(0, offset) + "👉" + json.substring(offset), e);
        }
    }

    public static String formatJson(JsonValue value) {
        if (value == null) return null;
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

    private static String camelToKebab(String in) {return String.join("-", in.split("(?=\\p{javaUpperCase})")).toLowerCase(US);}

    @SneakyThrows(NoSuchFieldException.class)
    public static <T> T getField(Object instance, String fieldName) {
        Field field = instance.getClass().getDeclaredField(fieldName);
        return getField(instance, field);
    }

    @SneakyThrows(IllegalAccessException.class)
    public static <T> T getField(Object instance, Field field) {
        field.setAccessible(true);
        //noinspection unchecked
        return (T) field.get(instance);
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

    public static Stream<JsonValue> jsonNonAddDiff(JsonValue expected, JsonValue actual) {
        if (expected.getValueType() != actual.getValueType()) {
            expected = Json.createObjectBuilder().add("diff-dummy", expected).build();
            actual = Json.createObjectBuilder().add("diff-dummy", actual).build();
        }
        return Json.createDiff((JsonStructure) expected, (JsonStructure) actual).toJsonArray().stream()
            .filter(Utils::isNonAdd);
    }

    private static boolean isNonAdd(JsonValue jsonValue) {
        return !jsonValue.asJsonObject().getString("op").equals(ADD.operationName());
    }

    @Builder @Getter
    public static class ProblemDetails {
        public static ProblemDetails of(Exception exception) {
            return ProblemDetails.builder()
                .type(URI.create("urn:problem-type:" + errorCode(exception)))
                .title(exception.getClass().getSimpleName())
                .detail(exception.getMessage())
                .build();
        }

        URI type;
        String title;
        String detail;
    }
}

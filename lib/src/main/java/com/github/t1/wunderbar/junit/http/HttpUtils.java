package com.github.t1.wunderbar.junit.http;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParsingException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.StatusType;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.toList;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static jakarta.ws.rs.core.MediaType.CHARSET_PARAMETER;

@UtilityClass
public class HttpUtils {
    /** <code>application/json;charset=utf-8</code> */
    public static final MediaType APPLICATION_JSON_UTF8 = APPLICATION_JSON_TYPE.withCharset("utf-8");
    public static final MediaType PROBLEM_DETAIL_TYPE = MediaType.valueOf("application/problem+json;charset=utf-8");
    public static final Jsonb JSONB = JsonbBuilder.create(new JsonbConfig()
        .withFormatting(true)
        .withAdapters(new StatusTypeAdapter(), new MediaTypeAdapter()));

    static Charset charset(MediaType contentType) {
        var charsetName = (contentType == null) ? null : contentType.getParameters().get(CHARSET_PARAMETER);
        return (charsetName == null) ? ISO_8859_1 : Charset.forName(charsetName);
    }

    static MediaType firstMediaType(String string) {
        return (string == null) ? null : MediaType.valueOf(string.split(",", 2)[0]);
    }

    static StatusType toStatus(String string) {
        var code = Integer.parseInt(string.split(" ", 2)[0]);
        return Status.fromStatusCode(code);
    }

    public static Optional<String> optional(Properties properties, String method) {
        return Optional.ofNullable(properties.getProperty(method, null));
    }

    public static String formatJson(String json) {
        if (json == null || json.isBlank()) return json;

        var value = readJson(json);

        return formatJson(value);
    }

    public static <T> T fromJson(JsonValue jsonValue, Class<T> type) {
        return (jsonValue == null) ? null : JSONB.fromJson(jsonValue.toString(), type);
    }

    public static JsonObjectBuilder fromJson(String json) {
        return (json == null) ? Json.createObjectBuilder() : Json.createObjectBuilder(readJson(json).asJsonObject());
    }

    public static <T> T read(String string, MediaType contentType, Class<T> type) {
        if (isCompatible(APPLICATION_JSON_TYPE, contentType)) return JSONB.fromJson(string, type);
        throw new UnsupportedOperationException("unsupported content-type " + contentType);
    }

    public static String toJson(Object object) {return formatJson(readJson(object));}

    public static JsonValue readJson(Object object) {return readJson(JSONB.toJson(object));}

    public static JsonValue readJson(String json) {
        try {
            return ((json == null) || json.isEmpty()) ? JsonValue.NULL :
                Json.createReader(new StringReader(json)).readValue();
        } catch (JsonParsingException e) {
            var offset = (int) e.getLocation().getStreamOffset();
            var pre = (offset < 0) ? "" : json.substring(0, offset);
            var post = (offset < 0) ? json : json.substring(offset);
            throw new RuntimeException("can't parse json:\n" + pre + "👉" + post, e);
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

    public static String toFlatString(JsonValue jsonValue) {
        if (jsonValue == null || JsonValue.NULL.equals(jsonValue)) return "null";
        var string = (jsonValue instanceof JsonString) ? jsonString(jsonValue) : jsonValue.toString();
        return string.replace("\n", " ").replace("\"", "");
    }

    public static String jsonString(JsonValue value) {return (value == null) ? null : ((JsonString) value).getString();}

    public static String errorCode(Throwable exception) {
        var code = String.join("-", splitCamel(exception.getClass().getSimpleName())).toLowerCase(ROOT);
        if (code.endsWith("-exception")) code = code.substring(0, code.length() - 10);
        return code;
    }

    public static String[] splitCamel(String in) {return in.split("(?=\\p{javaUpperCase})");}

    public static String base64(String string) {
        return Base64.getEncoder().encodeToString(string.getBytes(UTF_8));
    }

    public static String base64decode(String string) {
        return new String(Base64.getDecoder().decode(string.getBytes(UTF_8)), UTF_8);
    }

    /**
     * Like {@link MediaType#isCompatible(MediaType)}, but taking suffixes like <b><code>+json</code></b> into account.
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc6838#section-4.2.8">RFC-6838</a>
     */
    public static boolean isCompatible(List<MediaType> left, List<MediaType> right) {
        if (left == null || left.isEmpty()) return right == null || right.isEmpty();
        for (MediaType leftType : left)
            if (right.stream().anyMatch(rightType -> isCompatible(leftType, rightType)))
                return true;
        return false;
    }

    /**
     * Like {@link MediaType#isCompatible(MediaType)}, but taking suffixes like <b><code>+json</code></b> into account.
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc6838#section-4.2.8">RFC-6838</a>
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

    /** The username (upn) in a JWT token string. Careful: this fails terribly with invalid tokens */
    public static String jwtUpn(String token) {
        return readJson(base64decode(token.split("\\.", 3)[1])).asJsonObject().getString("upn");
    }

    @SneakyThrows(IOException.class)
    public static Properties properties(String string) {
        var properties = new Properties();
        if (string != null) properties.load(new StringReader(string));
        return properties;
    }

    public static List<MediaType> mediaTypes(String string) {
        return (string == null) ? List.of() : Stream.of(string.replace(" ", "").split(","))
            .map(String::strip)
            .map(MediaType::valueOf)
            .collect(toList());
    }
}

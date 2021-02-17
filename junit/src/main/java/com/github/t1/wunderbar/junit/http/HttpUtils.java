package com.github.t1.wunderbar.junit.http;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.stream.JsonParsingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.CHARSET_PARAMETER;

@UtilityClass
public class HttpUtils {
    public static final MediaType APPLICATION_JSON_UTF8 = APPLICATION_JSON_TYPE.withCharset("utf-8");
    static final Jsonb JSONB = JsonbBuilder.create(new JsonbConfig().withFormatting(true));

    public static Charset charset(MediaType contentType) {
        var charsetName = (contentType == null) ? null : contentType.getParameters().get(CHARSET_PARAMETER);
        return (charsetName == null) ? ISO_8859_1 : Charset.forName(charsetName);
    }

    public static MediaType mediaType(String string) {
        return (string == null) ? null : MediaType.valueOf(string);
    }

    static StatusType toStatus(String string) {
        var code = Integer.parseInt(string.split(" ", 2)[0]);
        return Status.fromStatusCode(code);
    }

    static Optional<String> optional(Properties properties, String method) {
        return Optional.ofNullable(properties.getProperty(method, null));
    }

    public static JsonValue toJson(String string) {
        try {
            return string.isEmpty() ? JsonValue.NULL : Json.createReader(new StringReader(string)).read();
        } catch (JsonParsingException e) {
            return JsonValue.NULL;
        }
    }
}

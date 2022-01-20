package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.ws.rs.core.MediaType;

import static com.github.t1.wunderbar.junit.http.HttpUtils.APPLICATION_JSON_UTF8;

@ToString @Setter @Accessors(fluent = true, chain = true)
public class RestResponseSupplier implements ResponseSupplier {
    public static ResponseSupplier from(HttpResponse response) {
        return restResponse()
            .status(response.getStatus().getStatusCode())
            .contentType(response.getContentType().toString())
            .body(response.getJsonBody().map(JsonValue::asJsonObject).orElse(null));
    }

    public static RestResponseSupplier restResponse() {return new RestResponseSupplier();}

    int status = 200;
    String contentType = APPLICATION_JSON_UTF8.toString();
    JsonObjectBuilder builder;
    JsonValue body;

    RestResponseSupplier add(String name, String value) {
        if (builder == null) builder = Json.createObjectBuilder();
        builder.add(name, value);
        return this;
    }

    @Override
    public HttpResponse apply(HttpRequest request) {
        var response = HttpResponse.builder()
            .status(status)
            .contentType(MediaType.valueOf(contentType));
        if (body == null) body = builder.build();
        response.body(body);
        return response.build();
    }
}

package com.github.t1.wunderbar.mock;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Setter @Accessors(fluent = true, chain = true)
public class RestResponseSupplier implements ResponseSupplier {
    public static RestResponseSupplier restResponse() {return new RestResponseSupplier();}

    int status = 200;
    String contentType = "application/json;charset=utf-8";
    JsonObjectBuilder builder;
    JsonObject body;

    RestResponseSupplier add(String name, String value) {
        if (builder == null) builder = Json.createObjectBuilder();
        builder.add(name, value);
        return this;
    }

    @Override
    @SneakyThrows(IOException.class)
    public void apply(HttpServletRequest request, String requestBody, HttpServletResponse response) {
        response.setStatus(status);
        if (body == null) body = builder.build();
        Json.createWriter(response.getWriter()).write(body);
    }
}

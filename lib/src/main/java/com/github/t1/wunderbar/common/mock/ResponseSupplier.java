package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpResponse;

import javax.json.JsonValue;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.github.t1.wunderbar.common.mock.RestResponseSupplier.restResponse;

public interface ResponseSupplier {
    static ResponseSupplier from(HttpResponse response) {
        return restResponse()
            .status(response.getStatus().getStatusCode())
            .contentType(response.getContentType().toString())
            .body(response.getJsonBody().map(JsonValue::asJsonObject).orElse(null));
    }

    void apply(HttpServletRequest request, String requestBody, HttpServletResponse response);
}

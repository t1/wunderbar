package com.github.t1.wunderbar.mock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.github.t1.wunderbar.mock.RestResponseSupplier.restResponse;

public class RestErrorSupplier implements ResponseSupplier {
    private final RestResponseSupplier response = restResponse()
        .status(400)
        .contentType("application/problem+json;charset=utf-8");

    public static RestErrorSupplier restError() {return new RestErrorSupplier();}

    RestErrorSupplier status(int status) {
        response.status(status);
        return this;
    }

    RestErrorSupplier contentType(String contentType) {
        response.contentType(contentType);
        return this;
    }

    RestErrorSupplier detail(String detail) {
        response.add("detail", detail);
        return this;
    }

    RestErrorSupplier title(String title) {
        response.add("title", title);
        return this;
    }

    RestErrorSupplier type(String type) {
        response.add("type", type);
        return this;
    }

    @Override public void apply(HttpServletRequest request, String requestBody, HttpServletResponse response) {
        this.response.apply(request, requestBody, response);
    }
}

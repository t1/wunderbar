package com.github.t1.wunderbar.common.mock;

import lombok.ToString;

import static com.github.t1.wunderbar.common.mock.RestResponseSupplier.restResponse;
import static com.github.t1.wunderbar.junit.http.HttpUtils.PROBLEM_DETAIL_TYPE;

@ToString
public class RestErrorSupplier {
    private final RestResponseSupplier response = restResponse()
        .status(400)
        .contentType(PROBLEM_DETAIL_TYPE.toString());

    public static RestErrorSupplier restError() {return new RestErrorSupplier();}

    public RestErrorSupplier status(int status) {
        response.status(status);
        return this;
    }

    public RestErrorSupplier contentType(String contentType) {
        response.contentType(contentType);
        return this;
    }

    public RestErrorSupplier detail(String detail) {
        response.add("detail", detail);
        return this;
    }

    public RestErrorSupplier title(String title) {
        response.add("title", title);
        return this;
    }

    public RestErrorSupplier type(String type) {
        response.add("type", type);
        return this;
    }

    public RestResponseSupplier build() {return response;}
}

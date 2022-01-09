package com.github.t1.wunderbar.mock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface ResponseSupplier {
    void apply(HttpServletRequest request, String requestBody, HttpServletResponse response);
}

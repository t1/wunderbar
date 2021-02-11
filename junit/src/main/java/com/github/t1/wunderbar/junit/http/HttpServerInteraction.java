package com.github.t1.wunderbar.junit.http;

import lombok.Value;

public @Value class HttpServerInteraction {
    HttpServerRequest request;
    HttpServerResponse response;
}

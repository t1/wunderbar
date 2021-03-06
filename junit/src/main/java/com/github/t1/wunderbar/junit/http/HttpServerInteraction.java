package com.github.t1.wunderbar.junit.http;

import lombok.Value;
import lombok.With;

public @Value @With class HttpServerInteraction {
    int number;
    HttpServerRequest request;
    HttpServerResponse response;
}

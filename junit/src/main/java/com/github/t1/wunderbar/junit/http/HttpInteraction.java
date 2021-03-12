package com.github.t1.wunderbar.junit.http;

import lombok.Value;
import lombok.With;

public @Value @With class HttpInteraction {
    int number;
    HttpRequest request;
    HttpResponse response;
}

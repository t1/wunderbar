package com.github.t1.wunderbar.junit.http;

import lombok.Value;
import lombok.With;

public @Value @With class HttpInteraction {
    int number;
    HttpRequest request;
    HttpResponse response;

    @Override public String toString() {return "HttpInteraction#" + number + "\n" + request + "\n\n" + response;}
}

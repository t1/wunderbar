package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import lombok.EqualsAndHashCode;
import lombok.Value;

import static com.github.t1.wunderbar.junit.http.HttpUtils.toFlatString;

@Value @EqualsAndHashCode(callSuper = true)
class WunderBarMockInteractionExpectation extends WunderBarMockExpectation {
    HttpRequest expectedRequest;
    HttpResponse response;

    @Override public String toString() {return info(expectedRequest) + " => " + response.getStatusString();}

    private String info(HttpRequest request) {
        return request.getMethod() + " " + request.getUri()
               + (request.has("query") ? " query=" + request.get("query") : "")
               + (request.has("variables") ? " vars=" + toFlatString(request.get("variables")) : "")
               + (request.has("operationName") ? " op=" + toFlatString(request.get("operationName")) : "");
    }

    @Override public boolean matches(HttpRequest request) {return expectedRequest.matches(request);}

    @Override public HttpResponse handle(HttpRequest request) {return response;}
}

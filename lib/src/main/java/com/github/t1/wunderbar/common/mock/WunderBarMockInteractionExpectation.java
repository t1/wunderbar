package com.github.t1.wunderbar.common.mock;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import static com.github.t1.wunderbar.junit.http.HttpUtils.toFlatString;

@Slf4j
@Data @Builder @EqualsAndHashCode(callSuper = true)
class WunderBarMockInteractionExpectation extends WunderBarMockExpectation {
    private final HttpRequest expectedRequest;
    private final int maxCallCount;
    private final HttpResponse response;

    private int callCount;

    @Override public String toString() {return info(expectedRequest) + " => " + response.getStatusString();}

    private String info(HttpRequest request) {
        return request.getMethod() + " " + request.getUri()
               + (request.has("query") ? " query=" + request.get("query") : "")
               + (request.has("variables") ? " vars=" + toFlatString(request.get("variables")) : "")
               + (request.has("operationName") ? " op=" + toFlatString(request.get("operationName")) : "")
               + " (max:" + maxCallCount + ")";
    }

    @Override public boolean matches(HttpRequest request) {return expectedRequest.matches(request);}

    @Override public HttpResponse handle(HttpRequest request) {
        ++callCount;
        log.debug("invocation #{} of {}", callCount, this);
        return response;
    }

    @Override public boolean moreInvocationsAllowed() {return callCount < maxCallCount;}
}

package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.junit.http.HttpResponse;
import lombok.Value;

import java.util.function.Function;

/**
 * We need to be able to distinguish <em>by a Java type</em> between the <em>actual</em> {@link HttpResponse}
 * and the <em>expected</em> one, so a {@link AfterInteraction} can consume or produce both.
 */
public @Value class ActualHttpResponse {
    HttpResponse value;

    public ActualHttpResponse map(Function<HttpResponse, HttpResponse> function) {
        return new ActualHttpResponse(function.apply(this.value));
    }
}

package com.github.t1.wunderbar.junit.consumer.system;

import com.github.t1.wunderbar.junit.consumer.BarWriter;
import com.github.t1.wunderbar.junit.http.Authorization;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpRequest.HttpRequestBuilder;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.HttpResponse.HttpResponseBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.function.Supplier;

import static com.github.t1.wunderbar.junit.http.HttpUtils.formatJson;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

@Slf4j
@RequiredArgsConstructor
class BarFilter implements ClientRequestFilter, ClientResponseFilter {
    private final BarWriter bar;
    private final Supplier<Boolean> recording;
    private Builder builder;

    @Override public String toString() {return "BarFilter for " + bar;}

    @Override public void filter(ClientRequestContext requestContext) {
        if (bar == null || !recording.get()) return;
        this.builder = new Builder(requestContext);
    }

    @Override public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        if (bar == null || !recording.get()) return;
        this.builder.add(responseContext);
        bar.save(builder.buildRequest(), builder.buildResponse());
        this.builder = null;
    }

    private static class Builder {
        private final HttpRequestBuilder request = HttpRequest.builder();
        private StringBuilder requestStringBuilder;
        private final MediaType requestMediaType;

        private final HttpResponseBuilder response = HttpResponse.builder();

        private Builder(ClientRequestContext requestContext) {
            request
                    .method(requestContext.getMethod())
                    .uri(local(requestContext.getUri()));
            log.info("request {} {}", requestContext.getMethod(), request.build().getUri());
            if (!requestContext.getAcceptableMediaTypes().isEmpty())
                request.accept(requestContext.getAcceptableMediaTypes().get(0));
            this.requestMediaType = requestContext.getMediaType();
            if (requestMediaType != null)
                request.contentType(requestMediaType);
            request.authorization(Authorization.valueOf(requestContext.getHeaderString(AUTHORIZATION)));
            if (requestContext.hasEntity()) {
                requestStringBuilder = new StringBuilder();
                requestContext.setEntityStream(new StringBuildingOutputStreamFilter(requestContext.getEntityStream(), requestStringBuilder));
            }
        }

        private URI local(URI uri) {
            var schemeAndHost = URI.create(uri.getScheme() + "://" + uri.getRawAuthority());
            return URI.create("/" + schemeAndHost.relativize(uri));
        }

        private HttpRequest buildRequest() {
            if (requestStringBuilder != null) {
                var body = requestStringBuilder.toString();
                if (APPLICATION_JSON_TYPE.isCompatible(requestMediaType)) body = formatJson(body);
                request.body(body);
            }
            return request.build();
        }

        @SneakyThrows(IOException.class)
        private void add(ClientResponseContext responseContext) {
            log.info("response {} {}", responseContext.getStatus(), responseContext.getStatusInfo().getReasonPhrase());
            response.status(responseContext.getStatusInfo());
            var contentType = responseContext.getMediaType();
            if (contentType != null)
                response.contentType(contentType);
            if (responseContext.hasEntity()) {
                var bytes = responseContext.getEntityStream().readAllBytes();
                if (bytes.length > 0) { // resteasy returns hasEntity = true, even when content-length is 0
                    responseContext.setEntityStream(new ByteArrayInputStream(bytes));
                    var body = new String(bytes);
                    if (APPLICATION_JSON_TYPE.isCompatible(contentType)) body = formatJson(body);
                    response.body(body);
                }
            }
        }

        private HttpResponse buildResponse() {
            return response.build();
        }
    }
}

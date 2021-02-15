package com.github.t1.wunderbar.junit.consumer.system;

import com.github.t1.wunderbar.junit.Bar;
import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerRequest.HttpServerRequestBuilder;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import com.github.t1.wunderbar.junit.http.HttpServerResponse.HttpServerResponseBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import static com.github.t1.wunderbar.junit.Utils.formatJson;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

@Slf4j
public class BarFilter implements ClientRequestFilter, ClientResponseFilter {
    private final Bar bar;
    private Builder builder;

    public BarFilter(Bar bar) {
        this.bar = bar;
    }

    @Override public String toString() { return "BarFilter for " + bar; }

    @Override public void filter(ClientRequestContext requestContext) {
        if (bar == null) return;
        this.builder = new Builder(requestContext);
    }

    @Override public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        if (bar == null) return;
        this.builder.add(responseContext);
        bar.save(builder.buildRequest(), builder.buildResponse());
        this.builder = null;
    }

    private static class Builder {
        private final HttpServerRequestBuilder request = HttpServerRequest.builder();
        private StringBuilder requestStringBuilder;
        private final MediaType requestMediaType;

        private final HttpServerResponseBuilder response = HttpServerResponse.builder();

        public Builder(ClientRequestContext requestContext) {
            request
                .method(requestContext.getMethod())
                .uri(local(requestContext.getUri()));
            log.info("request {} {}", requestContext.getMethod(), request.build().getUri());
            if (!requestContext.getAcceptableMediaTypes().isEmpty())
                request.accept(requestContext.getAcceptableMediaTypes().get(0));
            this.requestMediaType = requestContext.getMediaType();
            if (requestMediaType != null)
                request.contentType(requestMediaType);
            if (requestContext.hasEntity()) {
                requestStringBuilder = new StringBuilder();
                requestContext.setEntityStream(new StringBuildingOutputStreamFilter(requestContext.getEntityStream(), requestStringBuilder));
            }
        }

        private URI local(URI uri) {
            var schemeAndHost = URI.create(uri.getScheme() + "://" + uri.getRawAuthority());
            return URI.create("/" + schemeAndHost.relativize(uri));
        }

        public HttpServerRequest buildRequest() {
            if (requestStringBuilder != null) {
                var body = requestStringBuilder.toString();
                if (APPLICATION_JSON_TYPE.isCompatible(requestMediaType)) body = formatJson(body);
                request.body(body);
            }
            return request.build();
        }

        @SneakyThrows(IOException.class)
        void add(ClientResponseContext responseContext) {
            log.info("response {} {}", responseContext.getStatus(), responseContext.getStatusInfo().getReasonPhrase());
            response.status(responseContext.getStatusInfo());
            var contentType = responseContext.getMediaType();
            if (contentType != null)
                response.contentType(contentType);
            if (responseContext.hasEntity()) {
                var bytes = responseContext.getEntityStream().readAllBytes();
                responseContext.setEntityStream(new ByteArrayInputStream(bytes));
                var body = new String(bytes);
                if (APPLICATION_JSON_TYPE.isCompatible(contentType)) body = formatJson(body);
                response.body(body);
            }
        }

        public HttpServerResponse buildResponse() {
            return response.build();
        }
    }
}

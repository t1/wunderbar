package com.github.t1.wunderbar.mock;

import com.github.t1.wunderbar.common.mock.MockService;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import static java.util.stream.Collectors.joining;

@WebServlet(name = "WunderBar-Mock-Servlet", urlPatterns = {"/*"})
public class MockServlet extends HttpServlet {
    @Inject MockService service;

    public void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        HttpRequest request = HttpRequest.builder()
            .method(servletRequest.getMethod())
            .uri(maybe(servletRequest.getPathInfo(), "") +
                 maybe(servletRequest.getQueryString(), "?"))
            .contentType(servletRequest.getContentType())
            .accept(servletRequest.getHeader("Accept"))
            .body(servletRequest.getReader().lines().collect(joining()))
            .build();

        HttpResponse response = service.service(request);

        servletResponse.setStatus(response.getStatus().getStatusCode());
        servletResponse.setContentType(response.getContentType().toString());
        response.body().ifPresent(servletResponse.getWriter()::write);
    }

    private String maybe(String string, String prefix) {
        return string == null ? "" : prefix + string;
    }
}

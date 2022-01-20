package com.github.t1.wunderbar.mock;

import com.github.t1.wunderbar.common.mock.MockService;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static java.util.stream.Collectors.joining;

@WebServlet(name = "WunderBar-Mock-Servlet", urlPatterns = {"/*"})
public class MockServlet extends HttpServlet {
    @Inject MockService service;

    public void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        HttpRequest request = HttpRequest.builder()
            .method(servletRequest.getMethod())
            .contentType(servletRequest.getContentType())
            .body(servletRequest.getReader().lines().collect(joining()))
            .build();

        HttpResponse response = service.service(request);

        servletResponse.setStatus(response.getStatus().getStatusCode());
        servletResponse.setContentType(response.getContentType().toString());
        response.getBody().ifPresent(servletResponse.getWriter()::write);
    }
}

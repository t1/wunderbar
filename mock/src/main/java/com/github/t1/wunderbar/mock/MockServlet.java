package com.github.t1.wunderbar.mock;

import com.github.t1.wunderbar.common.mock.MockService;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@WebServlet(name = "WunderBar-Mock-Servlet", urlPatterns = {"/*"})
public class MockServlet extends HttpServlet {
    @Inject MockService service;

    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        service.service(request, response);
    }
}

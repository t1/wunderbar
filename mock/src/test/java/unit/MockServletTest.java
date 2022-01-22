package unit;

import com.github.t1.wunderbar.common.mock.MockService;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.mock.MockServlet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MockServletTest {
    @Mock MockService service;
    @InjectMocks MockServlet servlet;

    @Mock HttpServletRequest servletRequest;
    @Mock HttpServletResponse servletResponse;
    @Mock PrintWriter responseWriter;

    @Test void shouldMapFromServletAndBack() throws Exception {
        given(servletRequest.getMethod()).willReturn("POST");
        given(servletRequest.getPathInfo()).willReturn("/foo/bar");
        given(servletRequest.getQueryString()).willReturn("foo=bar");
        given(servletRequest.getContentType()).willReturn("application/foo");
        given(servletRequest.getHeader("Accept")).willReturn("application/bar");
        given(servletRequest.getReader()).willReturn(new BufferedReader(new StringReader("{\"foo\":\"bar\"}")));
        given(service.service(HttpRequest.builder()
            .method("POST")
            .uri("/foo/bar?foo=bar")
            .contentType("application/foo")
            .accept("application/bar")
            .body("{\"foo\":\"bar\"}")
            .build())
        ).willReturn(HttpResponse.builder()
            .status(BAD_REQUEST)
            .contentType("application/bar")
            .body("ok")
            .build());
        given(servletResponse.getWriter()).willReturn(responseWriter);

        servlet.service(servletRequest, servletResponse);

        verify(servletResponse).setStatus(400);
        verify(servletResponse).setContentType("application/bar");
        verify(responseWriter).write("ok");
    }
}

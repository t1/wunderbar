package test;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.HttpResponse.HttpResponseBuilder;
import com.github.t1.wunderbar.junit.http.HttpServer;
import com.github.t1.wunderbar.common.mock.MockService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/** this server would normally be a real server running somewhere */
public class DummyServer implements Extension, AfterAllCallback {
    private static final HttpServer SERVER = new HttpServer(DummyServer::handle);

    public URI baseUri() {return SERVER.baseUri();}

    @SneakyThrows(IOException.class)
    private static HttpResponse handle(HttpRequest request) {
        var httpServletRequest = new DummyHttpServletRequest(request);
        var response = new DummyHttpServletResponse();
        new MockService().service(httpServletRequest, response);
        return response.toResponse();
    }

    @Override public void afterAll(ExtensionContext context) {SERVER.stop();}

    @RequiredArgsConstructor
    private static class DummyHttpServletRequest implements HttpServletRequest {
        private final HttpRequest request;

        @Override public String getAuthType() {throw new UnsupportedOperationException();}

        @Override public Cookie[] getCookies() {throw new UnsupportedOperationException();}

        @Override public long getDateHeader(String name) {throw new UnsupportedOperationException();}

        @Override public String getHeader(String name) {throw new UnsupportedOperationException();}

        @Override public Enumeration<String> getHeaders(String name) {throw new UnsupportedOperationException();}

        @Override public Enumeration<String> getHeaderNames() {throw new UnsupportedOperationException();}

        @Override public int getIntHeader(String name) {throw new UnsupportedOperationException();}

        @Override public String getMethod() {return request.getMethod();}

        @Override public String getPathInfo() {throw new UnsupportedOperationException();}

        @Override public String getPathTranslated() {throw new UnsupportedOperationException();}

        @Override public String getContextPath() {throw new UnsupportedOperationException();}

        @Override public String getQueryString() {throw new UnsupportedOperationException();}

        @Override public String getRemoteUser() {throw new UnsupportedOperationException();}

        @Override public boolean isUserInRole(String role) {throw new UnsupportedOperationException();}

        @Override public Principal getUserPrincipal() {throw new UnsupportedOperationException();}

        @Override public String getRequestedSessionId() {throw new UnsupportedOperationException();}

        @Override public String getRequestURI() {return request.getUri().toString();}

        @Override public StringBuffer getRequestURL() {throw new UnsupportedOperationException();}

        @Override public String getServletPath() {throw new UnsupportedOperationException();}

        @Override public HttpSession getSession(boolean create) {throw new UnsupportedOperationException();}

        @Override public HttpSession getSession() {throw new UnsupportedOperationException();}

        @Override public String changeSessionId() {throw new UnsupportedOperationException();}

        @Override public boolean isRequestedSessionIdValid() {throw new UnsupportedOperationException();}

        @Override public boolean isRequestedSessionIdFromCookie() {throw new UnsupportedOperationException();}

        @Override public boolean isRequestedSessionIdFromURL() {throw new UnsupportedOperationException();}

        @Deprecated @Override public boolean isRequestedSessionIdFromUrl() {throw new UnsupportedOperationException();}

        @Override public boolean authenticate(HttpServletResponse response) {throw new UnsupportedOperationException();}

        @Override public void login(String username, String password) {throw new UnsupportedOperationException();}

        @Override public void logout() {throw new UnsupportedOperationException();}

        @Override public Collection<Part> getParts() {throw new UnsupportedOperationException();}

        @Override public Part getPart(String name) {throw new UnsupportedOperationException();}

        @Override public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) {throw new UnsupportedOperationException();}

        @Override public Object getAttribute(String name) {throw new UnsupportedOperationException();}

        @Override public Enumeration<String> getAttributeNames() {throw new UnsupportedOperationException();}

        @Override public String getCharacterEncoding() {throw new UnsupportedOperationException();}

        @Override public void setCharacterEncoding(String env) {throw new UnsupportedOperationException();}

        @Override public int getContentLength() {throw new UnsupportedOperationException();}

        @Override public long getContentLengthLong() {throw new UnsupportedOperationException();}

        @Override public String getContentType() {return request.getContentType().toString();}

        @Override public ServletInputStream getInputStream() {throw new UnsupportedOperationException();}

        @Override public String getParameter(String name) {throw new UnsupportedOperationException();}

        @Override public Enumeration<String> getParameterNames() {throw new UnsupportedOperationException();}

        @Override public String[] getParameterValues(String name) {throw new UnsupportedOperationException();}

        @Override public Map<String, String[]> getParameterMap() {throw new UnsupportedOperationException();}

        @Override public String getProtocol() {return "HTTP/1.1";}

        @Override public String getScheme() {throw new UnsupportedOperationException();}

        @Override public String getServerName() {throw new UnsupportedOperationException();}

        @Override public int getServerPort() {throw new UnsupportedOperationException();}

        @Override public BufferedReader getReader() {return new BufferedReader(new StringReader(request.getBody().orElse("")));}

        @Override public String getRemoteAddr() {throw new UnsupportedOperationException();}

        @Override public String getRemoteHost() {throw new UnsupportedOperationException();}

        @Override public void setAttribute(String name, Object o) {throw new UnsupportedOperationException();}

        @Override public void removeAttribute(String name) {throw new UnsupportedOperationException();}

        @Override public Locale getLocale() {throw new UnsupportedOperationException();}

        @Override public Enumeration<Locale> getLocales() {throw new UnsupportedOperationException();}

        @Override public boolean isSecure() {throw new UnsupportedOperationException();}

        @Override public RequestDispatcher getRequestDispatcher(String path) {throw new UnsupportedOperationException();}

        @Deprecated @Override public String getRealPath(String path) {throw new UnsupportedOperationException();}

        @Override public int getRemotePort() {throw new UnsupportedOperationException();}

        @Override public String getLocalName() {throw new UnsupportedOperationException();}

        @Override public String getLocalAddr() {throw new UnsupportedOperationException();}

        @Override public int getLocalPort() {throw new UnsupportedOperationException();}

        @Override public ServletContext getServletContext() {throw new UnsupportedOperationException();}

        @Override public AsyncContext startAsync() throws IllegalStateException {throw new UnsupportedOperationException();}

        @Override public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {throw new UnsupportedOperationException();}

        @Override public boolean isAsyncStarted() {throw new UnsupportedOperationException();}

        @Override public boolean isAsyncSupported() {throw new UnsupportedOperationException();}

        @Override public AsyncContext getAsyncContext() {throw new UnsupportedOperationException();}

        @Override public DispatcherType getDispatcherType() {throw new UnsupportedOperationException();}
    }

    private static class DummyHttpServletResponse implements HttpServletResponse {
        private final HttpResponseBuilder response = HttpResponse.builder();
        private final StringWriter writer = new StringWriter();

        @Override public void addCookie(Cookie cookie) {throw new UnsupportedOperationException();}

        @Override public boolean containsHeader(String name) {throw new UnsupportedOperationException();}

        @Override public String encodeURL(String url) {throw new UnsupportedOperationException();}

        @Override public String encodeRedirectURL(String url) {throw new UnsupportedOperationException();}

        @Deprecated @Override public String encodeUrl(String url) {throw new UnsupportedOperationException();}

        @Deprecated @Override public String encodeRedirectUrl(String url) {throw new UnsupportedOperationException();}

        @Override public void sendError(int sc, String msg) {response.status(Status.fromStatusCode(sc)).body(msg);}

        @Override public void sendError(int sc) {throw new UnsupportedOperationException();}

        @Override public void sendRedirect(String location) {throw new UnsupportedOperationException();}

        @Override public void setDateHeader(String name, long date) {throw new UnsupportedOperationException();}

        @Override public void addDateHeader(String name, long date) {throw new UnsupportedOperationException();}

        @Override public void setHeader(String name, String value) {throw new UnsupportedOperationException();}

        @Override public void addHeader(String name, String value) {throw new UnsupportedOperationException();}

        @Override public void setIntHeader(String name, int value) {throw new UnsupportedOperationException();}

        @Override public void addIntHeader(String name, int value) {throw new UnsupportedOperationException();}

        @Override public void setStatus(int sc) {response.status(Status.fromStatusCode(sc));}

        @Deprecated @Override public void setStatus(int sc, String sm) {throw new UnsupportedOperationException();}

        @Override public int getStatus() {throw new UnsupportedOperationException();}

        @Override public String getHeader(String name) {throw new UnsupportedOperationException();}

        @Override public Collection<String> getHeaders(String name) {throw new UnsupportedOperationException();}

        @Override public Collection<String> getHeaderNames() {throw new UnsupportedOperationException();}

        @Override public String getCharacterEncoding() {throw new UnsupportedOperationException();}

        @Override public String getContentType() {throw new UnsupportedOperationException();}

        @Override public ServletOutputStream getOutputStream() {throw new UnsupportedOperationException();}

        @Override public PrintWriter getWriter() {return new PrintWriter(writer);}

        @Override public void setCharacterEncoding(String charset) {throw new UnsupportedOperationException();}

        @Override public void setContentLength(int len) {throw new UnsupportedOperationException();}

        @Override public void setContentLengthLong(long len) {throw new UnsupportedOperationException();}

        @Override public void setContentType(String type) {response.contentType(MediaType.valueOf(type));}

        @Override public void setBufferSize(int size) {throw new UnsupportedOperationException();}

        @Override public int getBufferSize() {throw new UnsupportedOperationException();}

        @Override public void flushBuffer() {throw new UnsupportedOperationException();}

        @Override public void resetBuffer() {throw new UnsupportedOperationException();}

        @Override public boolean isCommitted() {throw new UnsupportedOperationException();}

        @Override public void reset() {throw new UnsupportedOperationException();}

        @Override public void setLocale(Locale loc) {throw new UnsupportedOperationException();}

        @Override public Locale getLocale() {throw new UnsupportedOperationException();}

        public HttpResponse toResponse() {return response.body(writer.toString()).build();}
    }
}

package com.github.t1.wunderbar.mock;

import lombok.extern.slf4j.Slf4j;

import javax.json.Json;
import javax.json.JsonValue;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringReader;

import static javax.json.JsonValue.ValueType.OBJECT;

@Slf4j
@WebServlet(name = "WunderBar-Mock-Servlet", urlPatterns = {"/*"})
public class MockServlet extends HttpServlet {
    @Override protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        log.info("received {}:{}", req.getMethod(), req.getRequestURI());

        var expected = Json.createReader(new StringReader(
            "{" +
            "\"query\":\"query product($id: String!) { product(id: $id) {id name description price} }\"," +
            "\"variables\":{\"id\":\"existing-product-id\"}," +
            "\"operationName\":\"product\"" +
            "}"
        )).readObject();
        if (expected.equals(readJson(req))) {
            resp.setStatus(200);
            resp.setContentType("application/json;charset=utf-8");
            resp.getWriter().write(
                "{\n" +
                "    \"data\": {\n" +
                "        \"product\": {\n" +
                "            \"description\": null,\n" +
                "            \"id\": \"existing-product-id\",\n" +
                "            \"name\": \"some-product-name\",\n" +
                "            \"price\": 1599\n" +
                "        }\n" +
                "    }\n" +
                "}");
            return;
        }

        resp.setStatus(400);
        resp.getWriter().write("unmatched " + req.getMethod() + " request " + req.getRequestURL());
    }

    private boolean isGraphQlRequest(HttpServletRequest req) throws IOException {
        return isJsonObject(req);
    }

    private boolean isJsonObject(HttpServletRequest req) throws IOException {
        return isJson(req) && readJson(req).getValueType() == OBJECT;
    }

    private JsonValue readJson(HttpServletRequest req) throws IOException {
        return Json.createReader(req.getReader()).readValue();
    }

    private boolean isJson(HttpServletRequest req) {
        return req.getContentType().contains("json");
    }
}

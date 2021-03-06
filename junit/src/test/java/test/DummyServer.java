package test;

import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.HttpServer;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.json.Json;
import javax.ws.rs.core.MediaType;
import java.io.StringReader;
import java.net.URI;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/** this server would normally be a real server running somewhere */
public class DummyServer implements Extension, AfterEachCallback {
    private static final MediaType PROBLEM_DETAIL = MediaType.valueOf("application/problem+json;charset=utf-8");

    private final HttpServer server = new HttpServer(DummyServer::handle);

    public URI baseUri() { return server.baseUri(); }

    private static HttpResponse handle(HttpRequest request) {
        var isGraphQl = request.getUri().toString().equals("/graphql");
        return isGraphQl ? handleGraphQl(request) : handleRest(request);
    }

    private static HttpResponse handleGraphQl(HttpRequest request) {
        if (request.getBody().isEmpty()) return error("validation-error", "no body in GraphQL request");
        var body = Json.createReader(new StringReader(request.getBody().get())).readObject();
        if (!body.getString("query").equals("query product($id: String!) { product(id: $id) {id name price} }"))
            return error("unexpected-query", "unexpected query: [" + body.getString("query") + "]");
        var id = body.getJsonObject("variables").getString("id");
        switch (id) {
            case "existing-product-id":
                return HttpResponse.builder().body("{\"data\":{\"product\":{\"id\":\"" + id + "\", \"name\":\"some-product-name\", \"price\": 1599}}}").build();
            case "forbidden-product-id":
                return error("product-forbidden", "product " + id + " is forbidden");
            case "unexpected-fail":
                return error("unexpected-fail", "product " + id + " fails unexpectedly");
            default:
                return error("product-not-found", "product " + id + " not found");
        }
    }

    private static HttpResponse error(String code, String message) {
        return HttpResponse.builder()
            .body("{\"errors\": [\n" +
                "{\"extensions\": {\"code\": \"" + code + "\"},\"message\": \"" + message + "\"}" +
                "]}\n")
            .build();
    }

    private static HttpResponse handleRest(HttpRequest request) {
        var response = HttpResponse.builder();
        switch (request.getUri().toString()) {
            case "/q/health/ready":
                response.body("{\"status\": \"UP\"}");
                break;
            case "/rest/products/existing-product-id":
                response.body("{\"id\":\"existing-product-id\", \"name\":\"some-product-name\", \"price\": 1599}");
                break;
            case "/rest/products/forbidden-product-id":
                response.status(FORBIDDEN).contentType(PROBLEM_DETAIL).body("{\n" +
                    "    \"detail\": \"HTTP 403 Forbidden\",\n" +
                    "    \"title\": \"ForbiddenException\",\n" +
                    "    \"type\": \"urn:problem-type:javax.ws.rs.ForbiddenException\"\n" +
                    "}\n");
                break;
            default:
                response.status(NOT_FOUND).contentType(PROBLEM_DETAIL).body("{\n" +
                    "    \"detail\": \"HTTP 404 Not Found\",\n" +
                    "    \"title\": \"NotFoundException\",\n" +
                    "    \"type\": \"urn:problem-type:javax.ws.rs.NotFoundException\"\n" +
                    "}\n");
        }
        return response.build();
    }

    @Override public void afterEach(ExtensionContext context) { server.stop(); }
}

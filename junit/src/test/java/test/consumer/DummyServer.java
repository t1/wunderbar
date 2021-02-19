package test.consumer;

import com.github.t1.wunderbar.junit.http.HttpServer;
import com.github.t1.wunderbar.junit.http.HttpServerRequest;
import com.github.t1.wunderbar.junit.http.HttpServerResponse;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.json.Json;
import javax.ws.rs.core.MediaType;
import java.io.StringReader;
import java.net.URI;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/** this server would normally be a real server running somewhere */
public class DummyServer implements Extension, AfterAllCallback {
    private static final MediaType PROBLEM_DETAIL = MediaType.valueOf("application/problem+json;charset=utf-8");

    private final HttpServer server = new HttpServer(DummyServer::handle);

    public URI endpoint() { return server.baseUri(); }

    private static HttpServerResponse handle(HttpServerRequest request) {
        var isGraphQl = request.getUri().toString().equals("/graphql");
        return isGraphQl ? handleGraphQl(request) : handleRest(request);
    }

    private static HttpServerResponse handleGraphQl(HttpServerRequest request) {
        assert request.getBody().isPresent();
        var body = Json.createReader(new StringReader(request.getBody().get())).readObject();
        assert body.getString("query").equals("query product($id: String!) { product(id: $id) {id name} }")
            : "unexpected query: [" + body.getString("query") + "]";
        var response = HttpServerResponse.builder();
        var id = body.getJsonObject("variables").getString("id");
        switch (id) {
            case "existing-product-id":
                response.body("{\"data\":{\"product\":{\"id\":\"" + id + "\", \"name\":\"some-product-name\"}}}");
                break;
            case "forbidden-product-id":
                response.body("{\"errors\": [\n" +
                    "{\"extensions\": {\"code\": \"product-forbidden\"},\"message\": \"product " + id + " is forbidden\"}" +
                    "]}\n");
                break;
            default:
                response.body("{\"errors\": [\n" +
                    "{\"extensions\": {\"code\": \"product-not-found\"},\"message\": \"product " + id + " not found\"}" +
                    "]}\n");
                break;
        }
        return response.build();
    }

    private static HttpServerResponse handleRest(HttpServerRequest request) {
        var response = HttpServerResponse.builder();
        switch (request.getUri().toString()) {
            case "/rest/products/existing-product-id":
                response.body("{\"id\":\"existing-product-id\", \"name\":\"some-product-name\"}");
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

    @Override public void afterAll(ExtensionContext context) { server.stop(); }
}

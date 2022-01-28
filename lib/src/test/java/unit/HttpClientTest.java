package unit;

import com.github.t1.wunderbar.junit.http.HttpClient;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.BDDAssertions.then;

class HttpClientTest {
    private static final HttpServer SERVER = new HttpServer(HttpClientTest::handle);
    private static String body;
    private static MediaType responseContentType;

    private static HttpResponse handle(HttpRequest request) {
        return HttpResponse.builder().contentType(responseContentType).body(body).build();
    }

    @AfterAll static void tearDown() {SERVER.stop();}

    HttpClient client = new HttpClient(SERVER.baseUri());

    @Test void shouldReadJson() {
        responseContentType = APPLICATION_JSON_TYPE;
        body = "{\"foo\":\"bar\"}";

        var response = client.send(HttpRequest.builder().build());

        then(response.getStatus()).isEqualTo(OK);
        then(response.getBody()).isEqualTo("{\"foo\":\"bar\"}");
    }

    @Test void shouldReadXml() {
        responseContentType = APPLICATION_XML_TYPE;
        body = "<foo>bar</foo>";

        var response = client.send(HttpRequest.builder().build());

        then(response.getStatus()).isEqualTo(OK);
        then(response.getBody()).isEqualTo("<foo>bar</foo>");
    }
}

package test;

import com.github.t1.wunderbar.junit.http.HttpClient;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.HttpServer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.BDDAssertions.then;

class HttpServerTest {
    @NoArgsConstructor @AllArgsConstructor
    public static @Data class Foo {
        String foo;
    }

    @Test void should() {
        var server = new HttpServer(8080, HttpServerTest::handle);

        try {
            var client = new HttpClient(server.baseUri()).logging(true);

            var response = client.send(HttpRequest.builder().body(new Foo("bar")).build());

            then(response.getStatus()).isEqualTo(OK);
            then(response.as(Foo.class).getFoo()).isEqualTo("bar+");
        } finally {
            server.stop();
        }
    }

    private static HttpResponse handle(HttpRequest request) {
        var body = new Foo(request.as(Foo.class).getFoo() + "+");
        return HttpResponse.builder().body(body).build();
    }
}

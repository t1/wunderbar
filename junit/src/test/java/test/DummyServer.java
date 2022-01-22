package test;

import com.github.t1.wunderbar.common.mock.MockService;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.HttpServer;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.net.URI;

/** this server would normally be a real server running somewhere */
public class DummyServer implements Extension, AfterAllCallback {
    private static final HttpServer SERVER = new HttpServer(DummyServer::handle);

    public URI baseUri() {
        return SERVER.baseUri();
    }

    private static HttpResponse handle(HttpRequest request) {return new MockService().service(request);}

    @Override public void afterAll(ExtensionContext context) {SERVER.stop();}
}

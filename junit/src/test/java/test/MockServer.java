package test;

import com.github.t1.wunderbar.common.mock.MockService;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.http.HttpServer;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

import java.net.URI;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

/** this server would normally be a real server running somewhere */
public class MockServer implements Extension, BeforeEachCallback {
    private static boolean initialized;

    private final HttpServer server = new HttpServer(MockServer::handle);

    public URI baseUri() {
        return server.baseUri();
    }

    private static HttpResponse handle(HttpRequest request) {return new MockService().service(request);}

    @Override public void beforeEach(ExtensionContext context) {
        if (initialized) return;
        registerShutdownHook(server::stop, context);
        initialized = true;
    }

    private static void registerShutdownHook(CloseableResource shutDown, ExtensionContext context) {
        context.getRoot().getStore(GLOBAL).put(MockServer.class.getName(), shutDown);
    }
}

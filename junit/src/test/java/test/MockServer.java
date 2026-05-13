package test;

import com.github.t1.wunderbar.common.mock.MockService;
import com.github.t1.wunderbar.http.HttpRequest;
import com.github.t1.wunderbar.http.HttpResponse;
import com.github.t1.wunderbar.http.HttpServer;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.net.URI;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

/// This would normally be a real server running somewhere with a deployed [MockService], e.g. with the MockServlet.
public class MockServer implements Extension, BeforeEachCallback {
    private static boolean initialized;

    private static final HttpServer SERVER = new HttpServer(MockServer::handle);

    public URI baseUri() {return SERVER.baseUri();}

    private static HttpResponse handle(HttpRequest request) {return new MockService().service(request.withoutContextPath());}

    @Override public void beforeEach(@NonNull ExtensionContext context) {
        if (initialized) return;
        registerShutdownHook(SERVER::stop, context);
        initialized = true;
    }

    private static void registerShutdownHook(AutoCloseable shutDown, ExtensionContext context) {
        context.getRoot().getStore(GLOBAL).put(MockServer.class.getName(), shutDown);
    }
}

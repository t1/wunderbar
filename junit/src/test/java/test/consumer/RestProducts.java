package test.consumer;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import test.consumer.ProductResolver.Item;
import test.consumer.ProductResolver.Product;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.io.Closeable;

public class RestProducts {
    @Inject ProductsRestClient products;

    public Product product(Item item) {
        return products.product(item.getProductId());
    }

    public Product productWithPriceUpdate(Item item, int newPrice) {
        var patch = new Product().withId(item.getProductId()).withPrice(newPrice);
        return products.patch(patch);
    }

    @RegisterRestClient @Path("/products")
    public interface ProductsRestClient extends Closeable {
        @GET @Path("/{id}")
        Product product(@PathParam("id") String id);

        @PATCH
        Product patch(Product patch);
    }
}

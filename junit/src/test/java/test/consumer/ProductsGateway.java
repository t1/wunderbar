package test.consumer;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import test.consumer.ProductResolver.Item;
import test.consumer.ProductResolver.Product;

import java.io.Closeable;

class ProductsGateway {
    @Inject ProductsRestClient products;

    Product product(Item item) {
        return products.product(item.getProductId());
    }

    Product product(String customHeader, Item item) {
        return products.product(customHeader, item.getProductId());
    }

    Product productWithPriceUpdate(Item item, int newPrice) {
        var patch = Product.builder().id(item.getProductId()).price(newPrice).build();
        return products.patch(patch);
    }

    void postVoid() {
        products.postVoid();
    }

    @RegisterRestClient @Path("/products")
    public interface ProductsRestClient extends Closeable {
        @GET @Path("/{id}")
        Product product(@PathParam("id") String id);

        @GET @Path("/{id}")
        @Produces("application/vnd.product+json;charset=utf-8")
        Product product(@HeaderParam("Custom-Header") String customHeader, @PathParam("id") String id);

        @PATCH
        Product patch(Product patch);

        @POST
        Void postVoid();
    }
}

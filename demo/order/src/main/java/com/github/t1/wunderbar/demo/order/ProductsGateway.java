package com.github.t1.wunderbar.demo.order;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.io.Closeable;

public class ProductsGateway {
    @Inject ProductsRestClient products;

    public Product product(OrderItem item) {
        return products.product(item.getProductId());
    }

    public Product productWithPriceUpdate(OrderItem item, int newPrice) {
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

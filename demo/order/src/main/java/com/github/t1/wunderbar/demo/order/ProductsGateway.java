package com.github.t1.wunderbar.demo.order;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.io.Closeable;

public class ProductsGateway {
    @Inject ProductsRestClient products;

    public Product product(OrderItem item) {
        return products.product(item.getProductId());
    }

    public Product productWithPriceUpdate(OrderItem item, int newPrice) {
        return products.patch(new Product().withId(item.getProductId()).withPrice(newPrice));
    }

    @RegisterRestClient @Path("/products")
    public interface ProductsRestClient extends Closeable {
        @GET @Path("/{id}")
        Product product(@PathParam("id") String id);

        @PATCH
        Product patch(Product patch);
    }
}

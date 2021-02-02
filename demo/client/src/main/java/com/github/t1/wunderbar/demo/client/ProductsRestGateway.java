package com.github.t1.wunderbar.demo.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

public class ProductsRestGateway {
    ProductsRestClient products;

    public Product product(OrderItem item) {
        return products.product(item.getProductId());
    }

    @RegisterRestClient @Path("/products")
    public interface ProductsRestClient {
        @GET @Path("/{id}")
        Product product(@PathParam("id") String id);
    }
}

package com.github.t1.wunderbar.demo.server;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@GraphQLApi @Path("/products")
public class Boundary {
    @Query
    @GET @Path("/{id}")
    public Product getProduct(@PathParam("id") String id) {
        return Product.builder().id(id).name("product " + id).build();
    }
}

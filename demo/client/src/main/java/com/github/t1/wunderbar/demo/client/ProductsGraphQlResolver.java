package com.github.t1.wunderbar.demo.client;

import io.smallrye.graphql.client.typesafe.api.GraphQlClientApi;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Source;

@GraphQLApi
public class ProductsGraphQlResolver {
    Products products;

    public Product product(@Source OrderItem item) {
        return products.product(item.getProductId());
    }

    @GraphQlClientApi
    public interface Products {
        Product product(String id);
    }
}

package com.github.t1.wunderbar.demo.order;

import io.smallrye.graphql.client.typesafe.api.AuthorizationHeader;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Source;

@GraphQLApi
public class ProductsResolver {
    Products products;

    public Product product(@Source OrderItem item) {
        return products.product(item.getProductId());
    }

    public Product productWithPriceUpdate(OrderItem item, int newPrice) {
        var patch = new Product().withId(item.getProductId()).withPrice(newPrice);
        return products.update(patch);
    }

    @GraphQLClientApi
    public interface Products {
        Product product(@NonNull String id);
        @AuthorizationHeader
        @Mutation Product update(@NonNull Product patch);
    }
}

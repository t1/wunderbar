package com.github.t1.wunderbar.demo.order;

import io.smallrye.graphql.client.typesafe.api.AuthorizationHeader;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Source;

@GraphQLApi
@Slf4j
public class ProductsResolver {
    @Inject
    Products products;

    public Product product(@Source OrderItem item) {
        log.debug("resolve product for {}", item);
        var product = products.product(item.getProductId());
        log.debug("resolved product {}", product);
        return product;
    }

    public Product productWithPriceUpdate(OrderItem item, int newPrice) {
        log.debug("patch product {} with price {}", item, newPrice);
        var patch = new Product().withId(item.getProductId()).withPrice(newPrice);
        log.debug("patch {}", patch);
        var patchedProduct = products.update(patch);
        log.debug("patched product {}", patchedProduct);
        return patchedProduct;
    }

    @GraphQLClientApi(configKey = "products")
    public interface Products {
        Product product(@NonNull String id);

        @AuthorizationHeader
        @Mutation Product update(@NonNull Product patch);
    }
}

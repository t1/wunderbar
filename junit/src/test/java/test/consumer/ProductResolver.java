package test.consumer;

import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.With;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Source;

import static lombok.AccessLevel.PRIVATE;

@GraphQLApi
class ProductResolver {
    // would be @Inject
    Products products;

    Product product(@Source Item item) {
        return products.product(item.getProductId());
    }

    public Product productWithPriceUpdate(Item item, int newPrice) {
        return products.patch(new Product().withId(item.getProductId()).withPrice(newPrice));
    }

    @GraphQLClientApi(endpoint = "dummy")
    interface Products {
        Product product(@NonNull String id);
        Product patch(@NonNull Product patch);
    }

    @Getter @Setter @ToString @NoArgsConstructor
    @Builder(toBuilder = true) @With @AllArgsConstructor(access = PRIVATE)
    public // Yasson requires the POJO to be `public`
    static class Product {
        @Id @NonNull String id;
        String name;
        Integer price;
    }

    @Getter @Setter @ToString
    @Builder(toBuilder = true) @AllArgsConstructor
    static class Item {
        @Id String productId;
    }
}

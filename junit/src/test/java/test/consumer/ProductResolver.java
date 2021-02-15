package test.consumer;

import io.smallrye.graphql.client.typesafe.api.GraphQlClientApi;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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

    @GraphQlClientApi(endpoint = "dummy")
    interface Products {
        Product product(String id);
    }

    @Getter @Setter @ToString @NoArgsConstructor
    @Builder(toBuilder = true) @AllArgsConstructor(access = PRIVATE)
    public // Yasson requires the POJO to be `public`
    static class Product {
        @Id @NonNull String id;
        String name;
    }

    @Getter @Setter @ToString @NoArgsConstructor
    @Builder(toBuilder = true) @AllArgsConstructor(access = PRIVATE)
    static class Item {
        @Id String productId;
    }
}

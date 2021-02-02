package test;

import io.smallrye.graphql.client.typesafe.api.GraphQlClientApi;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Source;

import static lombok.AccessLevel.PRIVATE;

@GraphQLApi
public class ProductResolver {
    Products products;

    public Product product(@Source Item item) {
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
        @Id String id;
        String name;
    }

    @Getter @Setter @ToString @NoArgsConstructor
    @Builder(toBuilder = true) @AllArgsConstructor(access = PRIVATE)
    static class Item {
        @Id String productId;
    }
}

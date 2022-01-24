package test.consumer;

import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.With;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Source;

import static lombok.AccessLevel.PRIVATE;

@GraphQLApi
class ProductResolver {
    // would be @Inject
    Products products;

    // would be @Inject
    NamedProducts namedProducts;

    // would be @Inject
    ProductsGetter productsGetter;

    Product product(@Source Item item) {return products.product(item.getProductId());}

    Product namedProduct(@Source Item item) {return namedProducts.productById(item.getProductId());}

    Product productGetter(@Source Item item) {return productsGetter.getProduct(item.getProductId());}

    Product productWithPriceUpdate(Item item, int newPrice) {
        var patch = new Product().withId(item.getProductId()).withPrice(newPrice);
        return products.patch(patch);
    }

    @GraphQLClientApi(endpoint = "dummy")
    interface Products {
        Product product(@NonNull String id);
        Product patch(@NonNull Product patch);
    }

    @GraphQLClientApi
    interface NamedProducts {
        @Name("p")
        Product productById(String id);
    }

    @GraphQLClientApi
    interface ProductsGetter {
        Product getProduct(String id);
    }

    @Data @NoArgsConstructor
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

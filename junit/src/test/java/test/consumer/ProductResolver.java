package test.consumer;

import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.smallrye.graphql.client.typesafe.api.Header;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.With;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@GraphQLApi
@RequiredArgsConstructor
class ProductResolver {
    // would be @Inject
    Products products;

    // would be @Inject
    NamedProducts namedProducts;

    // would be @Inject
    QueriedProducts queriedProducts;

    // would be @Inject
    ProductsGetter productsGetter;

    Product product(@Source Item item) {return products.product(item.getProductId());}

    Product product(String customHeader, @Source Item item) {return products.product(customHeader, item.getProductId());}

    Product namedProduct(@Source Item item) {return namedProducts.productById(item.getProductId());}

    Product queriedProduct(@Source Item item) {return queriedProducts.productById(item.getProductId());}

    Product productGetter(@Source Item item) {return productsGetter.getProduct(item.getProductId());}

    Product productWithPriceUpdate(Item item, int newPrice) {
        var patch = new Product().withId(item.getProductId()).withPrice(newPrice);
        return products.patch(patch);
    }

    String strings(List<String> strings) {
        return products.strings(strings);
    }

    String products(List<Product> products) {
        return this.products.products(products);
    }

    @GraphQLClientApi(endpoint = "dummy")
    interface Products {
        Product product(@NonNull String id);

        Product product(@Header String customHeader, @NonNull String id);

        Product patch(@NonNull Product patch);

        String strings(@NonNull List<@NonNull String> strings);

        String products(@NonNull List<@NonNull Product> products);
    }

    @GraphQLClientApi
    interface NamedProducts {
        @Name("p")
        Product productById(String id);
    }

    @GraphQLClientApi
    interface QueriedProducts {
        @Query("q")
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

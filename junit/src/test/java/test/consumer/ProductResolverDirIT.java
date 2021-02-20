package test.consumer;

import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.WunderBarConsumerExtension;
import org.junit.jupiter.api.Test;
import test.consumer.ProductResolver.Item;
import test.consumer.ProductResolver.Product;
import test.consumer.ProductResolver.Products;

import java.io.File;
import java.nio.file.Path;

import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static org.assertj.core.api.Assertions.contentOf;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static test.consumer.ProductResolverDirIT.DIR;

@WunderBarConsumerExtension(fileName = DIR)
class ProductResolverDirIT {
    static final String DIR = "target/wunder-bar/";

    @Service Products products;
    @SystemUnderTest ProductResolver resolver;

    @Test void shouldResolveProduct() {
        var givenProduct = Product.builder().id("x").name("some-product-name").build();
        given(products.product(givenProduct.getId())).willReturn(givenProduct);

        var resolvedProduct = resolver.product(Item.builder().productId(givenProduct.getId()).build());

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(givenProduct);
        thenSoftly(softly -> {
            softly.then(contentOf(barFile("request-headers.properties"))).isEqualTo("" +
                "Method: POST\n" +
                "URI: /graphql\n" +
                "Content-Type: application/json;charset=utf-8\n");
            softly.then(contentOf(barFile("request-body.json"))).isEqualTo("" +
                "{\n" +
                "    \"query\": \"query product($id: String!) { product(id: $id) {id name} }\",\n" +
                "    \"variables\": {\n" +
                "        \"id\": \"x\"\n" +
                "    },\n" +
                "    \"operationName\": \"product\"\n" +
                "}\n");
            softly.then(contentOf(barFile("response-headers.properties"))).isEqualTo("" +
                "Status: 200 OK\n" +
                "Content-Type: application/json;charset=utf-8\n");
            softly.then(contentOf(barFile("response-body.json"))).isEqualTo("" +
                "{\n" +
                "    \"data\": {\n" +
                "        \"product\": {\n" +
                "            \"id\": \"x\",\n" +
                "            \"name\": \"some-product-name\"\n" +
                "        }\n" +
                "    }\n" +
                "}\n");
        });
    }

    private File barFile(String file) {
        return Path.of(DIR)
            .resolve(ProductResolverDirIT.class.getSimpleName())
            .resolve("shouldResolveProduct")
            .resolve("1 " + file)
            .toFile();
    }
}

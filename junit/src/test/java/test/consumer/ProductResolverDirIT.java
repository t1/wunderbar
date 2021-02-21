package test.consumer;

import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.WunderBarConsumerExtension;
import org.junit.jupiter.api.Test;
import test.consumer.ProductResolver.Item;
import test.consumer.ProductResolver.Product;
import test.consumer.ProductResolver.Products;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
            softly.then(contentOf(barFile("shouldResolveProduct/1 request-headers.properties"))).isEqualTo("" +
                "Method: POST\n" +
                "URI: /graphql\n" +
                "Content-Type: application/json;charset=utf-8\n");
            softly.then(contentOf(barFile("shouldResolveProduct/1 request-body.json"))).isEqualTo("" +
                "{\n" +
                "    \"query\": \"query product($id: String!) { product(id: $id) {id name} }\",\n" +
                "    \"variables\": {\n" +
                "        \"id\": \"x\"\n" +
                "    },\n" +
                "    \"operationName\": \"product\"\n" +
                "}\n");
            softly.then(contentOf(barFile("shouldResolveProduct/1 response-headers.properties"))).isEqualTo("" +
                "Status: 200 OK\n" +
                "Content-Type: application/json;charset=utf-8\n");
            softly.then(contentOf(barFile("shouldResolveProduct/1 response-body.json"))).isEqualTo("" +
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

    @Test void shouldResolveTwoProducts() throws IOException {
        var givenProduct1 = Product.builder().id("x").name("some-product-x").build();
        var givenProduct2 = Product.builder().id("y").name("some-product-y").build();
        given(products.product(givenProduct1.getId())).willReturn(givenProduct1);
        given(products.product(givenProduct2.getId())).willReturn(givenProduct2);

        var resolvedProduct1 = resolver.product(Item.builder().productId(givenProduct1.getId()).build());
        var resolvedProduct2 = resolver.product(Item.builder().productId(givenProduct2.getId()).build());

        then(resolvedProduct1).usingRecursiveComparison().isEqualTo(givenProduct1);
        then(resolvedProduct2).usingRecursiveComparison().isEqualTo(givenProduct2);
        then(Files.list(baseDir().resolve("shouldResolveTwoProducts"))
            .map(path -> path.subpath(3, 5)).map(Path::toString))
            .containsExactlyInAnyOrder(
                "shouldResolveTwoProducts/1 request-headers.properties",
                "shouldResolveTwoProducts/2 response-headers.properties",
                "shouldResolveTwoProducts/1 request-body.json",
                "shouldResolveTwoProducts/1 response-headers.properties",
                "shouldResolveTwoProducts/2 response-body.json",
                "shouldResolveTwoProducts/2 request-body.json",
                "shouldResolveTwoProducts/1 response-body.json",
                "shouldResolveTwoProducts/2 request-headers.properties");
    }

    private File barFile(String file) {
        return baseDir().resolve(file).toFile();
    }

    private Path baseDir() {
        return Path.of(DIR).resolve(ProductResolverDirIT.class.getSimpleName());
    }
}

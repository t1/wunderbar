package test.consumer;

import com.github.t1.wunderbar.junit.Register;
import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.Some;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import org.junit.jupiter.api.Test;
import test.consumer.ProductResolver.Item;
import test.consumer.ProductResolver.Product;
import test.consumer.ProductResolver.Products;

import javax.json.JsonValue;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static com.github.t1.wunderbar.junit.http.HttpUtils.APPLICATION_JSON_UTF8;
import static com.github.t1.wunderbar.junit.http.HttpUtils.readJson;
import static org.assertj.core.api.Assertions.contentOf;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDSoftAssertions.thenSoftly;
import static test.consumer.ProductResolverDirIT.DIR;

@WunderBarApiConsumer(fileName = DIR)
@Register(SomeProduct.class)
class ProductResolverDirIT {
    static final String DIR = "target/wunder-bar/";

    @Service Products products;
    @SystemUnderTest ProductResolver resolver;

    @Test void shouldResolveProduct(@Some Product product) {
        given(products.product(product.getId())).returns(product);

        var resolvedProduct = resolver.product(Item.builder().productId(product.getId()).build());

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product);
        var prefix = "shouldResolveProduct(Product)/";
        thenSoftly(softly -> {
            softly.then(contentOf(barFile(prefix + "1 request-headers.properties"))).isEqualTo(
                "Method: POST\n" +
                "URI: /graphql\n" +
                "Accept: " + APPLICATION_JSON_UTF8 + "\n" +
                "Content-Type: " + APPLICATION_JSON_UTF8 + "\n");
            softly.then(jsonFile(prefix + "1 request-body.json")).isEqualTo(readJson(
                "{\n" +
                "    \"query\": \"query product($id: String!) { product(id: $id) {id name price} }\",\n" +
                "    \"variables\": {\n" +
                "        \"id\": \"" + product.id + "\"\n" +
                "    },\n" +
                "    \"operationName\": \"product\"\n" +
                "}\n"));
            softly.then(jsonFile(prefix + "1 request-variables.json")).isEqualTo(readJson(
                "{" +
                "    \"/variables/id\": {" +
                "        \"type\": \"java.lang.String\"," +
                "        \"value\": \"" + product.id + "\"," +
                "        \"location\": \"parameter [product] @method ProductResolverDirIT#shouldResolveProduct\"," +
                "        \"some\": {" +
                "            \"tags\": [\"id\"]" +
                "        }" +
                "    }" +
                "}"));
            softly.then(contentOf(barFile(prefix + "1 response-headers.properties"))).isEqualTo(
                "Status: 200 OK\n" +
                "Content-Type: application/json;charset=utf-8\n");
            softly.then(jsonFile(prefix + "1 response-body.json")).isEqualTo(readJson(
                "{" +
                "    \"data\": {" +
                "        \"product\": {" +
                "            \"id\": \"" + product.id + "\"," +
                "            \"name\": \"" + product.name + "\"," +
                "            \"price\": " + product.price + "" +
                "        }" +
                "    }" +
                "}"));
        });
    }

    @Test void shouldResolveTwoProducts(@Some Product product1, @Some Product product2) throws IOException {
        given(products.product(product1.getId())).returns(product1);
        given(products.product(product2.getId())).returns(product2);

        var resolvedProduct1 = resolver.product(Item.builder().productId(product1.getId()).build());
        var resolvedProduct2 = resolver.product(Item.builder().productId(product2.getId()).build());

        then(resolvedProduct1).usingRecursiveComparison().isEqualTo(product1);
        then(resolvedProduct2).usingRecursiveComparison().isEqualTo(product2);
        then(Files.list(baseDir().resolve("shouldResolveTwoProducts(Product, Product)"))
            .map(path -> path.subpath(4, 5)).map(Path::toString))
            .containsExactlyInAnyOrder(
                "1 request-headers.properties",
                "1 request-body.json",
                "1 request-variables.json",
                "1 response-headers.properties",
                "1 response-body.json",
                "1 response-variables.json",
                "2 request-headers.properties",
                "2 request-body.json",
                "2 request-variables.json",
                "2 response-headers.properties",
                "2 response-body.json",
                "2 response-variables.json");
    }

    private JsonValue jsonFile(String fileName) {return readJson(contentOf(barFile(fileName)));}

    private File barFile(String file) {
        return baseDir().resolve(file).toFile();
    }

    private Path baseDir() {
        return Path.of(DIR).resolve(ProductResolverDirIT.class.getSimpleName());
    }
}

package test.consumer;

import com.github.t1.wunderbar.junit.Register;
import com.github.t1.wunderbar.junit.consumer.*;
import jakarta.json.JsonValue;
import org.eclipse.microprofile.graphql.NonNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import test.consumer.ProductResolver.Item;
import test.consumer.ProductResolver.Product;
import test.consumer.ProductResolver.Products;
import test.consumer.ProductsGateway.ProductsRestClient;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static com.github.t1.wunderbar.junit.assertions.WunderBarBDDAssertions.then;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static com.github.t1.wunderbar.junit.http.HttpUtils.*;
import static org.assertj.core.api.Assertions.contentOf;
import static test.consumer.ProductResolverDirIT.DIR;

@WunderBarApiConsumer(fileName = DIR)
@Register(SomeProducts.class)
@SuppressWarnings("JUnitMalformedDeclaration")
class ProductResolverDirIT {
    static final String DIR = "target/wunder-bar/";

    @Service Products products;
    @SystemUnderTest ProductResolver resolver;

    @Test void shouldResolveProduct(@Some Product product, @Some("header") String customHeader) {
        given(products.product(customHeader, product.getId())).returns(product);

        var resolvedProduct = resolver.product(customHeader, item(product.getId()));

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product);
        var prefix = "shouldResolveProduct(Product, String)/";
        var methodName = ProductResolverDirIT.class.getName() + "#shouldResolveProduct";
        then(propertiesFile(prefix + "1 request-headers.properties"))
                .containsEntry("Method", "POST")
                .containsEntry("URI", "/graphql")
                .containsEntry("Accept", APPLICATION_JSON_UTF8.toString())
                .containsEntry("Content-Type", APPLICATION_JSON_UTF8.toString());
        then(jsonFile(prefix + "1 request-body.json")).isEqualTo(readJson(
                "{\n" +
                        "    \"query\": \"query product($id: String!) { product(id: $id) {id name price} }\",\n" +
                        "    \"variables\": {\n" +
                        "        \"id\": \"" + product.id + "\"\n" +
                        "    },\n" +
                        "    \"operationName\": \"product\"\n" +
                        "}\n"));
        then(jsonFile(prefix + "1 request-variables.json")).isEqualTo(readJson(
                "{" +
                        "    \":Custom-Header\": {" +
                        "        \"some\": {\"tags\": [\"header\"]}," +
                        "        \"type\": \"java.lang.String\"," +
                        "        \"location\": \"parameter [customHeader] @method " + methodName + "\"," +
                        "        \"value\": \"" + customHeader + "\"" +
                        "    }," +
                        "    \"/variables/id\": {" +
                        "        \"some\": {\"tags\": [\"id\"]}," +
                        "        \"type\": \"java.lang.String\"," +
                        "        \"location\": \"parameter [product] @method " + methodName + "\"," +
                        "        \"value\": \"" + product.id + "\"" +
                        "    }" +
                        "}"));
        then(propertiesFile(prefix + "1 response-headers.properties"))
                .containsEntry("Status", "200 OK")
                .containsEntry("Content-Type", APPLICATION_JSON_UTF8.toString());
        then(jsonFile(prefix + "1 response-body.json")).isEqualTo(readJson(
                "{" +
                        "    \"data\": {" +
                        "        \"product\": {" +
                        "            \"id\": \"" + product.id + "\"," +
                        "            \"name\": \"" + product.name + "\"," +
                        "            \"price\": " + product.price +
                        "        }" +
                        "    }" +
                        "}"));
        then(jsonFile(prefix + "1 response-variables.json")).isEqualTo(readJson(
                "{" +
                        "    \"/data/product\": {" +
                        "        \"some\": {}," +
                        "        \"type\": \"test.consumer.ProductResolver$Product\"," +
                        "        \"location\": \"parameter [product] @method " + methodName + "\"," +
                        "        \"value\": {\"id\": \"product-id-00100\", \"name\": \"test product product-id-00100\", \"price\": 101}" +
                        "    }," +
                        "    \"/data/product/id\": {" +
                        "        \"some\": {\"tags\": [\"id\"]}," +
                        "        \"type\": \"java.lang.String\"," +
                        "        \"location\": \"parameter [product] @method " + methodName + "\"," +
                        "        \"value\": \"product-id-00100\"" +
                        "    }," +
                        "    \"/data/product/price\": {" +
                        "        \"type\": \"java.lang.Integer\"," +
                        "        \"location\": \"field [price] @class " + Product.class.getName() + "\"," +
                        "        \"value\": 101" +
                        "    }" +
                        "}"));
    }

    @Test void shouldReplaceNumericHeader(@Some Product product, @Some int someInt) {
        given(products.product("x" + someInt, product.getId())).returns(product);

        var resolvedProduct = resolver.product("x" + someInt, item(product.getId()));

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product);
        then(jsonFile("shouldReplaceNumericHeader(Product, int)/1 request-variables.json")).isEqualToIgnoringNewFields(readJson(
                "{" +
                        "    \":Custom-Header:x{}\": {" +
                        "        \"value\": 102" +
                        "    }," +
                        "    \"/variables/id\": {" +
                        "        \"value\": \"" + product.id + "\"" +
                        "    }" +
                        "}"));
    }

    @Test void shouldNotReplaceNumericHeaderWithNumericPrefix(@Some Product product, @Some int someInt) {
        given(products.product("1" + someInt, product.getId())).returns(product);

        var resolvedProduct = resolver.product("1" + someInt, item(product.getId()));

        then(resolvedProduct).usingRecursiveComparison().isEqualTo(product);
        var prefix = "shouldNotReplaceNumericHeaderWithNumericPrefix(Product, int)/";
        var methodName = ProductResolverDirIT.class.getName() + "#shouldNotReplaceNumericHeaderWithNumericPrefix";
        then(jsonFile(prefix + "1 request-variables.json")).isEqualTo(readJson(
                "{" +
                        "    \"/variables/id\": {" +
                        "        \"some\": {\"tags\": [\"id\"]}," +
                        "        \"type\": \"java.lang.String\"," +
                        "        \"location\": \"parameter [product] @method " + methodName + "\"," +
                        "        \"value\": \"" + product.id + "\"" +
                        "    }" +
                        "}"));
    }

    @Test void shouldResolveTwoProducts(@Some Product product1, @Some Product product2) throws IOException {
        given(products.product(product1.getId())).returns(product1);
        given(products.product(product2.getId())).returns(product2);

        var resolvedProduct1 = resolver.product(item(product1.getId()));
        var resolvedProduct2 = resolver.product(item(product2.getId()));

        then(resolvedProduct1).usingRecursiveComparison().isEqualTo(product1);
        then(resolvedProduct2).usingRecursiveComparison().isEqualTo(product2);
        try (var files = Files.list(baseDir().resolve("shouldResolveTwoProducts(Product, Product)"))) {
            then(files.map(path -> path.subpath(4, 5)).map(Path::toString))
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
    }

    @Nested class Rest {
        @Service ProductsRestClient restService;
        @SystemUnderTest ProductsGateway gateway;

        @Register(SomeVendorContentTypeName.class)
        @Test
        void shouldGetProduct(@Some Product product, @Some("header") String customHeader, @Some("vendor") String vendor) {
            given(restService.product(customHeader, product.id)).returns(product);

            var response = gateway.product(customHeader, item(product.getId()));

            then(response).usingRecursiveComparison().isEqualTo(product);
            var prefix = "Rest/shouldGetProduct(Product, String, String)/";
            then(propertiesFile(prefix + "1 request-headers.properties")).as("request headers")
                    .containsEntry("Method", "GET")
                    .containsEntry("URI", "/rest/products/product-id-00100")
                    .containsEntry("Accept", "application/" + vendor + "+json;charset=utf-8")
                    .containsEntry("Content-Type", APPLICATION_JSON_UTF8.toString())
                    .containsEntry("Custom-Header", customHeader);
            then(jsonFile(prefix + "1 request-variables.json")).as("request variables").isEqualToIgnoringNewFields(readJson(
                    "{" +
                            "    \":URI:/rest/products/{}\": {" +
                            "        \"value\": \"" + product.id + "\"" +
                            "    }," +
                            "    \":Custom-Header\": {" +
                            "        \"some\": {\"tags\": [\"header\"]}," +
                            "        \"value\": \"" + customHeader + "\"" +
                            "    }" +
                            "}"));
            then(propertiesFile(prefix + "1 response-headers.properties")).as("response headers")
                    .containsEntry("Status", "200 OK")
                    .containsEntry("Content-Type", "application/" + vendor + "+json;charset=utf-8");
            then(jsonFile(prefix + "1 response-body.json")).as("response body").isEqualTo(readJson(
                    "{" +
                            "    \"id\": \"" + product.id + "\"," +
                            "    \"name\": \"" + product.name + "\"," +
                            "    \"price\": " + product.price +
                            "}"));
            then(jsonFile(prefix + "1 response-variables.json")).as("response variables").isEqualToIgnoringNewFields(readJson(
                    "{" +
                            "    \":Content-Type:application/{}+json;charset=utf-8\": {" +
                            "        \"some\": {\"tags\": [\"vendor\"]}," +
                            "        \"value\": \"" + vendor + "\"" +
                            "    }," +
                            "    \"\": {" +
                            "        \"value\": {\"id\": \"product-id-00100\", \"name\": \"test product product-id-00100\", \"price\": 101}" +
                            "    }," +
                            "    \"/id\": {" +
                            "        \"value\": \"product-id-00100\"" +
                            "    }," +
                            "    \"/price\": {" +
                            "        \"value\": 101" +
                            "    }" +
                            "}"));
        }

        static class SomeVendorContentTypeName extends SomeSingleTypes<@Some("vendor") String> {
            @Override public String generate(Some some, Type type, AnnotatedElement location) {return "vnd.product";}
        }
    }

    private Item item(@NonNull String productId) {return Item.builder().productId(productId).build();}

    private JsonValue jsonFile(String fileName) {return readJson(contentOf(barFile(fileName)));}

    private Properties propertiesFile(String fileName) {return properties(contentOf(barFile(fileName)));}

    private File barFile(String file) {return baseDir().resolve(file).toFile();}

    private Path baseDir() {
        return Path.of(DIR).resolve(ProductResolverDirIT.class.getSimpleName());
    }
}

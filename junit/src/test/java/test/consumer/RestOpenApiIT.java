package test.consumer;

import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.SystemUnderTest;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import org.junit.jupiter.api.Test;
import test.consumer.ProductResolver.Item;
import test.consumer.ProductResolver.Product;
import test.consumer.ProductsGateway.ProductsRestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.t1.wunderbar.http.HttpUtils.readJson;
import static com.github.t1.wunderbar.junit.ContractFormat.OPENAPI;
import static com.github.t1.wunderbar.junit.assertions.WunderBarBDDAssertions.then;
import static com.github.t1.wunderbar.junit.consumer.Level.INTEGRATION;
import static com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer.Output;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;

@WunderBarApiConsumer(level = INTEGRATION, output = {
        @Output(fileName = RestOpenApiIT.BAR_FILE),
        @Output(format = OPENAPI, fileName = RestOpenApiIT.OPEN_API_FILE)
})
class RestOpenApiIT {
    static final String BAR_FILE = "target/rest-openapi.bar";
    static final String OPEN_API_FILE = "target/rest-openapi.json";

    @Service ProductsRestClient restService;

    @SystemUnderTest ProductsGateway gateway;

    @Test void shouldWriteOpenApiFile() throws IOException {
        var product = Product.builder().id("open-api-product-id").name("some-product-name").price(42).build();
        given(restService.product(product.id)).returns(product);

        var response = gateway.product(new Item(product.id));

        then(response).usingRecursiveComparison().isEqualTo(product);
        then(Path.of(BAR_FILE)).exists();

        var openApi = readJson(Files.readString(Path.of(OPEN_API_FILE))).asJsonObject();
        then(openApi.getString("openapi")).isEqualTo("3.1.0");
        then(openApi.getJsonObject("paths").getJsonObject("/rest/products/open-api-product-id").containsKey("get")).isTrue();
        then(openApi.containsKey("x-wunderbar-tests")).isFalse();
    }
}

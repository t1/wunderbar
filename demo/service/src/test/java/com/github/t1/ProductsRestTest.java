package com.github.t1;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class ProductsRestTest {
    @Test void shouldGetProduct() {
        given()

            .when().get("/products/foo")

            .then()
            .statusCode(200)
            .body(is("{\"id\":\"foo\",\"name\":\"product foo\"}"));
    }
}

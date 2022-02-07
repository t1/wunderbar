package test.consumer;

import test.consumer.ProductResolver.Product;

import static com.github.t1.wunderbar.junit.consumer.TestData.someInt;

public class TestData {
    public static Product someProduct() {
        var id = someInt();
        return Product.builder().id("#" + id).name("product " + id).price(1000 + id).build();
    }
}

package test.consumer;

import test.consumer.ProductResolver.Product;

import java.util.Random;

public class TestData {
    private static int nextInt = new Random().nextInt(9); // just a bit of initial random

    public static Product someProduct() {
        var id = nextInt++;
        return Product.builder().id("#" + id).name("product " + id).price(1000 + id).build();
    }
}

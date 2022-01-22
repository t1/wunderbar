package test.consumer;

import test.consumer.ProductResolver.Product;

import java.util.Random;

public class TestData {
    private static int nextInt = Math.abs(new Random().nextInt(9)); // just a bit of initial randomness

    public static Product someProduct() {
        var id = someInt();
        return Product.builder().id("#" + id).name("product " + id).price(1000 + id).build();
    }

    public static String someId() {return "id-" + someInt();}

    public static int someInt() {return nextInt++;}
}

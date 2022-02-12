package test.consumer;

import com.github.t1.wunderbar.junit.consumer.Some;
import com.github.t1.wunderbar.junit.consumer.SomeGenerator;
import com.github.t1.wunderbar.junit.consumer.SomeSingleTypeData;
import lombok.RequiredArgsConstructor;
import test.consumer.ProductResolver.Product;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

@RequiredArgsConstructor
public class SomeProduct extends SomeSingleTypeData<Product> {
    private final SomeGenerator generator;

    @Override public Product some(Some some, Type type, AnnotatedElement location) {
        int id = (int) generator.generate(some, int.class, location);
        return Product.builder().id("#" + id).name("product " + id).price(id).build();
    }
}

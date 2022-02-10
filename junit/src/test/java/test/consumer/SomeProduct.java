package test.consumer;

import com.github.t1.wunderbar.junit.consumer.SomeData;
import com.github.t1.wunderbar.junit.consumer.SomeGenerator;
import lombok.RequiredArgsConstructor;
import test.consumer.ProductResolver.Product;

import java.lang.reflect.Type;

@RequiredArgsConstructor
public class SomeProduct implements SomeData {
    private final SomeGenerator generator;

    @Override public boolean canGenerate(Type type) {
        return Product.class.equals(type);
    }

    @SuppressWarnings("unchecked")
    @Override public Product some(Type type) {
        int id = generator.generate(int.class, "some product");
        return Product.builder().id("#" + id).name("product " + id).price(id).build();
    }
}

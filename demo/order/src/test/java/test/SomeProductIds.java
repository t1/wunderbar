package test;

import com.github.t1.wunderbar.junit.consumer.Some;
import com.github.t1.wunderbar.junit.consumer.SomeGenerator;
import com.github.t1.wunderbar.junit.consumer.SomeSingleTypes;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

@RequiredArgsConstructor
public class SomeProductIds extends SomeSingleTypes<@Some("product-id") String> {
    private final SomeGenerator generator;

    @Override public String generate(Some some, Type type, AnnotatedElement location) {
        return "id-" + generator.generate(some, int.class, location);
    }
}

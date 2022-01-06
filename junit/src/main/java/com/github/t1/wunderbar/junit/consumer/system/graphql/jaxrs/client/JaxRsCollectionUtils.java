package com.github.t1.wunderbar.junit.consumer.system.graphql.jaxrs.client;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.util.Collections.singleton;
import static java.util.stream.Collector.Characteristics.IDENTITY_FINISH;

public final class JaxRsCollectionUtils {
    private JaxRsCollectionUtils() {}

    public static <K, V> Collector<Entry<K, V>, MultivaluedMap<K, V>, MultivaluedMap<K, V>> toMultivaluedMap() {
        return new Collector<>() {
            @Override
            public Supplier<MultivaluedMap<K, V>> supplier() {
                return MultivaluedHashMap::new;
            }

            @Override
            public BiConsumer<MultivaluedMap<K, V>, Entry<K, V>> accumulator() {
                return (map, entry) -> map.add(entry.getKey(), entry.getValue());
            }

            @Override
            public BinaryOperator<MultivaluedMap<K, V>> combiner() {
                return (a, b) -> {
                    a.putAll(b);
                    return a;
                };
            }

            @Override
            public Function<MultivaluedMap<K, V>, MultivaluedMap<K, V>> finisher() {
                return Function.identity();
            }

            @Override
            public Set<Characteristics> characteristics() {
                return singleton(IDENTITY_FINISH);
            }
        };
    }
}

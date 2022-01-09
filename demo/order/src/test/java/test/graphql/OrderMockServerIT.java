package test.graphql;

import com.github.t1.testcontainers.jee.JeeContainer;
import com.github.t1.testcontainers.jee.WildflyContainer;
import com.github.t1.wunderbar.junit.consumer.Service;
import com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.smallrye.graphql.client.typesafe.api.TypesafeGraphQLClientBuilder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import test.Slow;

import java.util.List;

import static com.github.t1.wunderbar.junit.consumer.Level.SYSTEM;
import static com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer.NONE;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.createService;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static org.assertj.core.api.BDDAssertions.then;
import static org.testcontainers.containers.Network.newNetwork;

@Slow
@Testcontainers
@WunderBarApiConsumer(level = SYSTEM, fileName = NONE)
class OrderMockServerIT {
    static final Network NETWORK = newNetwork();

    @Container static JeeContainer ORDERS = new WildflyContainer("rdohna/wildfly", "25.0.1.Final-jdk11")
        .withNetwork(NETWORK)
        .withDeployment("target/order.war");

    @Container static JeeContainer PRODUCTS = new WildflyContainer("rdohna/wildfly", "25.0.1.Final-jdk11")
        .withNetwork(NETWORK)
        .withNetworkAliases("products")
        .withDeployment("../../mock/war/target/wunderbar-mock-server.war");

    @SuppressWarnings("unused")
    static String productsEndpoint() {
        return "http://localhost:" + PRODUCTS.getMappedPort(8080) + "/wunderbar-mock-server/graphql";
    }

    interface Api {
        Order order(@NonNull String id);
    }

    @Data @SuperBuilder @NoArgsConstructor
    public static class Order {
        String id;
        String orderDate;
        @Singular List<OrderItem> items;
    }

    @Data @SuperBuilder @NoArgsConstructor
    public static class OrderItem {
        int position;
        String productId;
        Product product;
    }

    @Data @SuperBuilder @NoArgsConstructor
    public static class Product {
        String id;
        String name;
        String description;
        Integer price;
    }

    @GraphQLClientApi
    public interface Products {
        @Query Product product(@NonNull String id);
    }

    Api api;

    @BeforeEach
    void setUp() {
        var graphQlEndpoint = ORDERS.baseUri() + "graphql";
        this.api = TypesafeGraphQLClientBuilder.newBuilder().endpoint(graphQlEndpoint).build(Api.class);
    }

    @Test void shouldGetOrder() {
        Product givenProduct = Product.builder()
            .id(PRODUCT_ID)
            .name("some-product-name")
            .price(1599)
            .build();
        var products = createService(Products.class, Service.DEFAULT.withEndpoint("{productsEndpoint()}"));
        given(products.product(PRODUCT_ID)).willReturn(givenProduct);

        var order = api.order("1");

        then(order).isEqualTo(Order.builder()
            .id("1")
            .orderDate("2021-12-31")
            .item(OrderItem.builder()
                .position(1)
                .productId(PRODUCT_ID)
                .product(givenProduct)
                .build())
            .item(OrderItem.builder()
                .position(2)
                .productId(PRODUCT_ID)
                .product(givenProduct)
                .build())
            .build());
    }

    private static final String PRODUCT_ID = "existing-product-id";
}

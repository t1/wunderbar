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

import static com.github.t1.wunderbar.demo.order.Orders.PRODUCT_ID;
import static com.github.t1.wunderbar.junit.consumer.WunderBarApiConsumer.NONE;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.createService;
import static com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder.given;
import static org.assertj.core.api.BDDAssertions.then;
import static org.testcontainers.containers.Network.newNetwork;

@Slow
@Testcontainers
@WunderBarApiConsumer(fileName = NONE)
class OrderMockST {
    private static final Network NETWORK = newNetwork();

    @Container static JeeContainer ORDERS = jeeContainer()
        .withMainPortBoundToFixedPort(18080)
        .withPortBoundToFixedPort(19990, 9990)
        .withPortBoundToFixedPort(18787, 8787)
        .withDeployment("target/order.war");

    @Container static JeeContainer PRODUCTS = jeeContainer()
        .withMainPortBoundToFixedPort(28080)
        .withPortBoundToFixedPort(29990, 9990)
        .withPortBoundToFixedPort(28787, 8787)
        .withNetworkAliases("products")
        .withDeployment("../../mock/target/wunderbar-mock-server.war");

    private static JeeContainer jeeContainer() {
        return new WildflyContainer("rdohna/wildfly", "25.0.1.Final-jdk11").withNetwork(NETWORK);
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

    Products products;
    Api api;

    @BeforeEach
    void setUp() {
        this.products = createService(Products.class, Service.DEFAULT
            .withEndpoint("http://localhost:" + PRODUCTS.getMappedPort(8080) + "/wunderbar-mock-server/graphql"));
        this.api = TypesafeGraphQLClientBuilder.newBuilder().endpoint(ORDERS.baseUri() + "graphql").build(Api.class);
    }

    Product product = Product.builder()
        .id(PRODUCT_ID)
        .name("product #" + PRODUCT_ID)
        .price(1599)
        .build();

    @Test void shouldGetOrder() {
        given(products.product(PRODUCT_ID)).returns(product);

        var order = api.order("1");

        then(order).isEqualTo(Order.builder()
            .id("1")
            .orderDate("2021-12-31")
            .item(OrderItem.builder()
                .position(1)
                .productId(PRODUCT_ID)
                .product(product)
                .build())
            .item(OrderItem.builder()
                .position(2)
                .productId(PRODUCT_ID)
                .product(product)
                .build())
            .build());
    }
}

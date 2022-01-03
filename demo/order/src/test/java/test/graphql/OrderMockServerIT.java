package test.graphql;

import com.github.t1.testcontainers.jee.JeeContainer;
import com.github.t1.testcontainers.jee.WildflyContainer;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.smallrye.graphql.client.typesafe.api.TypesafeGraphQLClientBuilder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.testcontainers.containers.Network.newNetwork;

@Disabled("WIP") // TODO develop and use mock server
@Testcontainers
class OrderMockServerIT {
    static final Network NETWORK = newNetwork();

    @Container static JeeContainer ORDERS = new WildflyContainer()//"rdohna/wildfly", null)
        .withNetwork(NETWORK)
        .withDeployment("target/order.war");

    @SuppressWarnings("rawtypes")
    @Container static GenericContainer PRODUCTS = new GenericContainer("rdohna/wunderbar.demo.product")
        .withNetwork(NETWORK)
        .withNetworkAliases("products")
        .withExposedPorts(8081)
        .waitingFor(new HttpWaitStrategy().forPort(8081).forPath("/q/health/ready"));

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

    @GraphQLClientApi(configKey = "products")
    public interface Products {
        @Query Product product(@NonNull String id);
    }

    // Products products;
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
        // given(products.product(PRODUCT_ID)).willReturn(givenProduct);

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

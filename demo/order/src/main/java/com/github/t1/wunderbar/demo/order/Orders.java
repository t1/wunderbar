package com.github.t1.wunderbar.demo.order;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@GraphQLApi
@Path("/orders")
public class Orders {
    private final AtomicInteger nextOrderId = new AtomicInteger(100);
    private static final List<Order> ORDERS = new ArrayList<>();

    @GET @Path("/{id}")
    @Query public Order order(@NonNull @PathParam("id") String id) {
        log.debug("get order with id {}", id);
        var order = ORDERS.stream().filter(i -> i.id.equals(id))
                .findFirst().orElseThrow(() -> new NotFoundException("no order with id " + id));
        log.debug("found order {}", order);
        return order;
    }

    @Mutation public Order create(@NonNull Order order) {
        log.debug("create {}", order);
        if (order.id != null) throw new IllegalArgumentException("the order system creates order ids!");
        order.id = "o" + nextOrderId.getAndIncrement();
        ORDERS.add(order);
        log.debug("created {}", order);
        return order;
    }
}

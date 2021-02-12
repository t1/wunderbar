package com.github.t1.wunderbar.demo.product;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@GraphQLApi @Path("/products")
@Slf4j @SuppressWarnings("QsUndeclaredPathMimeTypesInspection")
public class Products {
    private static final ConcurrentMap<String, Product> PRODUCTS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Product> FORBIDDEN_PRODUCTS = new ConcurrentHashMap<>();

    @Query
    @GET
    public @NonNull Collection<@NonNull Product> all() {
        log.info("all");
        return PRODUCTS.values();
    }

    @Query
    @GET @Path("/{id}")
    public @NonNull Product product(@NonNull @PathParam("id") String id) {
        log.info("product({})", id);
        if (FORBIDDEN_PRODUCTS.containsKey(id)) throw new ProductForbiddenException(id);
        var product = PRODUCTS.get(id);
        if (product == null) throw new ProductNotFoundException(id);
        return product;
    }

    @Mutation public @NonNull Product store(@NonNull Product product) {
        log.info("store({})", product);
        PRODUCTS.put(product.getId(), product);
        return product;
    }

    @Mutation public @NonNull Product forbid(@NonNull String productId) {
        log.info("forbid({})", productId);
        var product = PRODUCTS.get(productId);
        if (product == null) throw new BadRequestException("can't forbid unknown product " + productId);
        FORBIDDEN_PRODUCTS.put(productId, product);
        return product;
    }

    @Mutation public Product delete(@NonNull String productId) {
        log.info("delete({})", productId);
        FORBIDDEN_PRODUCTS.remove(productId);
        return PRODUCTS.remove(productId);
    }

    @SuppressWarnings("CdiInjectionPointsInspection")
    static class ProductNotFoundException extends BusinessException {
        ProductNotFoundException(String productId) { super(new NotFoundException("product " + productId + " not found")); }
    }

    @SuppressWarnings("CdiInjectionPointsInspection")
    static class ProductForbiddenException extends BusinessException {
        ProductForbiddenException(String productId) { super(new ForbiddenException("product " + productId + " is forbidden")); }
    }

    @SuppressWarnings("CdiInjectionPointsInspection")
    static class BusinessException extends WebApplicationException {
        public BusinessException(WebApplicationException template) {
            super(template.getMessage(), response(template));
        }

        private static Response response(WebApplicationException type) {
            var status = type.getResponse().getStatus();
            return Response.status(status).type("application/problem+json").entity("" +
                "{\n" +
                "    \"detail\": \"HTTP " + status + " " + Status.fromStatusCode(status) + "\",\n" +
                "    \"title\": \"" + type.getClass().getSimpleName() + "\",\n" +
                "    \"type\": \"urn:problem-type:" + type.getClass().getName() + "\"\n" +
                "}\n").build();
        }
    }
}

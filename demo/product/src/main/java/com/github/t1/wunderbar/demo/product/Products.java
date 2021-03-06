package com.github.t1.wunderbar.demo.product;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PATCH;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.security.Principal;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Locale.US;

@GraphQLApi @Path("/products")
@Slf4j @SuppressWarnings("QsUndeclaredPathMimeTypesInspection")
public class Products {
    private static final ConcurrentMap<String, Product> PRODUCTS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Product> FORBIDDEN_PRODUCTS = new ConcurrentHashMap<>();

    /* this predefined data is required for the System Tests */
    private static final Product EXISTING_PRODUCT = Product.builder().id("existing-product-id").name("some-product-name").price(15_99).build();
    private static final Product FORBIDDEN_PRODUCT = Product.builder().id("forbidden-product-id").name("some-product-name").build();

    static {
        PRODUCTS.put(EXISTING_PRODUCT.id, EXISTING_PRODUCT);
        FORBIDDEN_PRODUCTS.put(FORBIDDEN_PRODUCT.id, FORBIDDEN_PRODUCT);
    }

    @Inject Principal principal;

    @GET
    @Query public @NonNull Collection<@NonNull Product> all() {
        log.info("all");
        return PRODUCTS.values();
    }

    @GET @Path("/{id}")
    @Query public @NonNull Product product(@NonNull @PathParam("id") String id) {
        log.info("product({})", id);
        if (FORBIDDEN_PRODUCTS.containsKey(id)) throw new ProductForbiddenException(id);
        var product = PRODUCTS.get(id);
        if (product == null) throw new ProductNotFoundException(id);
        log.info("-> {}", product);
        return product;
    }

    @Query public boolean exists(@NonNull String id) {
        log.info("exists({})", id);
        var exists = PRODUCTS.containsKey(id);
        log.info("-> {}", exists);
        return exists;
    }

    @RolesAllowed("Writer")
    @Mutation public @NonNull Product store(@NonNull Product product) {
        log.info("store({})", product);
        PRODUCTS.put(product.id, product);
        log.info("-> {}", product);
        return product;
    }

    @RolesAllowed("Writer")
    @PATCH
    @Mutation public @NonNull Product update(@NonNull Product patch) {
        log.info("update({})", patch);
        var existing = product(patch.id);
        var patched = existing.apply(patch);
        log.info("-> {}", patched);
        return patched;
    }

    @RolesAllowed("Writer")
    @Mutation public @NonNull Product forbid(@NonNull String productId) {
        log.info("forbid({}) by {}", productId, principal.getName());
        var product = PRODUCTS.get(productId);
        if (product == null) throw new BadRequestException("can't forbid unknown product " + productId);
        FORBIDDEN_PRODUCTS.put(productId, product);
        log.info("-> {}", product);
        return product;
    }

    @RolesAllowed("Writer")
    @Mutation public Product delete(@NonNull String productId) {
        log.info("delete({})", productId);
        FORBIDDEN_PRODUCTS.remove(productId);
        var removed = PRODUCTS.remove(productId);
        log.info("-> {}", removed);
        return removed;
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
                "    \"type\": \"urn:problem-type:" + errorCode(type) + "\"\n" +
                "}\n").build();
        }
    }

    public static String errorCode(Exception exception) {
        var code = camelToKebab(exception.getClass().getSimpleName());
        if (code.endsWith("-exception")) code = code.substring(0, code.length() - 10);
        return code;
    }

    private static String camelToKebab(String in) { return String.join("-", in.split("(?=\\p{javaUpperCase})")).toLowerCase(US); }
}

package com.github.t1.wunderbar.demo.product;

import com.github.t1.wunderbar.junit.http.ProblemDetails;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;

import java.security.Principal;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.Boolean.TRUE;

@GraphQLApi
@Path("/products")
@Slf4j
public class Products {
    private static int nextId = new Random().nextInt(100); // just a little randomness

    private static final ConcurrentMap<String, Product> PRODUCTS = new ConcurrentHashMap<>();

    @Inject Principal principal;

    @PostConstruct void postConstruct() {log.info("next product id is {}", nextId);}

    @GET
    @Query public @NonNull Collection<@NonNull Product> all() {
        log.info("all");
        return PRODUCTS.values();
    }

    @GET @Path("/{id}")
    @Query public @NonNull Product product(@NonNull @PathParam("id") String id) {
        log.info("product({})", id);
        var product = PRODUCTS.get(id);
        if (product == null) throw new ProductNotFoundException(id);
        if (product.forbidden == TRUE) throw new ProductForbiddenException(id);
        log.info("-> {}", product);
        return product;
    }

    @Query public Product maybeProduct(@NonNull String id) {
        log.info("maybeProduct({})", id);
        var maybeExisting = PRODUCTS.get(id);
        log.info("-> {}", maybeExisting);
        return maybeExisting;
    }

    @RolesAllowed("Writer")
    @Mutation public @NonNull Product store(@NonNull Product product) {
        log.info("store({}) by {}", product, principal.getName());
        if (product.id == null) product.id = "id-" + nextId++;
        PRODUCTS.put(product.id, product);
        log.info("-> {}", product);
        return product;
    }

    @RolesAllowed("Writer")
    @PATCH
    @Mutation public @NonNull Product update(@NonNull Product patch) {
        log.info("update({}) by {}", patch, principal.getName());
        var existing = product(patch.id);
        var patched = existing.apply(patch);
        log.info("-> {}", patched);
        return patched;
    }

    @RolesAllowed("Writer")
    @Mutation public Product delete(@NonNull String productId) {
        log.info("delete({}) by {}", productId, principal.getName());
        var removed = PRODUCTS.remove(productId);
        log.info("-> {}", removed);
        return removed;
    }

    static class ProductNotFoundException extends BusinessException {
        ProductNotFoundException(String productId) {super(new NotFoundException("product " + productId + " not found"));}
    }

    static class ProductForbiddenException extends BusinessException {
        ProductForbiddenException(String productId) {super(new ForbiddenException("product " + productId + " is forbidden"));}
    }

    static class BusinessException extends WebApplicationException {
        public BusinessException(WebApplicationException template) {
            super(template.getMessage(), response(template));
        }

        private static Response response(WebApplicationException exception) {
            return ProblemDetails.of(exception).toResponse().toJaxRs();
        }
    }
}

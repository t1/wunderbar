package test;

import com.github.t1.wunderbar.junit.Service;
import com.github.t1.wunderbar.junit.WunderBarExtension;
import io.smallrye.graphql.client.typesafe.api.GraphQlClientException;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import test.ProductResolver.Product;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;

import static com.github.t1.wunderbar.junit.ExpectedResponseBuilder.given;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;

@WunderBarExtension
class ProductResolverIT extends ProductResolverTest {
    @Override void failsWith(Throwable throwable, String message) {
        then(throwable).isNotNull();
        if (throwable instanceof GraphQlClientException) {
            GraphQlClientException e = (GraphQlClientException) throwable;
            then(e.getErrors()).hasSize(1);
            var error = e.getErrors().get(0);
            then(error.getMessage()).isEqualTo(message);
        } else {
            then(throwable.getMessage()).isEqualTo(message);
        }
    }

    @Disabled("https://github.com/smallrye/smallrye-graphql/issues/624")
    @Override @Test void shouldResolveNamedProductMethod() {}

    @Nested class OtherTechnologies {
        @Service UnrecognizableTechnologyService unrecognizableTechnologyService;

        @Test void shouldFailToRecognizeTechnology() {
            var throwable = catchThrowable(() -> given(unrecognizableTechnologyService.call()).willReturn(null));

            then(throwable).hasMessage("no technology recognized on " + UnrecognizableTechnologyService.class);
        }
    }

    interface UnrecognizableTechnologyService {
        Object call();
    }


    @Nested class RestServiceTest {
        @Service RestService restService;

        @Test void shouldCallRestService() {
            var givenProduct = Product.builder().id("r").name("some-product-name").build();
            given(restService.getProduct(givenProduct.id)).willReturn(givenProduct);

            var response = restService.getProduct(givenProduct.id);

            then(response).usingRecursiveComparison().isEqualTo(givenProduct);
        }

        @Test void shouldFailToCallFailingRestService() {
            var productId = "rx";
            given(restService.getProduct(productId)).willThrow(new ForbiddenException());

            var throwable = catchThrowableOfType(() -> restService.getProduct(productId), WebApplicationException.class);

            then(throwable.getResponse().getStatusInfo()).isEqualTo(FORBIDDEN);
        }

        @Test void shouldFailToResolveUnknownProduct() {
            given(restService.getProduct("x")).willThrow(new RuntimeException("product x not found"));

            var throwable = catchThrowableOfType(() -> restService.getProduct("x"), WebApplicationException.class);

            var response = throwable.getResponse();
            then(response.getStatusInfo()).isEqualTo(INTERNAL_SERVER_ERROR);
            then(response.readEntity(String.class)).isEqualTo(PRODUCT_NOT_FOUND_PROBLEM_DETAILS);
        }
    }

    @RegisterRestClient(baseUri = "dummy") @Path("/hello")
    public // RestEasy MP Rest Client requires the interface to be `public`
    interface RestService {
        @Path("/{productId}")
        @GET Product getProduct(@PathParam("productId") String productId);
    }

    private static final String PRODUCT_NOT_FOUND_PROBLEM_DETAILS = "{" +
        "\"detail\":\"product x not found\"," +
        "\"title\":\"RuntimeException\"," +
        "\"type\":\"urn:problem-type:java.lang.RuntimeException\"}";
}

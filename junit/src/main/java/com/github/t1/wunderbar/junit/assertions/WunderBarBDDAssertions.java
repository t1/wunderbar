package com.github.t1.wunderbar.junit.assertions;

import com.github.t1.wunderbar.junit.assertions.JsonValueAssert.JsonObjectAssert;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import io.smallrye.graphql.client.GraphQLClientException;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import org.assertj.core.api.BDDAssertions;

/**
 * An extension for {@link BDDAssertions BDD assertj}
 *
 * @see JsonValueAssert
 * @see JsonObjectAssert
 * @see GraphQLClientExceptionAssert
 * @see HttpRequestAssert
 * @see HttpResponseAssert
 */
public class WunderBarBDDAssertions extends BDDAssertions {
    public static JsonValueAssert<?, ?> then(JsonValue jsonValue) {return new JsonValueAssert<>(jsonValue);}

    public static JsonObjectAssert<?, ?> then(JsonObject jsonValue) {return new JsonObjectAssert<>(jsonValue);}

    public static GraphQLClientExceptionAssert<?, ?> then(GraphQLClientException exception) {return new GraphQLClientExceptionAssert<>(exception);}

    public static HttpRequestAssert<?, ?> then(HttpRequest request) {return new HttpRequestAssert<>(request);}

    public static HttpResponseAssert<?, ?> then(HttpResponse response) {return new HttpResponseAssert<>(response);}

    public static WebApplicationExceptionAssert<?, ?> then(WebApplicationException response) {return new WebApplicationExceptionAssert<>(response);}

    public static MediaTypeAssert<?, ?> then(MediaType response) {return new MediaTypeAssert<>(response);}
}

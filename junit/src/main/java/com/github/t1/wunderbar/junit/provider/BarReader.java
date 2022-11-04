package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.http.Authorization;
import com.github.t1.wunderbar.junit.http.Authorization.Dummy;
import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.Test;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;

import jakarta.ws.rs.core.MediaType;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.github.t1.wunderbar.junit.http.HttpUtils.optional;
import static com.github.t1.wunderbar.junit.http.HttpUtils.properties;
import static java.util.stream.Collectors.toUnmodifiableList;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;

abstract class BarReader {
    @SneakyThrows(IOException.class)
    static BarReader from(Path path) {
        if (Files.isDirectory(path))
            return new DirBarReader(path);
        return new JarBarReader(path);
    }

    Stream<Test> tests() {
        return treeEntries()
            .sorted()
            .distinct() // remove duplicates for all the files for one test
            .map(TreeEntry::toTest);
    }

    protected abstract Stream<TreeEntry> treeEntries();

    abstract String getDisplayName();


    public List<HttpInteraction> interactionsFor(Test test) {
        return IntStream.rangeClosed(1, test.getInteractionCount())
            .mapToObj(n -> new HttpInteraction(n, request(test, n), response(test, n)))
            .collect(toUnmodifiableList());
    }

    private HttpRequest request(Test test, int n) {return request(requestHeaders(test, n), requestBody(test, n));}

    private static HttpRequest request(Properties properties, Optional<String> body) {
        var builder = HttpRequest.builder();
        // FIXME #8 read all headers
        optional(properties, "Method").ifPresent(builder::method);
        optional(properties, "URI").ifPresent(builder::uri);
        optional(properties, ACCEPT).map(MediaType::valueOf).ifPresent(builder::accept);
        optional(properties, CONTENT_TYPE).ifPresent(builder::contentType);
        optional(properties, AUTHORIZATION).map(Authorization::valueOf).ifPresent(value -> {
            assert value instanceof Dummy : "expected " + AUTHORIZATION + " header to be the dummy value!";
            builder.authorization(value);
        });
        body.ifPresent(builder::body);
        return builder.build();
    }

    private Properties requestHeaders(Test test, int n) {return properties(read(test.getPath() + "/" + n + " request-headers.properties"));}

    private Optional<String> requestBody(Test test, int n) {return optionalRead(test.getPath() + "/" + n + " request-body.json");}


    private HttpResponse response(Test test, int n) {return HttpResponse.from(responseHeaders(test, n), responseBody(test, n));}

    private Properties responseHeaders(Test test, int n) {return properties(read(test.getPath() + "/" + n + " response-headers.properties"));}

    private Optional<String> responseBody(Test test, int n) {return optionalRead(test.getPath() + "/" + n + " response-body.json");}


    private String read(String name) {
        return optionalRead(name).orElseThrow(() -> new WunderBarException("bar entry [" + name + "] not found"));
    }

    protected abstract Optional<String> optionalRead(String name);


    @Value static class TreeEntry implements Comparable<TreeEntry> {
        @NonNull Path path;
        int number;
        @NonNull URI uri;

        private Test toTest() {return new Test(path, number, uri);}

        @Override public int compareTo(@NonNull TreeEntry that) {return COMPARATOR.compare(this, that);}

        private static final Comparator<TreeEntry> COMPARATOR = Comparator.comparing(TreeEntry::getUri)
            .thenComparing(TreeEntry::getNumber);
    }
}

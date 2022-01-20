package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.Test;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

abstract class BarReader {
    @SneakyThrows(IOException.class)
    static BarReader from(Path path) {
        if (Files.isDirectory(path))
            return new DirBarReader(path);
        return new JarBarReader(path);
    }

    Stream<Test> tests() {
        return treeEntries()
            .distinct() // remove duplicates for all the files for one test
            .map(TreeEntry::toTest);
    }

    protected abstract Stream<TreeEntry> treeEntries();

    abstract String getDisplayName();


    public List<HttpInteraction> interactionsFor(Test test) {
        return IntStream.rangeClosed(1, test.getInteractionCount())
            .mapToObj(n -> new HttpInteraction(n, request(test, n), response(test, n)))
            .collect(toList());
    }

    private HttpRequest request(Test test, int n) {return HttpRequest.from(requestHeaders(test, n), requestBody(test, n));}

    private Properties requestHeaders(Test test, int n) {return properties(read(test.getPath() + "/" + n + " request-headers.properties"));}

    private Optional<String> requestBody(Test test, int n) {return optionalRead(test.getPath() + "/" + n + " request-body.json");}


    private HttpResponse response(Test test, int n) {return HttpResponse.from(responseHeaders(test, n), responseBody(test, n));}

    private Properties responseHeaders(Test test, int n) {return properties(read(test.getPath() + "/" + n + " response-headers.properties"));}

    private Optional<String> responseBody(Test test, int n) {return optionalRead(test.getPath() + "/" + n + " response-body.json");}


    private String read(String name) {
        return optionalRead(name).orElseThrow(() -> new WunderBarException("bar entry [" + name + "] not found"));
    }

    protected abstract Optional<String> optionalRead(String name);


    @SneakyThrows(IOException.class)
    private static Properties properties(String string) {
        var properties = new Properties();
        properties.load(new StringReader(string));
        return properties;
    }


    @Value static class TreeEntry {
        @NonNull Path path;
        int number;
        @NonNull URI uri;

        Test toTest() {return new Test(path, number, uri);}
    }
}

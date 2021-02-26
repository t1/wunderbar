package com.github.t1.wunderbar.junit.runner;

import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.With;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Scanner;

import static java.nio.file.Files.exists;
import static java.util.concurrent.TimeUnit.SECONDS;

@Builder @With
public @Value class MavenCoordinates {
    public static MavenCoordinates of(String coordinates) {
        var split = coordinates.split(":", 5);
        if (split.length < 3) throw new RuntimeException("invalid Maven coordinates: " + coordinates);

        var builder = MavenCoordinates.builder()
            .groupId(split[0])
            .artifactId(split[1])
            .version(split[2]);
        if (split.length > 3)
            builder.packaging(split[3]);
        if (split.length > 4)
            builder.classifier(split[4]);
        return builder.build();
    }


    String groupId;
    String artifactId;
    String version;
    String packaging;
    String classifier;

    public String getCompactString() {
        return groupId + ":" + artifactId + ":" + version + ((packaging == null) ? "" : ":" + packaging) + ((classifier == null) ? "" : ":" + classifier);
    }

    public Path getLocalRepositoryPath() {
        return LOCAL_REPOSITORY
            .resolve(groupId.replace('.', '/'))
            .resolve(artifactId)
            .resolve(version)
            .resolve(fileName());
    }

    private String fileName() {
        var out = new StringBuilder()
            .append(artifactId)
            .append("-")
            .append(version);
        if (classifier != null) out.append("-").append(classifier);
        out.append(".").append(packaging);
        return out.toString();
    }


    @SneakyThrows({IOException.class, InterruptedException.class})
    public void download() {
        if (exists(getLocalRepositoryPath())) return;
        var mvn = new ProcessBuilder()
            .command("mvn", "dependency:get", "-D" + "artifact=" + getCompactString())
            .inheritIO() // may help with debugging
            .start();
        var exited = mvn.waitFor(30, SECONDS);
        if (!exited || mvn.exitValue() != 0) {
            System.out.println(readAll(mvn.getInputStream()));
            throw new RuntimeException("can't download maven dependency: " + getCompactString()
                + " " + readAll(mvn.getErrorStream()).trim());
        }
    }


    private static String readAll(InputStream inputStream) {
        if (inputStream == null) return "";
        try (var scanner = new Scanner(inputStream).useDelimiter("\\Z")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    private static final String MAVEN_HOME = System.getProperty("MAVEN_HOME", System.getProperty("user.home") + "/.m2");
    private static final Path LOCAL_REPOSITORY = Path.of(MAVEN_HOME).resolve("repository");
}

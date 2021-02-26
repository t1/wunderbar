package com.github.t1.wunderbar.junit.runner;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.nio.file.Path;

@Builder @With
public @Value class MavenCoordinates {
    public static MavenCoordinates of(String coordinates) {
        var split = coordinates.split(":", 5);
        if (split.length < 3) throw new RuntimeException("invalid Maven coordinates: " + coordinates);

        var builder = MavenCoordinates.builder()
            .groupId(split[0])
            .artifactId(split[1])
            .version(split[2]);
        if (split.length > 4)
            builder.classifier(split[3]);
        if (split.length > 3)
            builder.type(split[split.length - 1]);
        return builder.build();
    }


    String groupId;
    String artifactId;
    String version;
    String classifier;
    String type;

    public Path getPath() {
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
        out.append(".").append(type);
        return out.toString();
    }

    private static final Path LOCAL_REPOSITORY = Path.of(System.getProperty("user.home")).resolve(".m2/repository");
}

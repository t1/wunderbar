package com.github.t1.wunderbar.junit.consumer;

import lombok.experimental.UtilityClass;

import java.nio.file.Path;

import static java.util.stream.Collectors.toList;

@UtilityClass
public class TestBackdoor {
    public static Path writtenBar(String name) { return writtenBar(Path.of(name)); }

    public static Path writtenBar(Path name) {
        var bars = WunderBarApiConsumerJUnitExtension.BAR_WRITERS.values().stream()
            .filter(barWriter -> barWriter.getPath().equals(name))
            .peek(BarWriter::close)
            .map(BarWriter::getPath)
            .collect(toList());
        assert bars.size() == 1 : "Expected exactly one bar named " + name + " but got:" + bars;
        return bars.get(0);
    }
}

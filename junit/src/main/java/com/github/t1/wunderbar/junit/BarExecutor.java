package com.github.t1.wunderbar.junit;

import com.github.t1.wunderbar.junit.WunderBarJUnitExecutor.Test;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RequiredArgsConstructor
public class BarExecutor implements Executable {
    private final WunderBarJUnitExecutor bar;
    private final Test test;

    @Override public void execute() {
        assertNotNull(test);
    }
}

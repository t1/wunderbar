package com.github.t1.wunderbar.junit;

import com.github.t1.wunderbar.junit.WunderBarExecutorJUnit.Test;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@RequiredArgsConstructor
public class BarExecutor implements Executable {
    private final Test test;

    @Override public void execute() {
        assertNotNull(test);
    }
}

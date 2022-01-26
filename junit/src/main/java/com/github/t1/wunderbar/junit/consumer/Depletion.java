package com.github.t1.wunderbar.junit.consumer;

import com.github.t1.wunderbar.junit.WunderBarException;
import lombok.Builder;
import lombok.Value;

@Value @Builder
public class Depletion {
    int maxCallCount;

    public boolean isDepleted(int callCount) {
        return callCount > maxCallCount;
    }

    public void check(int callCount) {
        if (isDepleted(callCount))
            throw new WunderBarException("expectation is depleted [" + this + "] on call #" + callCount);
    }
}

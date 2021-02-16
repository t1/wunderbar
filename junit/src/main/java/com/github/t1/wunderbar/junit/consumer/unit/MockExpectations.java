package com.github.t1.wunderbar.junit.consumer.unit;

import com.github.t1.wunderbar.junit.Utils;
import com.github.t1.wunderbar.junit.consumer.WunderBarExpectation;
import com.github.t1.wunderbar.junit.consumer.WunderBarExpectations;
import com.github.t1.wunderbar.junit.consumer.WunderbarExpectationBuilder;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MockExpectations implements WunderBarExpectations {
    private final List<MockExpectation> expectations = new ArrayList<>();
    private final Object mock;

    public MockExpectations(Class<?> type) {
        this.mock = Mockito.mock(type);
    }

    @Override public Object invoke(Method method, Object... args) {
        for (var expectation : expectations)
            if (expectation.matches(method, args))
                return Utils.invoke(mock, method, args);

        var expectation = new MockExpectation(mock, method, args);
        expectations.add(expectation);
        WunderbarExpectationBuilder.buildingExpectation = expectation;

        return expectation.nullValue();
    }

    @Override public void done() {
        expectations.forEach(WunderBarExpectation::done);
        expectations.clear();
    }
}

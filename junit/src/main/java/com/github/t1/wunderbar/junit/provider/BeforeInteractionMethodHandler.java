package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProviderJUnitExtension.Execution;

import java.lang.reflect.Method;

class BeforeInteractionMethodHandler extends AbstractInteractionMethodHandler {
    public BeforeInteractionMethodHandler(Object instance, Method method) {
        super(instance, method);
    }

    @Override protected void apply(Object result, Execution execution) {
        if (result instanceof HttpInteraction) execution.expect(interaction -> (HttpInteraction) result);
        else if (result instanceof HttpRequest)
            execution.expect(interaction -> interaction.withRequest((HttpRequest) result));
        else super.apply(result, execution);
    }
}

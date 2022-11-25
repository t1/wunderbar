package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProviderJUnitExtension.Execution;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

class AfterInteractionMethodHandler extends AbstractInteractionMethodHandler {
    public AfterInteractionMethodHandler(Object instance, Method method) {
        super(instance, method);
    }

    @Override protected Object arg(Execution execution, Parameter parameter) {
        if (parameter.isAnnotationPresent(Actual.class)) {
            if (HttpResponse.class.equals(parameter.getType())) return execution.getActual();
            else throw new WunderBarException("a " + parameter.getType().getSimpleName() + " parameter can't take the `@Actual` annotation");
        }
        return super.arg(execution, parameter);
    }
}

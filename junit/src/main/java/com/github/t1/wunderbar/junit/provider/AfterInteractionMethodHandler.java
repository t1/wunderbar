package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.junit.provider.WunderBarApiProviderJUnitExtension.Execution;

import java.lang.reflect.Method;

class AfterInteractionMethodHandler extends AbstractInteractionMethodHandler {
    public AfterInteractionMethodHandler(Object instance, Method method) {
        super(instance, method);
    }

    @Override protected Object arg(Execution execution, Class<?> type) {
        if (type.equals(ActualHttpResponse.class))
            return new ActualHttpResponse(execution.getActual());
        else return super.arg(execution, type);
    }

    @Override protected void apply(Object result, Execution execution) {
        if (result instanceof ActualHttpResponse) execution.setActual(((ActualHttpResponse) result).getValue());
        else super.apply(result, execution);
    }
}

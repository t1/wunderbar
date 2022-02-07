package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.junit.provider.WunderBarApiProviderJUnitExtension.Executions;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

class AfterDynamicTestMethodHandler extends AbstractDynamicTestMethodHandler {
    public AfterDynamicTestMethodHandler(Object instance, Method method) {
        super(instance, method);
    }

    @Override protected Object arg(Executions executions, String typeName) {
        if (typeName.equals("java.util.List<" + ActualHttpResponse.class.getName() + ">")) return executions.getActualResponses();
        else return super.arg(executions, typeName);
    }

    @Override protected void apply(Type returnType, Object result, Executions executions) {
        if (result != null) throw wunderBarException("unexpected return type " + result.getClass()); // TODO test
    }
}

package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProviderJUnitExtension.Executions;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

class AfterDynamicTestMethodHandler extends AbstractDynamicTestMethodHandler {
    public AfterDynamicTestMethodHandler(Object instance, Method method) {
        super(instance, method);
    }

    @Override protected Object arg(Executions executions, Parameter parameter) {
        var typeName = parameter.getParameterizedType().getTypeName();
        if (typeName.equals("java.util.List<" + HttpResponse.class.getName() + ">") && parameter.isAnnotationPresent(Actual.class))
            return executions.getActualResponses();
        else return super.arg(executions, parameter);
    }

    @Override protected void apply(Type returnType, Object result, Executions executions) {
        if (result != null) throw wunderBarException("unexpected return type " + result.getClass()); // TODO test
    }
}

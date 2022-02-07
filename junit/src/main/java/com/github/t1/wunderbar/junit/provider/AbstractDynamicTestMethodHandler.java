package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.common.Utils;
import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProviderJUnitExtension.Executions;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

@RequiredArgsConstructor
abstract class AbstractDynamicTestMethodHandler {
    private final Object instance;
    private final Method method;

    void invoke(Executions executions) {
        var args = args(executions);
        var result = Utils.invoke(instance, method, args);
        apply(method.getGenericReturnType(), result, executions);
    }

    private Object[] args(Executions executions) {
        var args = new Object[method.getParameterCount()];
        for (int i = 0; i < args.length; i++) {
            var parameter = method.getParameters()[i];
            var typeName = parameter.getParameterizedType().getTypeName();
            args[i] = arg(executions, typeName);
        }
        return args;
    }

    protected Object arg(Executions executions, String typeName) {
        if (typeName.equals("java.util.List<" + HttpInteraction.class.getName() + ">"))
            return executions.getInteractions();
        else if (typeName.equals("java.util.List<" + HttpRequest.class.getName() + ">"))
            return executions.getExpectedRequests();
        else if (typeName.equals("java.util.List<" + HttpResponse.class.getName() + ">"))
            return executions.getExpectedResponses();
        else if (typeName.equals(WunderBarExecutions.class.getName()))
            return executions;
        else throw wunderBarException("unsupported argument type " + typeName);
    }

    protected abstract void apply(Type returnType, Object result, Executions executions);

    protected WunderBarException wunderBarException(String message) {
        return new WunderBarException(message);
    }
}

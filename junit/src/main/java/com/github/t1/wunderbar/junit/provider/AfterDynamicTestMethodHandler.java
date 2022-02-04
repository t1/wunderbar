package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.common.Utils;
import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProviderJUnitExtension.Executions;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;

@RequiredArgsConstructor
class AfterDynamicTestMethodHandler {
    private final Object instance;
    private final Method method;

    void invoke(Executions executions) {
        Object[] args = args(executions);
        Utils.invoke(instance, method, args);
    }

    private Object[] args(Executions executions) {
        var args = new Object[method.getParameterCount()];
        for (int i = 0; i < args.length; i++) {
            var typeName = method.getParameters()[i].getParameterizedType().getTypeName();
            if (typeName.equals("java.util.List<" + HttpInteraction.class.getName() + ">"))
                args[i] = executions.getInteractions();
            else if (typeName.equals("java.util.List<" + HttpRequest.class.getName() + ">"))
                args[i] = executions.getExpectedRequests();
            else if (typeName.equals("java.util.List<" + ActualHttpResponse.class.getName() + ">"))
                args[i] = executions.getActualResponses();
            else if (typeName.equals("java.util.List<" + HttpResponse.class.getName() + ">"))
                args[i] = executions.getExpectedResponses();
            else if (method.getParameters()[i].getType().equals(WunderBarExecutions.class))
                args[i] = executions;
            else throw new WunderBarException("invalid argument type for parameter " + i + " of " + method);
        }
        return args;
    }
}

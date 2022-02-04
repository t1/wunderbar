package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.common.Utils;
import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.HttpBarExecutable.Execution;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;

@RequiredArgsConstructor
class AfterInteractionMethodHandler {
    private final Object instance;
    private final Method method;

    public void invoke(Execution execution) {
        var args = args(execution);
        var result = Utils.invoke(instance, method, args);
        apply(execution, result);
    }

    private void apply(Execution execution, Object result) {
        if (result instanceof HttpResponse) execution.expected = execution.expected.withResponse((HttpResponse) result);
        else if (result instanceof ActualHttpResponse) execution.actual = ((ActualHttpResponse) result).getValue();
        else if (result != null) throw new WunderBarException("unexpected return type " + result.getClass()); // TODO test (+ null)
    }

    private Object[] args(Execution execution) {
        var args = new Object[method.getParameterCount()];
        for (int i = 0; i < args.length; i++) {
            if (method.getParameters()[i].getType().equals(HttpInteraction.class))
                args[i] = execution.expected;
            else if (method.getParameters()[i].getType().equals(HttpRequest.class))
                args[i] = execution.expected.getRequest();
            else if (method.getParameters()[i].getType().equals(HttpResponse.class))
                args[i] = execution.expected.getResponse();
            else if (method.getParameters()[i].getType().equals(ActualHttpResponse.class))
                args[i] = new ActualHttpResponse(execution.actual);
            else throw new WunderBarException("invalid argument type for parameter " + i + " of " + method);
        }
        return args;
    }
}

package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.common.Utils;
import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProviderJUnitExtension.Execution;
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

    private Object[] args(Execution execution) {
        var args = new Object[method.getParameterCount()];
        for (int i = 0; i < args.length; i++) {
            Class<?> type = method.getParameters()[i].getType();
            if (type.equals(HttpInteraction.class))
                args[i] = execution.getExpected();
            else if (type.equals(HttpRequest.class))
                args[i] = execution.getExpected().getRequest();
            else if (type.equals(HttpResponse.class))
                args[i] = execution.getExpected().getResponse();
            else if (type.equals(ActualHttpResponse.class))
                args[i] = new ActualHttpResponse(execution.getActual());
            else if (type.equals(WunderBarExecution.class))
                args[i] = execution;
            else throw new WunderBarException("invalid argument type for parameter " + i + " of " + method);
        }
        return args;
    }

    private void apply(Execution execution, Object result) {
        if (result instanceof HttpResponse) execution.expect(interaction -> interaction.withResponse((HttpResponse) result));
        else if (result instanceof ActualHttpResponse) execution.setActual(((ActualHttpResponse) result).getValue());
        else if (result != null) throw new WunderBarException("unexpected return type " + result.getClass()); // TODO test (+ null)
    }
}

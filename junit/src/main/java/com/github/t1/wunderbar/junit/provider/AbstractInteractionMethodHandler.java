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
abstract class AbstractInteractionMethodHandler {
    private final Object instance;
    private final Method method;

    void invoke(Execution execution) {
        var args = args(execution);
        var result = Utils.invoke(instance, method, args);
        apply(result, execution);
    }

    private Object[] args(Execution execution) {
        var args = new Object[method.getParameterCount()];
        for (int i = 0; i < args.length; i++) {
            Class<?> type = method.getParameters()[i].getType();
            args[i] = arg(execution, type);
        }
        return args;
    }

    protected Object arg(Execution execution, Class<?> type) {
        if (type.equals(HttpInteraction.class))
            return execution.getExpected();
        else if (type.equals(HttpRequest.class))
            return execution.getExpected().getRequest();
        else if (type.equals(HttpResponse.class))
            return execution.getExpected().getResponse();
        else if (type.equals(WunderBarExecution.class))
            return execution;
        else throw new WunderBarException("unsupported argument type " + type.getSimpleName() + " of " + method);
    }

    protected void apply(Object result, Execution execution) {
        if (result instanceof HttpResponse) execution.expect(interaction -> interaction.withResponse((HttpResponse) result));
        else if (result != null) throw new WunderBarException("unexpected return type " + result.getClass()); // TODO test
    }
}

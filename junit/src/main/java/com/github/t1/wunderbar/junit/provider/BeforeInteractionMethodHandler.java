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
class BeforeInteractionMethodHandler {
    private final Object instance;
    private final Method method;

    void invoke(Execution execution) {
        Object[] args = args(execution);
        var result = Utils.invoke(instance, method, args);
        apply(result, execution);
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
            else if (type.equals(WunderBarExecution.class))
                args[i] = execution;
            else throw new WunderBarException("invalid argument type for parameter " + i + " of " + method);
        }
        return args;
    }

    private void apply(Object result, Execution execution) {
        if (result instanceof HttpInteraction) execution.expect(interaction -> (HttpInteraction) result);
        else if (result instanceof HttpRequest) execution.expect(interaction -> interaction.withRequest((HttpRequest) result));
        else if (result instanceof HttpResponse) execution.expect(interaction -> interaction.withResponse((HttpResponse) result));
        else if (result != null) throw new WunderBarException("unexpected return type " + result.getClass()); // TODO test
    }
}

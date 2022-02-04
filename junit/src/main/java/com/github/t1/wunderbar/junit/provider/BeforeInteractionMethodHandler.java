package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.common.Utils;
import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.WunderBarTestFinder.Test;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DynamicNode;

import java.lang.reflect.Method;

@RequiredArgsConstructor
class BeforeInteractionMethodHandler {
    private final Object instance;
    private final Method method;

    HttpInteraction invoke(Test test, HttpInteraction interaction) {
        var node = test.toDynamicNode(t -> BeforeDynamicTestMethodHandler::dummyExecutable);
        Object[] args = args(node, interaction);
        var result = Utils.invoke(instance, method, args);
        return apply(result, interaction);
    }

    private Object[] args(DynamicNode node, HttpInteraction interaction) {
        var args = new Object[method.getParameterCount()];
        for (int i = 0; i < args.length; i++) {
            if (method.getParameters()[i].getType().equals(HttpInteraction.class))
                args[i] = interaction;
            else if (method.getParameters()[i].getType().equals(HttpRequest.class))
                args[i] = interaction.getRequest();
            else if (method.getParameters()[i].getType().equals(HttpResponse.class))
                args[i] = interaction.getResponse();
            else throw new WunderBarException("invalid argument type for parameter " + i + " of " + method);
        }
        return args;
    }

    private HttpInteraction apply(Object result, HttpInteraction interaction) {
        if (result instanceof HttpInteraction) return (HttpInteraction) result;
        else if (result instanceof HttpRequest) return interaction.withRequest((HttpRequest) result);
        else if (result instanceof HttpResponse) return interaction.withResponse((HttpResponse) result);
        else if (result == null) return interaction; // void
        throw new WunderBarException("unexpected return type " + result.getClass()); // TODO test (+ null)
    }
}

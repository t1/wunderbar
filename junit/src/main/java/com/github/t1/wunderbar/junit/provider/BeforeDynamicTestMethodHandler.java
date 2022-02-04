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
import java.util.List;
import java.util.function.BiFunction;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
class BeforeDynamicTestMethodHandler {
    private final Object instance;
    private final Method method;

    List<HttpInteraction> invoke(Test test, List<HttpInteraction> list) {
        var node = test.toDynamicNode(t -> BeforeDynamicTestMethodHandler::dummyExecutable);
        Object[] args = args(node, list);
        var result = Utils.invoke(instance, method, args);
        return apply(result, list);
    }

    static void dummyExecutable() {
        throw new WunderBarException("don't invoke this DynamicNode; it's only for reading the displayName, etc.");
    }

    private Object[] args(DynamicNode node, List<HttpInteraction> list) {
        var args = new Object[method.getParameterCount()];
        for (int i = 0; i < args.length; i++) {
            if (method.getParameters()[i].getParameterizedType().getTypeName().equals("java.util.List<" + HttpInteraction.class.getName() + ">"))
                args[i] = list;
            else if (method.getParameters()[i].getParameterizedType().getTypeName().equals("java.util.List<" + HttpRequest.class.getName() + ">"))
                args[i] = list.stream().map(HttpInteraction::getRequest).collect(toList());
            else if (method.getParameters()[i].getParameterizedType().getTypeName().equals("java.util.List<" + HttpResponse.class.getName() + ">"))
                args[i] = list.stream().map(HttpInteraction::getResponse).collect(toList());
            else if (method.getParameters()[i].getType().equals(DynamicNode.class))
                args[i] = node;
            else throw new WunderBarException("invalid argument type for parameter " + i + " of " + method);
        }
        return args;
    }

    @SuppressWarnings("unchecked")
    private List<HttpInteraction> apply(Object result, List<HttpInteraction> interactions) {
        if (result instanceof List) {
            var list = (List<?>) result;
            if (list.isEmpty()) return emptyList();
            else if (list.get(0) instanceof HttpInteraction) return (List<HttpInteraction>) result;
            else if (list.get(0) instanceof HttpRequest) return replaceInteractions(interactions, (List<HttpRequest>) list, HttpInteraction::withRequest);
            else if (list.get(0) instanceof HttpResponse) return replaceInteractions(interactions, (List<HttpResponse>) list, HttpInteraction::withResponse);
            else throw new WunderBarException("unexpected list return type " + result.getClass()); // TODO test
        } else if (result != null) throw new WunderBarException("unexpected return type " + result.getClass()); // TODO test (+ null)
        else return interactions;
    }

    private <T> List<HttpInteraction> replaceInteractions(List<HttpInteraction> interactions, List<T> replacements, BiFunction<HttpInteraction, T, HttpInteraction> replace) {
        if (interactions.size() != replacements.size())
            throw new WunderBarException("you can't change the size of interactions by returning a different number of requests or responses");
        for (int i = 0; i < interactions.size(); i++) {
            interactions.set(i, replace.apply(interactions.get(i), replacements.get(i)));
        }
        return interactions;
    }
}

package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProviderJUnitExtension.Executions;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BiFunction;

import static java.util.Collections.emptyList;

class BeforeDynamicTestMethodHandler extends AbstractDynamicTestMethodHandler {
    public BeforeDynamicTestMethodHandler(Object instance, Method method) {
        super(instance, method);
    }

    @SuppressWarnings("unchecked")
    @Override protected void apply(Object result, Executions executions) {
        if (result instanceof List) {
            var list = (List<?>) result;
            if (list.isEmpty()) executions.setInteractions(emptyList());
            else if (list.get(0) instanceof HttpInteraction) executions.setInteractions((List<HttpInteraction>) list);
            else if (list.get(0) instanceof HttpRequest) {
                replaceInteractions(executions, (List<HttpRequest>) list, HttpInteraction::withRequest);
            } else if (list.get(0) instanceof HttpResponse) {
                replaceInteractions(executions, (List<HttpResponse>) list, HttpInteraction::withResponse);
            } else throw new WunderBarException("unexpected list return type " + result.getClass()); // TODO test
        } else if (result != null) throw new WunderBarException("unexpected return type " + result.getClass()); // TODO test
    }

    private <T> void replaceInteractions(Executions executions, List<T> replacements, BiFunction<HttpInteraction, T, HttpInteraction> replace) {
        var interactions = executions.getInteractions();
        if (interactions.size() != replacements.size())
            throw new WunderBarException("you can't change the size of interactions by returning a different number of requests or responses");
        for (int i = 0; i < interactions.size(); i++) {
            interactions.set(i, replace.apply(interactions.get(i), replacements.get(i)));
        }
        executions.setInteractions(interactions);
    }
}

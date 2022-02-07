package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import com.github.t1.wunderbar.junit.provider.WunderBarApiProviderJUnitExtension.Executions;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.BiFunction;

class BeforeDynamicTestMethodHandler extends AbstractDynamicTestMethodHandler {
    public BeforeDynamicTestMethodHandler(Object instance, Method method) {
        super(instance, method);
    }

    @SuppressWarnings("unchecked")
    @Override protected void apply(Type returnType, Object result, Executions executions) {
        if (returnType instanceof ParameterizedType) {
            var parameterizedType = (ParameterizedType) returnType;
            if (parameterizedType.getRawType() instanceof Class && List.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())) {
                var elementType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                if (elementType.isAssignableFrom(HttpInteraction.class)) executions.setInteractions((List<HttpInteraction>) result);
                else if (elementType.isAssignableFrom(HttpRequest.class)) replaceInteractions(executions, (List<HttpRequest>) result, HttpInteraction::withRequest);
                else if (elementType.isAssignableFrom(HttpResponse.class)) replaceInteractions(executions, (List<HttpResponse>) result, HttpInteraction::withResponse);
                else throw wunderBarException("unexpected list return type " + returnType); // TODO test
            } else if (result != null) throw wunderBarException("unexpected parameterized return type " + returnType); // TODO test
        } else if (result != null) throw wunderBarException("unexpected return type " + returnType); // TODO test
    }

    private <T> void replaceInteractions(Executions executions, List<T> replacements, BiFunction<HttpInteraction, T, HttpInteraction> replace) {
        var interactions = executions.getInteractions();
        if (interactions.size() != replacements.size())
            throw wunderBarException("you can't change the size of interactions by returning a different number of requests or responses");
        for (int i = 0; i < interactions.size(); i++) {
            interactions.set(i, replace.apply(interactions.get(i), replacements.get(i)));
        }
        executions.setInteractions(interactions);
    }
}

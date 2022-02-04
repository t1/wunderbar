package com.github.t1.wunderbar.junit.provider;

import com.github.t1.wunderbar.common.Utils;
import com.github.t1.wunderbar.junit.WunderBarException;
import com.github.t1.wunderbar.junit.http.HttpInteraction;
import com.github.t1.wunderbar.junit.http.HttpRequest;
import com.github.t1.wunderbar.junit.http.HttpResponse;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;
import java.util.List;

import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
class AfterDynamicTestMethodHandler {
    private final Object instance;
    private final Method method;

    Object invoke(List<HttpInteraction> interactions, List<HttpResponse> actualResponses) {
        Object[] args = args(interactions, actualResponses);
        return Utils.invoke(instance, method, args);
    }

    private Object[] args(List<HttpInteraction> list, List<HttpResponse> actualResponses) {
        var args = new Object[method.getParameterCount()];
        for (int i = 0; i < args.length; i++) {
            if (method.getParameters()[i].getParameterizedType().getTypeName().equals("java.util.List<" + HttpInteraction.class.getName() + ">"))
                args[i] = list;
            else if (method.getParameters()[i].getParameterizedType().getTypeName().equals("java.util.List<" + HttpRequest.class.getName() + ">"))
                args[i] = list.stream().map(HttpInteraction::getRequest).collect(toList());
            else if (method.getParameters()[i].getParameterizedType().getTypeName().equals("java.util.List<" + HttpResponse.class.getName() + ">"))
                args[i] = list.stream().map(HttpInteraction::getResponse).collect(toList());
                // TODO List<ActualHttpResponse>
            else throw new WunderBarException("invalid argument type for parameter " + i + " of " + method);
        }
        return args;
    }
}

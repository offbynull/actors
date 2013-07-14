package com.offbynull.p2prpc;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public final class InvokeData {
    private String methodName;
    private Pair<Object, String>[] arguments;

    public InvokeData(String methodName, Object[] arguments,
            Class<?>[] paramTypes) {
        if (methodName == null || arguments == null || paramTypes == null) {
            throw new NullPointerException();
        }
        
        for (Class<?> paramType : paramTypes) {
            if (paramType == null) {
                throw new NullPointerException();
            }
        }
        
        if (arguments.length != paramTypes.length) {
            throw new IllegalArgumentException();
        }
        
        this.arguments = new Pair[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            this.arguments[i] = new ImmutablePair(arguments[i],
                    paramTypes[i].getName());
        }
        
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }

    public Object[] getArguments() {
        Object[] ret = new Object[arguments.length];
        
        for (int i = 0; i < arguments.length; i++) {
            ret[i] = arguments[i].getKey();
        }
        
        return ret;
    }

    public Class<?>[] getParameterTypes() throws ClassNotFoundException {
        Class<?>[] ret = new Class[arguments.length];
        
        for (int i = 0; i < arguments.length; i++) {
            ret[i] = Class.forName(arguments[i].getValue());
        }
        
        return ret;
    }
}

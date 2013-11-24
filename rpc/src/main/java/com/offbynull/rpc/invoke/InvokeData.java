package com.offbynull.rpc.invoke;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Method invokation details. Contains the method name, arguments, and parameter types of the method being invoked.
 * @author Kasra F
 */
public final class InvokeData {
    private String methodName;
    private Pair<Object, String>[] arguments;

    public InvokeData(String methodName, Object[] arguments,
            Class<?>[] paramTypes) {
        Validate.notNull(methodName);
        Validate.noNullElements(arguments);
        Validate.noNullElements(paramTypes);
        Validate.isTrue(arguments.length == paramTypes.length);
        
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

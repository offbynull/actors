/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.rpc.invoke;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Method invocation details. Contains the method name, arguments, and parameter types of the method being invoked.
 * @author Kasra Faghihi
 */
public final class InvokeData {
    private String methodName;
    private Pair<Object, String>[] arguments;

    /**
     * Constructs an {@link InvokeData} object.
     * @param methodName method name called
     * @param arguments arguments passed in to method
     * @param paramTypes method's expected parameter types
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    public InvokeData(String methodName, Object[] arguments,
            Class<?>[] paramTypes) {
        Validate.notNull(methodName);
        Validate.notNull(arguments);
        Validate.noNullElements(paramTypes);
        Validate.isTrue(arguments.length == paramTypes.length);
        
        this.arguments = new Pair[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            this.arguments[i] = new ImmutablePair(arguments[i],
                    paramTypes[i].getName());
        }
        
        this.methodName = methodName;
    }

    /**
     * Gets the method name.
     * @return method name
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Gets the method arguments.
     * @return method arguments
     */
    public Object[] getArguments() {
        Object[] ret = new Object[arguments.length];
        
        for (int i = 0; i < arguments.length; i++) {
            ret[i] = arguments[i].getKey();
        }
        
        return ret;
    }

    /**
     * Gets the method parameter types.
     * @return parameter types
     * @throws ClassNotFoundException 
     */
    public Class<?>[] getParameterTypes() throws ClassNotFoundException {
        Class<?>[] ret = new Class[arguments.length];
        
        for (int i = 0; i < arguments.length; i++) {
            ret[i] = Class.forName(arguments[i].getValue());
        }
        
        return ret;
    }
}

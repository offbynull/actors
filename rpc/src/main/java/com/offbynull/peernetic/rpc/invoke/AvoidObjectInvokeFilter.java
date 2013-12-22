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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * An {@link InvokeFilter} that makes sure that method names being invoked don't match that of {@link Object}.
 * @author Kasra F
 */
public final class AvoidObjectInvokeFilter implements InvokeFilter {
    private static final Set<String> OBJECT_METHOD_NAMES;
    
    static {
        Set<String> methodNames = new HashSet<>();
        
        for (Method method : Object.class.getMethods()) {
            methodNames.add(method.getName().toLowerCase(Locale.ENGLISH));
        }
        
        OBJECT_METHOD_NAMES = Collections.unmodifiableSet(methodNames);
    }
    
    @Override
    public InvokeData filter(InvokeData data) {
        if (OBJECT_METHOD_NAMES.contains(data.getMethodName().toLowerCase(Locale.ENGLISH))) {
            throw new RuntimeException("Method name of Object detected");
        }
        
        return data;
    }
    
}

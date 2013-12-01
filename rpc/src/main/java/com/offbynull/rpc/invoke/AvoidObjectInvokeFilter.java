package com.offbynull.rpc.invoke;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
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
            methodNames.add(method.getName().toLowerCase());
        }
        
        OBJECT_METHOD_NAMES = Collections.unmodifiableSet(methodNames);
    }
    
    @Override
    public InvokeData filter(InvokeData data) {
        if (OBJECT_METHOD_NAMES.contains(data.getMethodName().toLowerCase())) {
            throw new RuntimeException("Method name of Object detected");
        }
        
        return data;
    }
    
}

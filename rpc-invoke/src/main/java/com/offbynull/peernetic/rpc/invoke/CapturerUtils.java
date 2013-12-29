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
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;

/**
 * Utility class that allows {@link Capturer} and {@link AsyncCapturer} implementations to perform sanity checks on interfaces and classes .
 * @author User
 */
public final class CapturerUtils {

    private CapturerUtils() {
        // Do nothing
    }

    /**
     * Map the methods from an async interface to a normal interface.
     * @param <T> type
     * @param <AT> async type
     * @param normalClass normal class
     * @param asyncClass async class
     * @return returns a {@link Map} where the keys are methods in the async class and the values are the matching methods in the normal
     * class
     * @throws NullPointerException if any arguments are {@code null}
     */
    public static <T, AT> Map<Method, Method> mapAsyncInterfaceToNormalClass(Class<T> normalClass, Class<AT> asyncClass) {
        Validate.isTrue(asyncClass.isInterface(), "Async type must be interface");
        
        Map<Method, Method> methodMap = new HashMap<>();
        
        Method[] asyncMethods = asyncClass.getDeclaredMethods();
        for (Method asyncMethod : asyncMethods) {
            validateAsyncMethod(asyncMethod);
            Method syncMethod = matchSyncMethodForAsyncMethod(normalClass, asyncClass, asyncMethod);
            
            methodMap.put(asyncMethod, syncMethod);
        }
        
        return methodMap;
    }
    
    /**
     * Finds the method in the normal class that matches a method in the async class.
     * @param <T> type
     * @param <AT> async type
     * @param normalClass normal class
     * @param asyncClass async class
     * @param asyncMethod method to find normal equivalent for
     * @return method from {@code normalClass} that matches {@code asyncMethod}
     * @throws IllegalArgumentException if {@code asyncMethod} is not in {@code normalClass}
     * @throws NullPointerException if any arguments are {@code null}
     */
    public static <T, AT> Method matchSyncMethodForAsyncMethod(Class<T> normalClass, Class<AT> asyncClass, Method asyncMethod) {
        Validate.isTrue(ClassUtils.isAssignable(asyncClass, asyncMethod.getDeclaringClass()),
                "Async class doesn't match async method class");
        
        Class<?>[] asyncParamTypes = asyncMethod.getParameterTypes();
            
        String methodName = asyncMethod.getName();
        Class<?>[] relevantAsyncParamTypes = Arrays.copyOfRange(asyncParamTypes, 1, asyncParamTypes.length);
        
        Method syncMethod;
        try {
            syncMethod = normalClass.getMethod(methodName, relevantAsyncParamTypes);
        } catch (NoSuchMethodException nsme) {
            throw new IllegalArgumentException("Method " + methodName + " not found", nsme);
        }
        
        return syncMethod;
    }
    
    /**
     * Validates a method in an async class to make sure that it's public, it returns void, and its first parameter is a
     * {@link AsyncResultListener} (meaning that it must have at least 1 parameter).
     * @param method method to check
     * @throws IllegalArgumentException if not valid (see method description).
     * @throws NullPointerException if any arguments are {@code null}
     */
    public static void validateAsyncMethod(Method method) {
        Validate.isTrue(Modifier.isPublic(method.getModifiers()), "Async method must be public");
        Validate.isTrue(method.getReturnType() == Void.TYPE, "Async method return type must be void");

        Validate.isTrue(method.getExceptionTypes().length == 0, "Async method must not declare exceptions");
        Class<?>[] paramTypes = method.getParameterTypes();

        Validate.isTrue(paramTypes.length > 0 && paramTypes[0] == AsyncResultListener.class,
            "Async method first parameter must be " + AsyncResultListener.class.getSimpleName());
    }

    /**
     * Validates that an object fits as a method's return type.
     * @param method method to check against
     * @param result object to check
     * @throws IllegalArgumentException if there's a mis-match between primitive/object types, if the types aren't assignable, or if the
     * return type is void but the return value is non-null
     * @throws NullPointerException if {@code method} is {@code null}
     */
    public static void validateReturn(Method method, Object result) {
        Class<?> returnType = method.getReturnType();

        if (result != null) {
            Validate.isTrue(returnType != Void.TYPE, "Must return non-null for void type");
            Validate.isTrue(ClassUtils.isAssignable(result.getClass(), returnType), "Must return matching class type");
        } else {
            Validate.isTrue(!(returnType.isPrimitive() && returnType != Void.TYPE), "Return null for primitive type");
        }
    }

    /**
     * Validates that a throwable fits as a throwable that a method could send.
     * @param method method to check against
     * @param throwable throwable to check
     * @throws IllegalArgumentException if {@code throwable} isn't of type {@link RuntimeException} and isn't assignable as one of the 
     * declared type in {@code method}
     * @throws NullPointerException if {@code method} is {@code null}
     */
    public static void validateThrowable(Method method, Object throwable) {
        Validate.notNull(throwable, "null is not throwable");

        Validate.isTrue(throwable instanceof Throwable, "Not of type throwable");

        if (!(throwable instanceof RuntimeException)) {
            Class<?>[] throwableTypes = method.getExceptionTypes();

            boolean matched = false;
            for (Class<?> throwableType : throwableTypes) {
                if (ClassUtils.isAssignable(throwable.getClass(), throwableType)) {
                    matched = true;
                    break;
                }
            }

            Validate.isTrue(matched, throwable.getClass() + " does not match method throws classes");
        }
    }
}

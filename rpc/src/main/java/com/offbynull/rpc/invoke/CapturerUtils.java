package com.offbynull.rpc.invoke;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;

final class CapturerUtils {

    private CapturerUtils() {
        // Do nothing
    }

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
    
    public static <T, AT> Method matchSyncMethodForAsyncMethod(Class<T> normalClass, Class<AT> asyncClass, Method asyncMethod) {
        Validate.isTrue(ClassUtils.isAssignable(asyncClass, asyncMethod.getDeclaringClass()), "Async class doesn't match async method class");
        
        Class<?>[] asyncParamTypes = asyncMethod.getParameterTypes();
            
        String methodName = asyncMethod.getName();
        Class<?>[] relevantAsyncParamTypes = Arrays.copyOfRange(asyncParamTypes, 1, asyncParamTypes.length);
        
        Method syncMethod;
        try {
            syncMethod = normalClass.getMethod(methodName, relevantAsyncParamTypes);
        } catch (NoSuchMethodException nsme) {
            throw new IllegalArgumentException("Method " + methodName + " not found");
        }
        
        return syncMethod;
    }
    
    public static void validateAsyncMethod(Method method) {
        Validate.isTrue(Modifier.isPublic(method.getModifiers()), "Async method must be public");
        Validate.isTrue(method.getReturnType() == Void.TYPE, "Async method return type must be void");

        Validate.isTrue(method.getExceptionTypes().length == 0, "Async method must not declare exceptions");
        Class<?>[] paramTypes = method.getParameterTypes();

        Validate.isTrue(paramTypes.length > 0 && paramTypes[0] == AsyncResultListener.class,
            "Async method first parameter must be " + AsyncResultListener.class.getSimpleName());
    }

    public static void validateReturn(Method method, Object result) {
        Class<?> returnType = method.getReturnType();

        if (result != null) {
            Validate.isTrue(returnType != Void.TYPE, "Must return non-null for void type");
            Validate.isTrue(ClassUtils.isAssignable(result.getClass(), returnType), "Must return matching class type");
        } else {
            Validate.isTrue(!(returnType.isPrimitive() && returnType != Void.TYPE), "Return null for primitive type");
        }
    }

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

package com.offbynull.peernetic.common.message;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.FieldUtils;

final class InternalUtils {
    private InternalUtils() {
        // do nothing
    }

    public static <N> N getNonceValue(Object message) {
        try {
            Field nonceField = findNonceField(message.getClass());
            nonceField.setAccessible(true);
            return (N) nonceField.get(message);
        } catch (IllegalArgumentException |IllegalAccessException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public static <N> void setNonceValue(Object message, N nonce) {
        try {
            Field nonceField = findNonceField(message.getClass());
            nonceField.setAccessible(true);
            nonceField.set(message, nonce);
        } catch (IllegalArgumentException |IllegalAccessException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public static boolean validateMessage(Object message) {
        Method validationMethod = findValidationMethod(message.getClass());
        validationMethod.setAccessible(true);
        try {
            validationMethod.invoke(message);
            return true;
        } catch (IllegalArgumentException |IllegalAccessException ex) {
            throw new IllegalArgumentException(ex);
        } catch (InvocationTargetException ite) {
            return false;
        }
    }

    public static Field findNonceField(Class<?> cls) {
        Field[] fields = FieldUtils.getAllFields(cls);
        List<Field> foundFields = new ArrayList<>(1);

        for (Field field : fields) {
            int len = field.getAnnotationsByType(NonceField.class).length;
            Validate.isTrue(len <= 1, "Multiple " + NonceField.class.getSimpleName() + " annotations found on a single field");

            if (len == 1) {
                foundFields.add(field);
            }
        }

        Validate.isTrue(foundFields.size() == 1, "Incorrect number of fields with " + NonceField.class.getSimpleName() + " annotation"
                + " found (must only be 1)");

        return foundFields.get(0);
    }

    public static Method findValidationMethod(Class<?> cls) {
        Method[] methods = cls.getMethods();
        List<Method> foundMethods = new ArrayList<>(1);

        for (Method method : methods) {
            int len = method.getAnnotationsByType(ValidationMethod.class).length;
            Validate.isTrue(len <= 1, "Multiple " + ValidationMethod.class.getSimpleName() + " annotations found on a single method");

            if (len == 1) {
                foundMethods.add(method);
            }
        }

        Validate.isTrue(foundMethods.size() == 1, "Incorrect number of methods with " + ValidationMethod.class.getSimpleName()
                + " annotation found (must only be 1)");

        Method method = foundMethods.get(0);
        Validate.isTrue(method.getParameterTypes().length == 0, "Validation method must not take in any arguments");

        return method;
    }
}

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

import java.util.Collections;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * A static class that can contains extra information for methods invoked through {@link Invoker}.
 * @author Kasra Faghihi
 */
public final class InvokeThreadInformation {
    private static ThreadLocal<Map<Object, Object>> tls = new ThreadLocal<>();

    private InvokeThreadInformation() {
        // do nothing
    }
    
    /**
     * Sets the extra data for the current thread. Don't touch if you don't know what you're doing.
     * @param info extra data
     * @throws NullPointerException if any arguments are {@code null}
     */
    public static void setInvokeThreadInfo(Map<Object, Object> info) {
        Validate.notNull(info);

        tls.set(Collections.unmodifiableMap(info));
    }
    
    /**
     * Removes the extra data for the current thread.  Don't touch if you don't know what you're doing.
     */
    public static void removeInvokeThreadInfo() {
        tls.remove();
    }

    /**
     * Gets the extra data for the current thread.
     * @param <K> key type
     * @param <V> value type
     * @return unmodifiable map containing the extra data
     */
    public static <K, V> Map<K, V> getInfoMap() {
        return (Map<K, V>) tls.get();
    }

    /**
     * Gets the extra data for a key for the current thread.
     * @param key key
     * @param <K> key type
     * @param <V> value type
     * @return unmodifiable map containing the extra data
     */
    public static <K, V> V getInfo(K key) {
        Map<K, V> map = (Map<K, V>) tls.get();
        return map == null ? null : map.get(key);
    }
}

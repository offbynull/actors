package com.offbynull.rpc.invoke;

import java.util.Collections;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * A static class that can contains extra information for methods invoked through {@link Invoker}.
 * @author Kasra F
 */
public final class InvokeThreadInformation {
    private static ThreadLocal<Map<Object, Object>> tls = new ThreadLocal<>();

    private InvokeThreadInformation() {
        // do nothing
    }
    
    /**
     * Sets the extra data for the current thread.
     * @param info extra data
     * @throws NullPointerException if any arguments are {@code null}
     */
    static void setInvokeThreadInfo(Map<Object, Object> info) {
        Validate.notNull(info);

        tls.set(Collections.unmodifiableMap(info));
    }
    
    /**
     * Removes the extra data for the current thread.
     */
    static void removeInvokeThreadInfo() {
        tls.remove();
    }

    /**
     * Gets the extra data for the current thread.
     * @param <K> key type
     * @param <V> value type
     * @return unmodifiable map containing the extra data
     */
    public static <K,V> Map<K,V> getInfoMap() {
        return (Map<K, V>) tls.get();
    }

    /**
     * Gets the extra data for a key for the current thread.
     * @param key key
     * @param <K> key type
     * @param <V> value type
     * @return unmodifiable map containing the extra data
     */
    public static <K,V> V getInfo(K key) {
        Map<K,V> map = (Map<K, V>) tls.get();
        return map == null ? null : map.get(key);
    }
}

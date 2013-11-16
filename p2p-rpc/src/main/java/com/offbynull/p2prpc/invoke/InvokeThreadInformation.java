package com.offbynull.p2prpc.invoke;

import java.util.Collections;
import java.util.Map;
import org.apache.commons.lang3.Validate;

public final class InvokeThreadInformation {
    private static ThreadLocal<Map<Object, Object>> tls = new ThreadLocal<>();

    private InvokeThreadInformation() {
        // do nothing
    }
    
    static void setInvokeThreadInfo(Map<Object, Object> info) {
        Validate.notNull(info);

        tls.set(Collections.unmodifiableMap(info));
    }
    
    static void removeInvokeThreadInfo() {
        tls.remove();
    }

    public static <K,V> Map<K,V> getInfoMap() {
        return (Map<K, V>) tls.get();
    }
    
    public static <K,V> V getInfo(K key) {
        Map<K,V> map = (Map<K, V>) tls.get();
        return map == null ? null : map.get(key);
    }
}

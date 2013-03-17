package com.offbynull.peernetic.eventframework.helper;

import java.util.HashMap;
import java.util.Map;

public final class ClassMap<K extends Class<?>, V> {
    private Map<K, V> interalMap;

    public ClassMap() {
        this.interalMap = new HashMap<>();
    }

    public boolean isEmpty() {
        return interalMap.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public V get(K key) {
        if (key == null) {
            throw new NullPointerException();
        }
        
        V res = interalMap.get(key);
        if (res == null) {
            K upperKey = (K) key.getSuperclass();
            if (upperKey != null) {
                res = get(upperKey);
                if (res != null) {
                    put(key, res);
                }
            }
        }
        
        if (res == null) {
            for (Class<?> upperKey : key.getInterfaces()) {
                res = get((K) upperKey);
                if (res != null) {
                    put(key, res);
                    break;
                }
            }
        }
        
        return res;
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    public V put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException();
        }
        return interalMap.put(key, value);
    }

    public V remove(K key) {
        if (key == null) {
            throw new NullPointerException();
        }
        return interalMap.remove(key);
    }
}

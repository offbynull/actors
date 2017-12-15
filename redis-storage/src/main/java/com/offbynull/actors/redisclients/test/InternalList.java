/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
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
package com.offbynull.actors.redisclients.test;

import java.util.LinkedList;
import java.util.List;

final class InternalList {
    private final LinkedList<byte[]> items = new LinkedList<>();

    byte[] rpop() {
        return items.removeLast();
    }

    byte[] lpop() {
        return items.removeFirst();
    }

    void rpush(byte[] e) {
        items.addLast(e);
    }

    void lpush(byte[] e) {
        items.addFirst(e);
    }
    
    List<byte[]> lrange(int start, int end) {
        int revisedStart = start;
        if (revisedStart >= items.size()) {
            revisedStart = items.size() - 1;
        }

        int revisedEnd = end;
        if (revisedEnd >= items.size()) {
            revisedEnd = items.size() - 1;
        }
        
        return new LinkedList<>(items.subList(revisedStart, revisedEnd + 1));
    }

    int size() {
        return items.size();
    }

    boolean isEmpty() {
        return items.isEmpty();
    }
    
}

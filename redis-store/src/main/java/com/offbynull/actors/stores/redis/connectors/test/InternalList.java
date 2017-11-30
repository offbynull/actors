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
package com.offbynull.actors.stores.redis.connectors.test;

import java.util.LinkedList;

final class InternalList {
    private final LinkedList<byte[]> items = new LinkedList<>();

    byte[] rpop() {
        return items.removeFirst();
    }

    byte[] lpop() {
        return items.removeLast();
    }

    void rpush(byte[] e) {
        items.addFirst(e);
    }

    void lpush(byte[] e) {
        items.addLast(e);
    }

    int size() {
        return items.size();
    }

    boolean isEmpty() {
        return items.isEmpty();
    }
    
}

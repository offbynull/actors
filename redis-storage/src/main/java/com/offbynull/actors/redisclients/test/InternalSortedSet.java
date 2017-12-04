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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

final class InternalSortedSet {
    private final HashSet<Item> items = new HashSet<>();
    private final TreeMap<Double, HashSet<Item>> sortedItems = new TreeMap<>();
    
    void put(double score, byte[] data) {
        Item item = new Item(score, data);
        items.add(item);
        
        HashSet<Item> innerItems = sortedItems.computeIfAbsent(score, k -> new HashSet<>());
        innerItems.add(item);
    }
    
    List<Item> getByRank(long start, long end) {
        List<Item> ret = new ArrayList<>();
        
        long counter = 0;
        top:
        for (Entry<Double, HashSet<Item>> entry : sortedItems.entrySet()) {
            for (Item item : entry.getValue()) {
                if (counter < start) {
                    continue;
                }
                
                ret.add(item);
                
                counter++;
                
                if (counter > end) {
                    break top;
                }
            }
        }
        
        return ret;
    }
    
    void removeByRank(long start, long end) {
        long counter = 0;
        Set<Double> scoresToRemove = new HashSet<>();
        top:
        for (Entry<Double, HashSet<Item>> entry : sortedItems.entrySet()) {
            Double score = entry.getKey();
            HashSet<Item> scoreItems = entry.getValue();

            Iterator<Item> it = scoreItems.iterator();
            while (it.hasNext()) {
                Item item = it.next();
                
                if (counter < start) {
                    continue;
                }
                
                items.remove(item);
                it.remove();
                
                counter++;
                
                if (counter > end) {
                    if (scoreItems.isEmpty()) {
                        scoresToRemove.add(score);
                    }
                    break top;
                }
            }
            if (scoreItems.isEmpty()) {
                scoresToRemove.add(score);
            }
        }

        sortedItems.keySet().removeAll(scoresToRemove);
    }
    
    boolean isEmpty() {
        return items.isEmpty();
    }


    static final class Item {
        private final Double score;
        private final byte[] data;

        Item(Double score, byte[] data) {
            this.score = score;
            this.data = data;
        }

        public Double getScore() {
            return score;
        }

        public byte[] getData() {
            return data;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 31 * hash + Arrays.hashCode(this.data);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Item other = (Item) obj;
            if (!Arrays.equals(this.data, other.data)) {
                return false;
            }
            return true;
        }


    }
}

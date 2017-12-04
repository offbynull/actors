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
package com.offbynull.actors.redisclient;

/**
 * Item for a Redis sorted set (score and item).
 * @author Kasra Faghihi
 */
public final class SortedSetItem {

    private final double score;
    private final Object item;

    /**
     * Constructs a {@link SortedSetItem} object.
     * @param score score
     * @param item item
     */
    public SortedSetItem(double score, Object item) {
        this.score = score;
        this.item = item;
    }

    /**
     * Get score.
     * @return score
     */
    public double getScore() {
        return score;
    }

    /**
     * Get item.
     * @param <T> expected type
     * @return item
     * @throws ClassCastException if expected type is not the type stored
     */
    public <T> T getItem() {
        return (T) item;
    }

}

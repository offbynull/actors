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
package com.offbynull.actors.gateways.servlet.stores.redis;

import com.offbynull.actors.address.Address;
import static com.offbynull.actors.redisclient.RedisUtils.toClusterKey;
import org.apache.commons.lang3.Validate;

final class QueueDetails {
    private static final String KEY_PREFIX = "servlet:";

    private static final String IN_QUEUE_OFFSET_SUFFIX = ":in_offset";
    private static final String IN_QUEUE_SUFFIX = ":in";
    private static final String OUT_QUEUE_OFFSET_SUFFIX = ":out_offset";
    private static final String OUT_QUEUE_SUFFIX = ":out";

    private final String inQueueOffsetKey;
    private final String inQueueKey;
    private final String outQueueOffsetKey;
    private final String outQueueKey;
    private final long timeout;
    
    QueueDetails(Address address, long timeout) {
        Validate.notNull(address);
        Validate.isTrue(timeout >= 0L);

        this.outQueueOffsetKey = toClusterKey(KEY_PREFIX, address, OUT_QUEUE_OFFSET_SUFFIX);
        this.outQueueKey = toClusterKey(KEY_PREFIX, address, OUT_QUEUE_SUFFIX);
        this.inQueueOffsetKey = toClusterKey(KEY_PREFIX, address, IN_QUEUE_OFFSET_SUFFIX);
        this.inQueueKey = toClusterKey(KEY_PREFIX, address, IN_QUEUE_SUFFIX);
        this.timeout = timeout;
    }

    public String getInQueueOffsetKey() {
        return inQueueOffsetKey;
    }

    public String getInQueueKey() {
        return inQueueKey;
    }

    public String getOutQueueOffsetKey() {
        return outQueueOffsetKey;
    }

    public String getOutQueueKey() {
        return outQueueKey;
    }

    public long getTimeout() {
        return timeout;
    }
    
}

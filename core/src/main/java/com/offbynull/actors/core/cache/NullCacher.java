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
package com.offbynull.actors.core.cache;

import com.offbynull.actors.core.context.SourceContext;
import com.offbynull.actors.core.shuttle.Address;
import org.apache.commons.lang3.Validate;

/**
 * A cacher implementation that does no caching.
 * @author Kasra Faghihi
 */
public final class NullCacher implements Cacher {

    @Override
    public boolean save(SourceContext ctx) {
        Validate.notNull(ctx);
        Validate.isTrue(ctx.isRoot());
        return false;
    }

    @Override
    public SourceContext restore(Address address) {
        Validate.notNull(address);
        return null;
    }

    @Override
    public void delete(Address address) {
        Validate.notNull(address);
        // do nothing
    }

    @Override
    public void close() {
        // do nothing
    }
}

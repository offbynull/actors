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
package com.offbynull.actors.core.gateway.web;

import com.offbynull.actors.core.shuttle.Message;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class SystemToHttpBundle {
    private final long systemToHttpOffset;
    private final long httpToSystemOffset;
    private final List<Message> messages;

    public SystemToHttpBundle(long systemToHttpOffset, long httpToSystemOffset, List<Message> messages) {
        Validate.isTrue(systemToHttpOffset >= 0);
        Validate.isTrue(httpToSystemOffset >= 0);
        Validate.notNull(messages);
        Validate.noNullElements(messages);

        this.systemToHttpOffset = systemToHttpOffset;
        this.httpToSystemOffset = httpToSystemOffset;
        this.messages = messages;
    }

    public long getSystemToHttpOffset() {
        return systemToHttpOffset;
    }

    public long getHttpToSystemOffset() {
        return httpToSystemOffset;
    }

    public List<Message> getMessages() {
        return Collections.unmodifiableList(messages);
    }
    
}

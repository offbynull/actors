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
package com.offbynull.actors.core.gateway.servlet;

import com.offbynull.actors.core.shuttle.Message;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class HttpToSystemBundle {
    private final String httpAddressId;
    private final int httpToSystemOffset;
    private final int systemToHttpOffset;
    private final List<Message> messages;

    public HttpToSystemBundle(String httpAddressId, int httpToSystemOffset, int systemToHttpOffset, List<Message> messages) {
        Validate.notNull(httpAddressId);
        Validate.isTrue(httpToSystemOffset >= 0);
        Validate.isTrue(systemToHttpOffset >= 0);
        Validate.notNull(messages);
        Validate.noNullElements(messages);

        this.httpAddressId = httpAddressId;
        this.httpToSystemOffset = httpToSystemOffset;
        this.systemToHttpOffset = systemToHttpOffset;
        this.messages = new ArrayList<>(messages);
    }

    public String getHttpAddressId() {
        return httpAddressId;
    }

    public int getHttpToSystemOffset() {
        return httpToSystemOffset;
    }

    public int getSystemToHttpOffset() {
        return systemToHttpOffset;
    }

    public List<Message> getMessages() {
         return Collections.unmodifiableList(messages);
    }
}

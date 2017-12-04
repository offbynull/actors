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
package com.offbynull.actors.gateways.servlet;

import com.offbynull.actors.shuttle.Message;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.list.UnmodifiableList;
import static org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList;
import org.apache.commons.lang3.Validate;

final class RequestBlock {
    private final String id;
    private final UnmodifiableList<Message> messages;

    RequestBlock(String id, List<Message> messages) {
        Validate.notNull(id);
        Validate.notNull(messages);
        Validate.noNullElements(messages);

        this.id = id;
        this.messages = (UnmodifiableList<Message>) unmodifiableList(new ArrayList<>(messages));
    }

    public String getId() {
        return id;
    }

    public UnmodifiableList<Message> getMessages() {
        return messages;
    }

}

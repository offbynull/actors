/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.actor;

import org.apache.commons.lang3.Validate;

/**
 * Incoming message.
 * @author Kasra Faghihi
 */
public final class Incoming {
    private Object content;
    private Endpoint source;
    
    /**
     * Constructs a {@link Incoming} object.
     * @param content content
     * @param source source
     * @throws NullPointerException if any arguments are {@code null}
     */
    public Incoming(Object content, Endpoint source) {
        Validate.notNull(content);
        Validate.notNull(source);
        
        this.content = content;
        this.source = source;
    }
    
    /**
     * Get the content within this message.
     * @return content
     */
    public final Object getContent() {
        return content;
    }
    
    /**
     * Source of the request message. Replies can be sent to this.
     * @return source of the request message
     */
    public Endpoint getSource() {
        return source;
    }
}

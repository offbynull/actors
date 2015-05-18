/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.core.simulator;

import java.time.Instant;

final class PullFromMessageSourceEvent extends Event {

    private final MessageSource messageSource;
    
    public PullFromMessageSourceEvent(MessageSource messageSource, Instant triggerTime, long sequenceNumber) {
        super(triggerTime, sequenceNumber);
        this.messageSource = messageSource;
    }

    public MessageSource getMessageSource() {
        return messageSource;
    }

    @Override
    public String toString() {
        return "PullFromMessageSourceEvent{" + super.toString() + ", messageSource=" + messageSource + '}';
    }
    
}

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
package com.offbynull.actors.core.gateways.log;

import com.offbynull.actors.core.shuttle.Address;
import com.offbynull.actors.core.shuttle.Message;
import com.offbynull.actors.core.shuttles.simple.Bus;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LogRunnable implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(LogRunnable.class);

    private final Bus bus;
    private final AtomicBoolean shutdownFlag;

    LogRunnable(Bus bus, AtomicBoolean shutdownFlag) {
        Validate.notNull(bus);
        Validate.notNull(shutdownFlag);
        this.bus = bus;
        this.shutdownFlag = shutdownFlag;
    }

    @Override
    public void run() {
        LOG.debug("Log gateway started");
        try {
            while (!shutdownFlag.get()) {
                // Poll for new messages
                List<Object> incomingObjects = bus.pull();

                Validate.notNull(incomingObjects); // sanity checks
                Validate.noNullElements(incomingObjects);

                for (Object incomingObj : incomingObjects) {
                    if (!(incomingObj instanceof Message)) {
                        continue;
                    }

                    Message message = (Message) incomingObj;
                    Address src = message.getSourceAddress();
                    Address dst = message.getDestinationAddress();
                    Object payload = message.getMessage();

                    if (!(payload instanceof LogMessage)) {
                        continue;
                    }

                    LogMessage logMsg = (LogMessage) payload;
                    
                    String msg = "{} - " + logMsg.getMessage();
                    
                    Object[] origArgs = logMsg.getArguments();
                    Object[] args = new Object[origArgs.length + 1];
                    System.arraycopy(origArgs, 0, args, 1, origArgs.length);
                    args[0] = src.toString();

                    switch (logMsg.getType()) {
                        case TRACE:
                            LOG.trace(msg, args);
                            break;
                        case DEBUG:
                            LOG.debug(msg, args);
                            break;
                        case INFO:
                            LOG.info(msg, args);
                            break;
                        case WARN:
                            LOG.warn(msg, args);
                            break;
                        case ERROR:
                            LOG.error(msg, args);
                            break;
                        default:
                            throw new IllegalStateException(); // this should never happen
                    }
                }
            }
        } catch (InterruptedException ie) {
            LOG.debug("Log gateway interrupted");
            Thread.interrupted();
        } catch (RuntimeException re) {
            LOG.error("Internal error encountered", re);
        } finally {
            shutdownFlag.set(true);
            bus.close();
        }
    }

}

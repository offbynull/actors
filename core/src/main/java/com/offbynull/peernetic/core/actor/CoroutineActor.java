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
package com.offbynull.peernetic.core.actor;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import org.apache.commons.lang3.Validate;

/**
 * An actor implementation that delegates to a coroutine. Nearly all actor implementations, except for the most rudimentary, will end up
 * requiring some sort of complex state machine logic to be written by hand. Writing your actor logic as a coroutine avoids the need for
 * these state machines.
 * <p>
 * For example... imagine the following scenario. We're expecting 10 messages to come in. Once a message comes in, if that message has a
 * certain flag set, it means that we expect more messages of a different type to immediately follow it. If that flag isn't set, we process
 * the message and wait for the next one.
 * <p>
 * Implemented as a coroutine, the logic would look like this:
 * <pre>
 * for (int i = 0; i &lt; 10; i++) {
 *    cnt.suspend();
 *    Message msg = context.getIncomingMessage();
 *    if (msg.isMultipart()) {
 *       for (int j = 0; j &lt; msg.numberOfChunks(); j++) {
 *           cnt.suspend();
 *           MessageChunk msgChunk = context.getIncomingMessage();
 *           processMultipartMessageChunk(msg, msgChunk);
 *       }
 *    } else {
 *       processMessage(msg);
 *    }
 * }
 * </pre>
 * Implemented as a normal actor, the logic would look similar to this:
 * <pre>
 * //
 * // Keep in mind that, due to the need to retain state between calls to onStep(), all variables have essentially become fields.
 * // 
 * switch (state) {
 *     case START:
 *         i = 0;
 *         state = OUTER_LOOP;
 *     case OUTER_LOOP:
 *         if (i == 10) {
 *             state = END;
 *             return;
 *         }
 *         i++;
 *         msg = context.getIncomingMessage();
 *         if (msg.isMultipart()) {
 *            state = INNER_LOOP;
 *         } else {
 *            process(msg);
 *         }
 *         return;
 *     case INNER_LOOP:
 *         msgChunk = context.getIncomingMessage();
 *         if (i == msg.getNumberOfChunks()) {
 *             state = OUTER_LOOP;
 *             return;
 *         }
 *         processMultipartMessageChunk(msg, msgChunk);
 *         return;
 *     case END:
 *         return;
 * }
 * </pre>
 * Even though they technically perform the same set of steps, one is way easier to follow/change than the other. For more information, see
 * <a href='https://github.com/offbynull/coroutines'>https://github.com/offbynull/coroutines</a>.
 * @author Kasra Faghihi
 */
public final class CoroutineActor implements Actor {
    private CoroutineRunner coroutineRunner;
    private boolean executing;

    /**
     * Constructs a {@link CoroutineActor} object.
     * @param task coroutine to delegate to
     * @throws NullPointerException if any argument is {@code null}
     */
    public CoroutineActor(Coroutine task) {
        Validate.notNull(task);
        coroutineRunner = new CoroutineRunner(task);
        executing = true;
    }

    @Override
    public boolean onStep(Context context) throws Exception {
        // if continuation has ended, ignore any further messages
        if (executing) {
            coroutineRunner.setContext(context); // set once
            executing = coroutineRunner.execute();
        }
        
        return executing;
    }
    
}

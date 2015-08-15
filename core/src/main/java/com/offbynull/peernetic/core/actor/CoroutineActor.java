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

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import org.apache.commons.lang3.Validate;

/**
 * An {@link Actor} implementation that delegates to a {@link Coroutine}. Except for the most rudimentary, nearly all actor implementations
 * require that execution state be retained between incoming messages and/or require multiple threads of execution. Writing
 * your actor as a coroutine avoids the need to handle this through convoluted hand-written state machine logic.
 * <p>
 * For example, imagine the following scenario: an actor expects 10 messages to arrive. For each of those 10 that arrive, if the message
 * has a multi-part flag set, we expect a variable number of other "chunk" messages to immediately follow it. Implemented as a coroutine,
 * the logic would be written similar to this:
 * <pre>
 * for (int i = 0; i &lt; 10; i++) {
 *    Context ctx = (Context) cnt.getContext();
 * 
 *    Message msg = context.getIncomingMessage();
 *    if (msg.isMultipart()) {
 *       for (int j = 0; j &lt; msg.numberOfChunks(); j++) {
 *           cnt.suspend();
 *           MessageChunk msgChunk = ctx.getIncomingMessage();
 *           processMultipartMessageChunk(msg, msgChunk);
 *       }
 *    } else {
 *       processMessage(msg);
 *    }
 * 
 *    cnt.suspend();
 * }
 * </pre>
 * However, if it were implemented as a basic actor, the logic would have to be written in a much more convoluted manner:
 * <pre>
 * //
 * // Keep in mind that, due to the need to retain state between calls to onStep(), all variables have become fields.
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
 *         throw new IllegalStateException();
 * }
 * </pre>
 * <p>
 * <b>Important note</b>: You can access your actor's {@link Context} object in your coroutine via {@link Continuation#getContext() }.
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

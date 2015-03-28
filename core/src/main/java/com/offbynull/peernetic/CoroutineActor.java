package com.offbynull.peernetic;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.Endpoint;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class CoroutineActor implements Actor {
    private Context context;
    private CoroutineRunner coroutineRunner;
    private boolean executing;

    public CoroutineActor(Coroutine task) {
        Validate.notNull(task);
        context = new Context();
        coroutineRunner = new CoroutineRunner(task);
        executing = true;

        coroutineRunner.setContext(context); // set once
    }

    @Override
    public void onStep(Instant time, Endpoint source, Object message) throws Exception {
        // if continuation has ended, ignore any further messages
        if (executing) {
            context.time = time;
            context.source = source;
            context.message = message;
            
            executing = coroutineRunner.execute();
        }
    }

    public boolean isFinished() {
        return executing;
    }
    
    public static final class Context {
        private Instant time;
        private Endpoint source;
        private Object message;

        public Instant getTime() {
            return time;
        }

        public Endpoint getSource() {
            return source;
        }

        public Object getMessage() {
            return message;
        }

        public void setTime(Instant time) {
            this.time = time;
        }

        public void setSource(Endpoint source) {
            this.source = source;
        }

        public void setMessage(Object message) {
            this.message = message;
        }
        
        
    }
}

# Actors (pre-alpha)

<p align="center"><img src ="logo.png" alt="Actors logo" /></p>

The Actors project is a lightweight Java actor framework that works off the ide of coroutines/fibers. Why use Actors over other actor frameworks? 

 * Designed first and foremost with developer productivity in mind
 * Seamlessly distribute actors
 * Seamlessly serialize/store actors
 * Avoid complicated FSM logic via coroutines
 * Run actors in simulations (great for unit testing)

More information on the topic of actors can be found on the following pages:

* [Programmers Stack Exchange: How is the actor model used?](http://programmers.stackexchange.com/questions/99501/how-is-the-actor-model-used)
* [Wikipedia: Actor model](https://en.wikipedia.org/wiki/Actor_model)

## Table of Contents

 * [Hello World Example](#hello-world-example)
 * [Concepts](#concepts)
   * [Actors](#actors)
   * [Gateways](#gateways)

## Hello World Example

In the following example, when the actor receives "Hi", it'll reply with "Hi back to you!". Note that this example uses [Coroutines](https://github.com/offbynull/coroutines), and as such you'll need to make use of the coroutines plugin to instrument your code.

```java
// Create coroutine actor that echos back incoming messages
Coroutine echoActor = (Continuation cnt) -> {
    Context ctx = (Context) cnt.getContext();
    ctx.allow();

    do {
        Object msg = ctx.in();
        if ("Hi".equals(msg)) {
            ctx.addOutgoingMessage("Hi back to you!");
        }

        cnt.suspend();
    } while (true);
};

// Create the actor runner, and direct gateway.
ActorRunner actorRunner = new ActorRunner("actors"); // container for actors
DirectGateway directGateway = new DirectGateway("direct"); // gateway that allows interfacing with actors/gateways from normal java code

// Bind the runner and the direct gateway so that they can send messages to each other
actorRunner.addOutgoingShuttle(directGateway.getIncomingShuttle());
directGateway.addOutgoingShuttle(actorRunner.getIncomingShuttle());

// Add echoer to actor runner
actorRunner.addActor("echoer", echoActor);

// Send "Hi!" to actor and print out response
directGateway.writeMessage("actors:echoer", "Hi!");
String response = directGateway.readPayload();
System.out.println(response);
```

## Concepts

The Actors project has two basic primitives in its implementation of the actor model: [Actors](#actors) and [Gateways](#gateways). Each primitive has an address associated with it, and primitives communicate with each other by passing messages to addresses. Messages sent between primitives must be immutable.

### Actors

An Actor is a coroutine which can only communicate with the outside world through message-passing.

Actors must adhere to the following constraints:

 1. **Do not expose any internal state.** Unlike traditional Java objects, actors should not provide any publicly accessibly methods or fields that expose or change their state. If an outside component needs to know or change the state of an actor, it must request it via message-passing.
 1. **Do not share state.** Actors must only ever access/change their own internal state, meaning that an actor must not share any references with other outside objects (unless those references are to immutable objects). For example, an actor shouldn't have a reference to a ConcurrentHashMap that's being shared with other objects. As stated in the previous constraint, communication must be done via message-passing.
 1. **Do not block** for I/O, long running operations, thread synchronization, or anything else. Multiple actors may be running in the same Java thread. As such, if an actor were to block for any reason, it may prevent other actors from processing messages in a timely manner.
 1. **Do not directly access time.** Actors must use the time supplied to them via the Context rather than making calls to Java's date and time APIs (e.g. Instant or System.currentTimeMillis()).
 1. **Only make use of serializable objects.** Actors must only make use of objects that are serializable. This includes fields on the actor as well as local variables and items on the operand stack. Doing so ensures that we can easily store and load the actor (should the choice be made to do so).

Following the above implementation rules means that outside of receiving and sending messages, an actor is fully isolated. This isolation helps with concurrency (no shared state, so you don't have to worry about synchronizing state) and transparency (it doesn't matter if you're passing messages to a component that's remote or local, the underlying transport mechanism should be transparent).

#### Actor FSM logic

Except for the most rudimentary, nearly all actor implementations require execution state be retained between incoming messages and/or require multiple threads of execution. Writing your actor as a [coroutine](https://github.com/offbynull/coroutines) avoids the need to handle this through convoluted hand-written FSM logic.

For example, imagine the following scenario: an actor expects 10 messages to arrive. For each of those 10 that arrive, if the message has a multi-part flag set, we expect a variable number of other "chunk" messages to immediately follow it. Implemented as a coroutine, the logic would be written similar to this:

```java
public final class CustomActor implements Coroutine {
    public void run(Continuation cnt) {
        Context ctx = (Context) cnt.getContext();

        for (int i = 0; i < 10; i++) {
           Message msg = context.getIncomingMessage();
           if (msg.isMultipart()) {
              for (int j = 0; j < msg.numberOfChunks(); j++) {
                  cnt.suspend();
                  MessageChunk msgChunk = ctx.getIncomingMessage();
                  processMultipartMessageChunk(msg, msgChunk);
              }
           } else {
              processMessage(msg);
           }

           cnt.suspend();
        }
    }
}
```

However, if it were implemented as a basic actor, the logic would have to be written in a much more convoluted manner:

```java
public final class CustomActor implements Actor {
    //
    // Keep in mind that, due to the need to retain state between calls to onStep(), all variables have become fields.
    // 
    public boolean onStep(Context ctx) {
        switch (state) {
            case START:
                i = 0;
                state = OUTER_LOOP;
            case OUTER_LOOP:
                if (i == 10) {
                    state = END;
                    return false;
                }
                i++;
                msg = context.getIncomingMessage();
                if (msg.isMultipart()) {
                   state = INNER_LOOP;
                } else {
                   process(msg);
                }
                return true;
            case INNER_LOOP:
                msgChunk = context.getIncomingMessage();
                if (i == msg.getNumberOfChunks()) {
                    state = OUTER_LOOP;
                    return true;
                }
                processMultipartMessageChunk(msg, msgChunk);
                return true;
            case END:
                throw new IllegalStateException();
        }
    }
}
```

#### Actor Storage

Coroutines/fibers store execution state in software when they suspend. That means that your actor can be stored to and loaded from off-site storage (e.g. a database).

INSERT CACHING EXAMPLE HERE

#### Actor Distribution

INSERT DISTRIBUTION EXAMPLE HERE

### Gateways

A Gateway, like an Actor, communicates with other components through message-passing, but isn't bound by any of the same rules as Actors. Gateways are mainly used to interface with third-party components (e.g. relational database). As such, it's perfectly acceptable for a gateway to expose internal state, share state, perform I/O, perform thread synchronization, or otherwise block.
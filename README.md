# Actors (pre-alpha)

<p align="center"><img src ="logo.png" alt="Actors logo" /></p>

The Actors project is a lightweight Java actor framework that greatly simplifies the design and development of horizontally scalable software. Why use Actors over other actor frameworks? 

 * Actors as lightweight fibers/coroutines
 * Seamless distribution of actors
 * Seamless checkpointing of actors
 * Seamless versioning of actors

What does all this mean? *Distribution* means your actors will seamlessly execute across a cluster of servers. You can add and remove servers at will, and server crashes won't effect the execution of your actors. *Checkpointing* makes sure that if your actors enter into a bad state, they will automatically revert to the last known good state (as specified by you) and be notified of the problem. *Versioning* allows you to change your actor's logic on the fly, without breaking compatibility with older versions of that actor. Fix bugs and add features without stopping your service. You can even have your servers running different versions of the same actor.

The Actors project gives you everything you need to write highly resilient/elastic/scalable systems, but without all the headaches. 

## Example

This is a simple "Hello World" style example. It doesn't show many of the core features that make the Actors project what it is, but more in-depth material and screencasts are in the works.

First, create your actor. The Actors project uses the [Coroutines](https://github.com/offbynull/coroutines) project, so you'll need to use the coroutines plugin to instrument your actors.

```java
public final class SimpleActor implements Coroutine {    
    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        Object msg;
        
        // Display the priming message that's initially sent to this actor
        msg = ctx.in();
        ctx.logInfo("Priming message: {}", msg);
        
        // Allow any address to communicate with this actor
        ctx.allow();
        
        while (true) {
            // Add a checkpoint -- if no activity in 60 seconds send a message notifying as such.
            ctx.checkpoint("ACTOR HASN'T BEEN HIT IN OVER 60 SECONDS -- CHECKPOINT TRIGGERED", 60 * 1000L);
            // Add a 1000L timer to echo a message back after 1 second.
            ctx.timer(1000L, "timer!");
            
            // Wait for next message
            cnt.suspend();
            
            // Print message
            msg = ctx.in();
            ctx.logInfo("Got message: {}", msg);
        }
    }
}
```

Next, set up a server to run instances of your actor. You can spawn as many instances of the server as you want.

```java
public final class SimpleRedisStoreTest {
    private static final int WORKER_COUNT = 1000;
    private static final int THREAD_COUNT = 100;
    private static final int REDIS_POOL_COUNT = THREAD_COUNT;
    private static final int REDIS_QUEUE_COUNT = 10;
    
    public static void main(String[] args) throws Exception {
        // Start actor system backed by Redis
        RedisStore store = RedisStore.create(
                "actor",
                "192.168.56.101",
                6379,
                REDIS_POOL_COUNT,
                REDIS_QUEUE_COUNT);

        ActorSystem actorSystem = ActorSystem.builder()
                .withLogGateway()
                .withActorGateway(THREAD_COUNT, store)
                .build();
        
        // Add 1000 new actors into the system
        ActorGateway actorGateway = actorSystem.getActorGateway();
        for (int i = 0; i < WORKER_COUNT; i++) {
            String name = InetAddress.getLocalHost().getHostName() + "_" + i;
            Coroutine coroutine = new SimpleActor();

            actorGateway.addActor(name, coroutine, "start");
        }
        
        // Block
        actorSystem.join();
    }
}
```
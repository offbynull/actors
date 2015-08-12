# Peernetic Actor Framework

<p align="center"><img src ="../gh-pages/logo.png" alt="Peernetic logo" /></p>

Peernetic is a lightweight Java actor framework that was designed first and foremost with P2P algorithm/application development in mind. While Peernetic can be used as a generic actor framework, it provides features that remove some of the pain from distributed P2P development.

Peernetic's high-level features include:

* **First-class support for coroutines** - Retain your actor's execution state between incoming messages and/or have multiple threads of execution within your actor. [Coroutines](https://github.com/offbynull/coroutines) allow you to suspend the execution of logic within your actor without the need for tedious hand-written state machine logic.
* **Networking** - Send messages transparently over a network. The UDP gateway allows you to send messages over UDP, and the UDP simulator actor allows you to mimic a UDP-like environment locally where you control the network conditions (e.g. packet loss. packet duplication, latency, jitter, etc..) to see how your actor's logic reacts.
* **Visualizer** - Visualize directed graphs with ease. The visualizer gateway makes it easy to visualize graph-related information, such as P2P network overlay information, in real-time.
* **Simulator** - Deterministicly simulate many actors interacting with each other in faster than real-time. The simulator makes it easy to write deterministic and reproducible tests for your actor logic. It also allows you to see how your actor deals with common P2P issues - network churn, network partitioning, high latency, high packet-loss, etc.

More information on the topic of actors and P2P can be found on the following pages:

* [Wikipedia: Actor model](https://en.wikipedia.org/wiki/Actor_model)
* [Programmers Stack Exchange: How is the actor model used?](http://programmers.stackexchange.com/questions/99501/how-is-the-actor-model-used)
* [Wikipedia: Peer-to-peer](https://en.wikipedia.org/wiki/Peer-to-peer)

*Please note that Peernetic is currently in beta.*

## Concepts

If you aren't familiar with the actor model and its role in concurrent/distributed computing, there's a good introduction available on [Programmers Stack Exchange](http://programmers.stackexchange.com/questions/99501/how-is-the-actor-model-used) and [Wikipedia](http://en.wikipedia.org/wiki/Actor_model).

There are two primitives in Peernetic's implementation of the actor model: Actors and Gateways. Each primitive has an address associated with it, and primitives communicate with each other by passing messages to addresses. Messages sent between primitives must be immutable and should be serializable.

### Actors

An Actor is an isolated class who's only method of communicating with the outside world is through message-passing.

Actors must adhere to the following constraints:

 1. **Do not expose any internal state.** Unlike traditional Java objects, actors should not provide any publicly accessibly methods or fields that expose or change their state. If an outside component needs to know or change the state of this actor, it must request it via message-passing.
 1. **Do not share state.** Actors must only ever access/change their own internal state, meaning that an actor must not share any references with other outside objects (unless those references are to immutable objects). For example, an actor shouldn't have a reference to a ConcurrentHashMap that's being shared with other objects. As stated in the previous constraint, communication must be done via message-passing.
 1. **Do not block,** whether it's for I/O, long running operations, thread synchronization, or otherwise. Multiple actors may be running in the same Java thread. As such, if an actor were to block for any reason, it may prevent other actors from processing messages in a timely manner.
 1. **Do not directly access time.** Actors must use the time supplied to them via the Context rather than making calls to Java's date and time APIs (e.g. Instant or System.currentTimeMillis()).

Following the above implementation rules means that, outside of receiving and sending messages, an actor is fully isolated. This isolation helps with concurrency (no shared state, so we don't have to worry about synchronizing state) and transparency (it doesn't matter if you're passing messages to a component that's remote or local, the underlying transport mechanism should make it transparent).

### Gateways

A Gateway, like an Actor, communicates with other components through message-passing, but isn't bound by any of the same rules as Actors. Gateways are mainly used to interface with third-party components that can't be communicated with via message-passing. As such, it's perfectly acceptable for a gateway to expose internal state, share state, perform I/O, perform thread synchronization, or otherwise block.

Peernetic provides the following gateway implementations:

 * TimerGateway -- Echoes back messages after a certain duration of time.
 * LogGateway -- Logs to SLF4J.
 * RecorderGateway -- Saves incoming messages to a file.
 * ReplayerGateway -- Replays events saved by a RecorderGateway.
 * DirectGateway -- Allows normal Java code to send and receive messages to other actors/gateways.
 * UdpGateway -- Sends and receives messages over UDP.
 * VisualizerGateway -- Displays 2D directed graphs.

Gateways run in their own isolated thread / threadpools.

### Differences

Unlike some other actor frameworks ...

 1. **Peernetic doesn't provide a "central directory" for actors/gateways.** Before a primitive can send messages to another primitive, it needs to be linked to that other primitive. This is done by binding Shuttles. If you've used other actor frameworks before, shuttles are similar to mailboxes.
 1. **Peernetic doesn't provide guarantees around message delivery.** The underlying transport mechanism is what determines guarantees around message delivery. Actors/Gateways communicating locally will have messages that are delivered and in ordered. But, if messages are piped over a volatile transport (e.g. UDP), nothing is guaranteed -- P2P algorithms should be able to operate in the face of message loss, message duplication, jitter, latency, etc..
 1. **Peernetic doesn't use Futures/Promises for anything.**

## Examples

It's highly recommended that you go through the [concepts primer](#concepts) before digging in to these examples.

### Simple Example

The following example reads messages from System.in and forwards those messages to an actor which ...

 1. logs the message
 1. echos the message back to the sender

Note that this example uses [Coroutines](https://github.com/offbynull/coroutines), and as such you'll need to make use of the coroutines plugin to instrument your code.

```java
// Create coroutine actor that logs and echos back incoming messages
Coroutine echoerActor = (cnt) -> {
    Context ctx = (Context) cnt.getContext();
    
    // First message is the priming message. It should be the address to the logger.
    final Address loggerAddress = (Address) ctx.getIncomingMessage();
    cnt.suspend();

    // All messages after the first message are messages that we should log and echo back.
    do {
        Object msg = ctx.getIncomingMessage();
        Address srcAddress = ctx.getSource();
        ctx.addOutgoingMessage(loggerAddress, LogMessage.debug("Received a message: {}", msg));
        ctx.addOutgoingMessage(srcAddress, "Echoing back " + msg);
        cnt.suspend();
    } while (true);
};

// Create the actor runner, logger gateway, and direct gateway.
ActorRunner actorRunner = new ActorRunner("actors"); // container for actors
LogGateway logGateway = new LogGateway("log"); // gateway that logs to slf4j
DirectGateway directGateway = new DirectGateway("direct"); // gateway that allows allows interfacing with actors/gateways from normal java code

// Allow the actor runner to send messages to the log gateway
actorRunner.addOutgoingShuttle(logGateway.getIncomingShuttle());

// Allow the actor runner and the direct gateway to send messages to eachother
actorRunner.addOutgoingShuttle(directGateway.getIncomingShuttle());
directGateway.addOutgoingShuttle(actorRunner.getIncomingShuttle());

// Add the coroutine actor and prime it with a hello world message
actorRunner.addCoroutineActor("echoer", echoerActor, Address.of("log"));


Scanner inScanner = new Scanner(System.in);
while(inScanner.hasNextLine()) {
    // Read next line and forward to actor
    String input = inScanner.nextLine();
    directGateway.writeMessage(Address.fromString("actors:echoer"), input);
    
    // Wait for response from actor and print out
    String response = (String) directGateway.readMessages().get(0).getMessage();
    System.out.println(response);
}
```

Example output:

```
hello! :)
22:00:28.817 DEBUG c.o.p.core.gateways.log.LogRunnable - actors:echoer - Received a message: hello! :)
Echoing back hello! :)
test123
22:00:44.111 DEBUG c.o.p.core.gateways.log.LogRunnable - actors:echoer - Received a message: test123
Echoing back test123
```

### Real-world Examples

Peernetic comes with 3 real-world examples, located in the [examples project](examples/src/main/java/com/offbynull/peernetic/examples/). Each of those examples comes with 5 executable Java classes...

 * **RealtimeDirect** - Actors run locally and communicate directly.
 * **RealtimeViaUdpGateway** - Actors run locally and communicate via UDP.
 * **RealtimeViaUdpSimulator** - Actors run locally and communicate via simulated UDP.
 * **SimulationDirect** - Actors run in a simulation and communicate directly. 
 * **SimulationViaUdpSimulator** - Actors run in a simulation and communicate via simulated UDP.

Realtime executables may require user input.

Simulation executables don't require user input because they run pre-configured scenarios. Simulation executables capture messages intended for a visualizer, and these messages are replayed to an actual visualizer once the simulation is complete.

#### Unstructured Example

[Unstructured](examples/src/main/java/com/offbynull/peernetic/examples/unstructured) is an implementation of an unstructured mesh network. From [Wikipedia](https://en.wikipedia.org/wiki/Peer-to-peer#Unstructured_networks): *Unstructured peer-to-peer networks do not impose a particular structure on the overlay network by design, but rather are formed by nodes that randomly form connections to each other. (Gnutella, Gossip, and Kazaa are examples of unstructured P2P protocols).*

Both realtime and simulation unstructured examples don't require any user input. They run the same pre-configured scenario: nodes are gradually created and added in to the network (100 nodes in total). Note that node 0 starts as the bootstrap node, and all other nodes (1 to 99) will enter the network via node 0 when they start.

![Unstructured Example Screenshot](../gh-pages/unstructured_example1.png)

#### Chord Example

[Chord](examples/src/main/java/com/offbynull/peernetic/examples/chord) is an implementation of Chord DHT's overlay network. From [Wikipedia](https://en.wikipedia.org/wiki/Chord_(peer-to-peer)): *In computing, Chord is a protocol and algorithm for a peer-to-peer distributed hash table. A distributed hash table stores key-value pairs by assigning keys to different computers (known as "nodes"); a node will store the values for all the keys for which it is responsible. Chord specifies how keys are assigned to nodes, and how a node can discover the value for a given key by first locating the node responsible for that key.*

Realtime Chord examples require user input. When the example starts, a console-like window is presented. The following sequence of commands will create a 64-node Chord cluster and bring it online. Note that node 0 will start as the bootstrap node, and all other nodes (1 to 63) will enter the network via node 0 when they start.

```
64
boot 0
start 1 63 0
```

Simulation Chord examples don't require any user input. They run pre-configured scenarios.

![Chord Example Screenshot](../gh-pages/chord_example1.png)

#### Raft Example

[Raft](examples/src/main/java/com/offbynull/peernetic/examples/raft) is an implementation of the Raft distributed consensus algorithm. From [Wikipedia](https://en.wikipedia.org/wiki/Raft_(computer_science)): *Raft is a consensus algorithm designed as an alternative to Paxos. It was meant to be more understandable than Paxos by means of separation of logic, but it is also formally proven safe and offers some new features. Raft offers a generic way to distribute a state machine across a cluster of computing systems, ensuring that each node in the cluster agrees upon the same series of state transitions.*

Realtime Raft examples require user input. When the example starts, a console-like window is presented. The following sequence of commands will create a 5-node Raft cluster and bring it online. Note that node 5 is the client that's reading from and writing to the cluster, while all other nodes (0 to 4) are Raft servers.

```
5
start 0 4
```

Simulation Raft examples don't require any user input. They run pre-configured scenarios.

![Raft Example Screenshot](../gh-pages/raft_example1.png)
# Peernetic Actor Framework

<p align="center"><img src ="../gh-pages/logo.png" alt="Peernetic logo" /></p>

 * [Introduction](#introduction)
   * [Hello World Example](#hello-world-example)
   * [Concepts](#concepts)
     * [Actors](#actors)
     * [Gateways](#gateways)
     * [Differences](#differences)
 * [Examples](#examples)
   * [Unstructured Mesh Example](#unstructured-mesh-example)
     * [What is an Unstructured Mesh?](#what-is-an-unstructured-mesh)
     * [Running the Example](#running-the-example)
   * [Chord Distributed Hash Table Example](#chord-distrubted-hash-table-example)
     * [What is Chord?](#what-is-chord)
     * [Running the Example](#running-the-example-1)
   * [Raft Distributed Consensus Example](#raft-distributed-consensus-example)
     * [What is Raft?](#what-is-raft)
     * [Running the Example](#running-the-example-2)

##Introduction

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

### Hello World Example

In the following example, when the actor receives "Hi", it'll reply with "Hi back to you!". Note that this example uses [Coroutines](https://github.com/offbynull/coroutines), and as such you'll need to make use of the coroutines plugin to instrument your code.

```java
// Create coroutine actor that echos back incoming messages
Coroutine echoerActor = (cnt) -> {
    Context ctx = (Context) cnt.getContext();

    // All messages after the first message are messages that we should log and echo back.
    do {
        Object msg = ctx.getIncomingMessage();
        Address srcAddress = ctx.getSource();

        if ("Hi".equals(msg)) {
            ctx.addOutgoingMessage(srcAddress, "Hi back to you!");
            cnt.suspend();
        }
    } while (true);
};

// Create the actor runner, and direct gateway.
ActorRunner actorRunner = new ActorRunner("actors"); // container for actors
DirectGateway directGateway = new DirectGateway("direct"); // gateway that allows interfacing with actors/gateways from normal java code

// Bind the runner and the direct gateway so that they can send messages to eachother
actorRunner.addOutgoingShuttle(directGateway.getIncomingShuttle());
directGateway.addOutgoingShuttle(actorRunner.getIncomingShuttle());

// Add echoer to actor runner
actorRunner.addActor("echoer", echoerActor);

// Send "Hi!" to actor and print out response
directGateway.writeMessage(Address.fromString("actors:echoer"), "Hi!");
String response = (String) directGateway.readMessages().get(0).getMessage();
System.out.println(response);
```

Example output:

```
hello world! :)
Echoing back hello world! :)
```

### Concepts

If you aren't familiar with the actor model and its role in concurrent/distributed computing, there's a good introduction available on [Programmers Stack Exchange](http://programmers.stackexchange.com/questions/99501/how-is-the-actor-model-used) and [Wikipedia](http://en.wikipedia.org/wiki/Actor_model).

There are two primitives in Peernetic's implementation of the actor model: [Actors](#actors) and [Gateways](#gateways). Each primitive has an address associated with it, and primitives communicate with each other by passing messages to addresses. Messages sent between primitives must be immutable and should be serializable.

![Unstructured Example Screenshot](../gh-pages/primitives_class_diagram.png)

#### Actors

An Actor is an isolated class who's only method of communicating with the outside world is through message-passing.

Actors must adhere to the following constraints:

 1. **Do not expose any internal state.** Unlike traditional Java objects, actors should not provide any publicly accessibly methods or fields that expose or change their state. If an outside component needs to know or change the state of this actor, it must request it via message-passing.
 1. **Do not share state.** Actors must only ever access/change their own internal state, meaning that an actor must not share any references with other outside objects (unless those references are to immutable objects). For example, an actor shouldn't have a reference to a ConcurrentHashMap that's being shared with other objects. As stated in the previous constraint, communication must be done via message-passing.
 1. **Do not block,** whether it's for I/O, long running operations, thread synchronization, or otherwise. Multiple actors may be running in the same Java thread. As such, if an actor were to block for any reason, it may prevent other actors from processing messages in a timely manner.
 1. **Do not directly access time.** Actors must use the time supplied to them via the Context rather than making calls to Java's date and time APIs (e.g. Instant or System.currentTimeMillis()).

Following the above implementation rules means that, outside of receiving and sending messages, an actor is fully isolated. This isolation helps with concurrency (no shared state, so we don't have to worry about synchronizing state) and transparency (it doesn't matter if you're passing messages to a component that's remote or local, the underlying transport mechanism should make it transparent).

##### Coroutine Actors

Nearly all actor implementations, except for the most rudimentary, will end up requiring that execution state be retained between incoming messages and/or requiring multiple threads of execution. Writing your actor as a [coroutine](https://github.com/offbynull/coroutines) avoids the need to handle this through convoluted hand-written state machine logic.

For example, imagine the following scenario: Our actor expects 10 messages to arrive. For each of those 10 that arrive, if the message has a multi-part flag set, we expect a variable number of other "chunk" messages to immediately follow it. Implemented as a coroutine, the logic would be written similar to this:

```java
for (int i = 0; i < 10; i++) {
   cnt.suspend();
   Message msg = context.getIncomingMessage();
   if (msg.isMultipart()) {
      for (int j = 0; j < msg.numberOfChunks(); j++) {
          cnt.suspend();
          MessageChunk msgChunk = context.getIncomingMessage();
          processMultipartMessageChunk(msg, msgChunk);
      }
   } else {
      processMessage(msg);
   }
}
```

However, if it were implemented as a normal actor, the logic would have to be written in a much more convoluted manner:

```java
//
// Keep in mind that, due to the need to retain state between calls to onStep(), all variables have become fields.
// 
switch (state) {
    case START:
        i = 0;
        state = OUTER_LOOP;
    case OUTER_LOOP:
        if (i == 10) {
            state = END;
            return;
        }
        i++;
        msg = context.getIncomingMessage();
        if (msg.isMultipart()) {
           state = INNER_LOOP;
        } else {
           process(msg);
        }
        return;
    case INNER_LOOP:
        msgChunk = context.getIncomingMessage();
        if (i == msg.getNumberOfChunks()) {
            state = OUTER_LOOP;
            return;
        }
        processMultipartMessageChunk(msg, msgChunk);
        return;
    case END:
        return;
}
```

#### Gateways

A Gateway, like an Actor, communicates with other components through message-passing, but isn't bound by any of the same rules as Actors. Gateways are mainly used to interface with third-party components that can't be communicated with via message-passing. As such, it's perfectly acceptable for a gateway to expose internal state, share state, perform I/O, perform thread synchronization, or otherwise block.

Peernetic provides the following gateway implementations:

 * TimerGateway -- Echoes back messages after a certain duration of time.
 * DirectGateway -- Allows normal Java code to interface with actors/gateways.
 * VisualizerGateway -- Displays 2D directed graphs.
 * UdpGateway -- Sends and receives messages over UDP.
 * LogGateway -- Logs messages to SLF4J.
 * RecorderGateway -- Saves incoming messages to a file.
 * ReplayerGateway -- Replays messages saved by a RecorderGateway.

Gateways run in their own isolated thread / threadpools.

#### Differences

Unlike some other actor frameworks ...

 1. **Peernetic doesn't provide a "central directory" for actors/gateways.** Before a primitive can send messages to another primitive, it needs to be linked to that other primitive. This is done by binding Shuttles. If you've used other actor frameworks before, shuttles are similar to mailboxes.
 1. **Peernetic doesn't provide guarantees around message delivery.** The underlying transport mechanism is what determines guarantees around message delivery. Actors/Gateways communicating locally will have messages that are delivered and in ordered. But, if messages are piped over a volatile transport (e.g. UDP), nothing is guaranteed.
 1. **Peernetic doesn't use Futures/Promises for anything.**

## Examples

It's highly recommended that you go through the [concepts primer](#concepts) before digging in to these examples.

### Unstructured Mesh Example

The Unstructured Mesh example is located in the [examples project](examples/src/main/java/com/offbynull/peernetic/examples/unstructured).

#### What is an Unstructured Mesh?

From [Wikipedia](https://en.wikipedia.org/wiki/Peer-to-peer#Unstructured_networks): *Unstructured peer-to-peer networks do not impose a particular structure on the overlay network by design, but rather are formed by nodes that randomly form connections to each other. (Gnutella, Gossip, and Kazaa are examples of unstructured P2P protocols).*

#### Running the Example

The Unstructured Mesh example comes with 5 executable Java classes...

 * **RealtimeDirect** - Nodes run locally and communicate directly.
 * **RealtimeViaUdpGateway** - Nodes run locally and communicate via UDP.
 * **RealtimeViaUdpSimulator** - Nodes run locally and communicate via simulated UDP.
 * **SimulationDirect** - Nodes run in a simulation and communicate directly. 
 * **SimulationViaUdpSimulator** - Nodes run in a simulation and communicate via simulated UDP.

![Unstructured Example Screenshot](../gh-pages/unstructured_example1.png)

##### Realtime executables

Realtime executables don't require user input because they run pre-configured scenarios. Simulation executables capture messages intended for a visualizer, and these messages are replayed to an actual visualizer once the simulation is complete.

##### Simulation executables

Simulation executables don't require user input because they run pre-configured scenarios. Simulation executables capture messages intended for a visualizer, and these messages are replayed to an actual visualizer once the simulation is complete.

### Chord Distrubted Hash Table Example

The Chord DHT example is located in the [examples project](examples/src/main/java/com/offbynull/peernetic/examples/chord).

#### What is Chord?

From [Wikipedia](https://en.wikipedia.org/wiki/Chord_(peer-to-peer)): *In computing, Chord is a protocol and algorithm for a peer-to-peer distributed hash table. A distributed hash table stores key-value pairs by assigning keys to different computers (known as "nodes"); a node will store the values for all the keys for which it is responsible. Chord specifies how keys are assigned to nodes, and how a node can discover the value for a given key by first locating the node responsible for that key.*

#### Running the Example

The Chord example comes with 5 executable Java classes...

 * **RealtimeDirect** - Chord nodes run locally and communicate directly.
 * **RealtimeViaUdpGateway** - Chord nodes run locally and communicate via UDP.
 * **RealtimeViaUdpSimulator** - Chord nodes run locally and communicate via simulated UDP.
 * **SimulationDirect** - Chord nodes run in a simulation and communicate directly. 
 * **SimulationViaUdpSimulator** - Chord nodes run in a simulation and communicate via simulated UDP.

![Chord Example Screenshot](../gh-pages/chord_example1.png)

##### Realtime executables

Executables classes prefixed with **Realtime** require user input. When the example starts, a console-like window is presented. The following sequence of commands will create a 64-node Chord cluster and bring it online. Note that node 0 will start as the bootstrap node, and all other nodes (1 to 63) will enter the network via node 0 when they start.

```
64
boot 0
start 1 63 0
```

##### Simulation executables

Simulation executables don't require user input because they run pre-configured scenarios. Simulation executables capture messages intended for a visualizer, and these messages are replayed to an actual visualizer once the simulation is complete.

### Raft Distributed Consensus Example

The Raft example is located in the [examples project](examples/src/main/java/com/offbynull/peernetic/examples/raft).

#### What is Raft?

From [Wikipedia](https://en.wikipedia.org/wiki/Raft_(computer_science)): *Raft is a consensus algorithm designed as an alternative to Paxos. It was meant to be more understandable than Paxos by means of separation of logic, but it is also formally proven safe and offers some new features. Raft offers a generic way to distribute a state machine across a cluster of computing systems, ensuring that each node in the cluster agrees upon the same series of state transitions.*

#### Running the Example

The Raft example comes with 5 executable Java classes...

 * **RealtimeDirect** - Raft nodes run locally and communicate directly.
 * **RealtimeViaUdpGateway** - Raft nodes run locally and communicate via UDP.
 * **RealtimeViaUdpSimulator** - Raft nodes run locally and communicate via simulated UDP.
 * **SimulationDirect** - Raft nodes run in a simulation and communicate directly. 
 * **SimulationViaUdpSimulator** - Raft nodes run in a simulation and communicate via simulated UDP.

![Raft Example Screenshot](../gh-pages/raft_example1.png)

##### Realtime executables

Executables classes prefixed with **Realtime** require user input. When the example starts, a console-like window is presented. The following sequence of commands will create a 5-node Raft cluster and bring it online. Note that node 5 is the client that's reading from and writing to the cluster, while all other nodes (0 to 4) are Raft server nodes.

```
5
start 0 4
```

##### Simulation executables

Simulation executables don't require user input because they run pre-configured scenarios. Simulation executables capture messages intended for a visualizer, and these messages are replayed to an actual visualizer once the simulation is complete.
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

## Examples

It's highly recommended that you go through the primer (COMING SOON!) before digging in to these examples.

### Simple Hello World

Note that this example uses [Coroutines](https://github.com/offbynull/coroutines), and as such you'll need to make use of the coroutines Maven plugin to instrument your code.

```java
private static void helloWorldTest() throws InterruptedException {
    // Create coroutine actor that forwards messages to the logger
    Coroutine echoerActor = (cnt) -> {
        final Address loggerAddress = Address.of("log");
        
        Context ctx = (Context) cnt.getContext();

        do {
            Object valueToWrite = ctx.getIncomingMessage();
            ctx.addOutgoingMessage(loggerAddress, LogMessage.debug("Received an echo: {}", valueToWrite));
            cnt.suspend();
        } while (true);
    };


    // Create the actor thread (container for actors) and the log gateway (gateway that pipes messages to slf4j).
    ActorThread actorThread = ActorThread.create("actors");
    LogGateway logGateway = new LogGateway("log");


    // Allow the actor thread to send messages to the log gateway
    actorThread.addOutgoingShuttle(logGateway.getIncomingShuttle());


    // Add the coroutine actor and prime it with a hello world message
    actorThread.addCoroutineActor("echoer", echoerActor, "Hello World!!!");


    // Wait until interrupted
    while(true) {
        Thread.sleep(1000L);
    }
}
```

The output is as follows:

```
16:36:42.623 [LogGateway-log] DEBUG c.o.p.core.gateways.log.LogRunnable - Log gateway started
... snip ...
16:36:42.668 [LogGateway-log] DEBUG c.o.p.core.gateways.log.LogRunnable - actors:echoer - Received an echo: Hello World!!!
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

[Unstructured](examples/src/main/java/com/offbynull/peernetic/examples/unstructured) is an implementation of an unstructured mesh network ([Wikipedia: Peer-to-peer](https://en.wikipedia.org/wiki/Peer-to-peer#Unstructured_networks)).

Both realtime and simulation unstructured examples don't require any user input. They run the same preset scenario: nodes gradually created and added in to the mesh (100 nodes in total), where they search for nodes that to connect to. Note that node 0 starts as the bootstrap node, and all other nodes (1 to 99) will enter the network via node 0 when they start.

![Unstructured Example Screenshot](../gh-pages/unstructured_example1.png)

#### Chord Example

[Chord](examples/src/main/java/com/offbynull/peernetic/examples/chord) is an implementation of Chord DHT's overlay network ([Wikipedia: Chord (peer-to-peer)](https://en.wikipedia.org/wiki/Chord_(peer-to-peer))).

Realtime Chord examples require user input. When the example starts, a console-like window is presented. The following sequence of commands will create a 64-node Chord cluster and bring it online. Note that node 0 will start as the bootstrap node, and all other nodes (1 to 63) will enter the network via node 0 when they start.

```
64
boot 0
start 1 63 0
```

Simulation Chord examples don't require any user input. They run preset scenarios.

![Chord Example Screenshot](../gh-pages/chord_example1.png)

#### Raft Example

[Raft](examples/src/main/java/com/offbynull/peernetic/examples/raft) is an implementation of the Raft distributed consensus algorithm ([Wikipedia: Raft (computer science)](https://en.wikipedia.org/wiki/Raft_(computer_science))).

Realtime Raft examples require user input. When the example starts, a console-like window is presented. The following sequence of commands will create a 5-node Raft cluster and bring it online. Note that node 5 is the client that's reading from and writing to the cluster, while all other nodes (0 to 4) are Raft servers.

```
5
start 0 4
```

![Raft Example Screenshot](../gh-pages/raft_example1.png)

Simulation Raft examples don't require any user input. They run preset scenarios.
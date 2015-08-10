<div style="text-align:center"><img src ="../tree/gh-pages/logo.svg" /></div>

# Peernetic

*CURRENTLY IN BETA.*

Peernetic is a lightweight Java actor framework.

Unlike other actor frameworks, Peernetic was designed first and foremost with distributed P2P algorithm/application development in mind. That is to say, while Peernetic can be used as a generic actor framework, it provides a feature-set that takes some of the pain out of distributed P2P development.

Peernetic's high-level features include:

* **First-class support for coroutines** - Coroutines allow you to suspend the execution of logic within your actor at will. Retain your actor's execution state between incoming messages and/or have multiple threads of execution within your actor, without the need for tedious hand-written state machine logic.
* **Networking** - Send messages transparently over a network. The UDP gateway allows you to send messages over UDP, and the UDP simulator actor allows you to mimic a UDP-like environment locally where you control the network conditions (e.g. packet loss. packet duplication, latency, jitter, etc..) to see how your actor reacts.
* **Visualizer** - Visualize directed graphs with ease. The visualizer gateway makes it easy to visualize graph-related information in real-time, such as P2P network overlay information.
* **Simulator** - Deterministicly simulate many actors interacting with each other in faster than real-time. The simulator makes it easy to write deterministic and reproducible tests for your actor logic. It also allows you to see how your actor deals with common P2P issues - network churn, network partitioning, high latency, high packet-loss, etc.

More information on the topic of actors and their advantages can be found on the following pages:

* [Wikipedia: Actor model](https://en.wikipedia.org/wiki/Actor_model)
* [Wikipedia: Peer-to-peer](https://en.wikipedia.org/wiki/Peer-to-peer)
* [Programmers Stack Exchange: How is the actor model used?](http://programmers.stackexchange.com/questions/99501/how-is-the-actor-model-used)

## Examples

It's highly recommended that you go through the primer (COMING SOON!) before digging in to these examples.

Peernetic comes with 3 real-world examples...

 1. **[Unstructured Mesh](examples/src/main/java/com/offbynull/peernetic/examples/unstructured)** -- More information on the unstructured mesh networks can be found on [Wikipedia: Peer-to-peer](https://en.wikipedia.org/wiki/Peer-to-peer#Unstructured_networks).
 1.  **[Chord DHT Overlay](examples/src/main/java/com/offbynull/peernetic/examples/chord)** -- More information on the Chord algorithm can be found on [Wikipedia: Chord (peer-to-peer)](https://en.wikipedia.org/wiki/Chord_(peer-to-peer)).
 1. **[Raft Distributed Consensus](examples/src/main/java/com/offbynull/peernetic/examples/raft)** --  More information on the Raft algorithm can be found on [Wikipedia: Raft (computer science)](https://en.wikipedia.org/wiki/Raft_(computer_science)).

Each example comes with 5 executable Java classes...

 * **RealtimeDirect** - All actors run locally and directly communicate with each other.
 * **RealtimeViaUdpGateway** - All actors run locally, but each actor communicates with the other actors via UDP.
 * **RealtimeViaUdpSimulator** - All actors run locally, but each actor communicates with the other actors via simulated UDP.
 * **SimulationDirect** - Actors run in a simulation environment where all actors directly communicate with each other. The simulation captures messages to intended for a visualizer, and these messages are replayed to an actual visualizer once the simulation is complete.
 * **SimulationViaUdpSimulator** - Actors run in a simulation environment, but each actor communicates with the other actors via simulated UDP. The simulation captures messages to intended for a visualizer, and these messages are replayed to an actual visualizer once the simulation is complete.

Realtime executable classes may require user input. Simulation executable classes run without user input.

As an example, if you were to run ReltimeDirect for Chord, a window would pop-up asking you for the number of nodes.

 
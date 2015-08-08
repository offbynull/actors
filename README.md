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

Peernetic comes with 3 real-world examples. It's highly recommended that you go through the primer (COMING SOON!) before digging in to these examples.

### [Unstructured Mesh](examples/src/main/java/com/offbynull/peernetic/examples/unstructured)

An example that forms an unstructured mesh network.

From [Wikipedia](https://en.wikipedia.org/wiki/Peer-to-peer): Unstructured peer-to-peer networks do not impose a particular structure on the overlay network by design, but rather are formed by nodes that randomly form connections to each other. Gnutella, Gossip, and Kazaa are examples of unstructured P2P protocols.

### [Chord DHT Overlay](examples/src/main/java/com/offbynull/peernetic/examples/chord)

An example that forms a Chord DHT overlay network, which is a form of structured network.

From  [Wikipedia](https://en.wikipedia.org/wiki/Chord_(peer-to-peer)): In computing, Chord is a protocol and algorithm for a peer-to-peer distributed hash table. A distributed hash table stores key-value pairs by assigning keys to different computers (known as "nodes"); a node will store the values for all the keys for which it is responsible. Chord specifies how keys are assigned to nodes, and how a node can discover the value for a given key by first locating the node responsible for that key.

### [Raft Distributed Consensus](examples/src/main/java/com/offbynull/peernetic/examples/raft)

An example that forms a Raft distributed consensus cluster.

From [Wikipedia](https://en.wikipedia.org/wiki/Raft_(computer_science)): Raft is a consensus algorithm designed as an alternative to Paxos. It was meant to be more understandable than Paxos by means of separation of logic, but it is also formally proven safe and offers some new features. Raft offers a generic way to distribute a state machine across a cluster of computing systems, ensuring that each node in the cluster agrees upon the same series of state transitions. 
 



# Peernetic

*CURRENTLY IN BETA.*

Peernetic is a lightweight Java actor framework. Unlike other actor frameworks, Peernetic was designed first and foremost with the development of P2P and distributed algorithms/applications in mind. That is to say, while Peernetic can be used as a generic actor framework, it provides a feature-set that takes some of the pain out of P2P and distributed software development.

Peernetic's high-level features include:

* *First-class support for coroutines* - easily retain actor execution state between incoming messages and easily support multiple threads of execution within your actors.
* *Networking* - send messages transparently over UDP, or simulate a UDP-like environment locally where you control the network conditions (e.g. packet loss. packet duplication, latency, jitter, etc..).
* *Visualizer* - plot out directed graphs like network overlays.
* *Simulator* - run simulations of actors faster than real-time and in a deterministic manner.

More information on the topic of actors and their advantages can be found on the following pages:

* [Wikipedia: Actor model](https://en.wikipedia.org/wiki/Actor_model)
* [Programmers Stack Exchange: How is the actor model used?](http://programmers.stackexchange.com/questions/99501/how-is-the-actor-model-used)

## Documentation

ADD DOCUMENTATION HERE

## Examples

1. Unstructured Mesh Network Algorithm
1. Chord DHT Algorithm
1. Raft Consensus Algorithm


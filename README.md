### Sistemes Intel·ligents Distribuïts (2019 Q1) final project

Polluted water management system for industry and treament plants simulated with agents (Jade), ontology (Proteus) and semantic web (Jena).

# How to execute

To execute the platform:

```bash
ant run
```

The sniffer will open, so the agents' communication can be checked in
runtime.

Please make sure that if you move the code for your industry agent into
the `edu.upc.fib.sid.Industry` the platform will still run successfully.

This project includes a class named `edu.upc.fib.sid.OntologyParser`
with an example on how to get the data from the ontology.

The final version of the ontology is found at
`edu/upc/fib/sid/sid2019-wwtp-final.owl`. The only change from the
previous version is that the `Morrich` individual has been replaced with
12 individuals `Industry-X` where X is the identifier for your group.


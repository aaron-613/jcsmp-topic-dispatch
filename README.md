# solace-java-topic-dispatch

A collection of classes to perform topic dispatch with JCSMP.

Also utilities to do Solace topic subscription-style matching against topics.  That is:

- `*` is a single-level wildcard, matches 0-or-more chars up to the next `/` level
- `>` is a multi-level wildcard, must occur at the end of a subscription following a `/`, and matches the rest of the topic




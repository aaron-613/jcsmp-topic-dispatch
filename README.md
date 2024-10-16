# jcsmp-topic-dispatch

A collection of helper classes to perform topic dispatch with JCSMP, which includes utilities to do Solace topic subscription-style matching against topics.
This was written with Direct messaging in mind, but it should work fine for queues as well (`FlowReceiver.onReceive()`).  I think, I'll have to test.

Probably the file to check out is [`TopicUtils.java`](jcsmp-topic-dispatch-lib/src/main/java/com/solace/labs/topic/TopicUtils.java)
in the "lib" which has the topic matching logic.  I have implemented a regex-style matching (which is more portable b/c
you can just convert your subscription into a regex, and use it elsewhere), or a more performant linear scan algorithm, comparing char-by-char.

My utilities assume a subscription is "well-formed".  That is, wildcard chars `*` and `>` are not used as literals in the subscription, and there are no empty levels
in the subscription.  See [the Solace docs on wildcards](https://docs.solace.com/Messaging/Wildcard-Charaters-Topic-Subs.htm) for more info.

Recall:

- `*` is a single-level wildcard, matches 0-or-more chars up to the next `/` level
- `>` is a multi-level wildcard, must occur at the end of a subscription following a `/`, and matches the rest of the topic

More here.

Explanations on how to use.

Threading issues?



Interesting internal Slack convo for my Solace colleagues: https://solacedotcom.slack.com/archives/C627M1NKA/p1692385333575059

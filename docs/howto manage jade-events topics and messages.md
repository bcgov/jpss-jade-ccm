# How-to manage isl-events topics and messages

This document lists the how-to's around accessing and managing Kafka topics and messages via command line.

## How to console log a topic as a command line consumer 


```
oc -n cef5dd-dev rsh service/events-kafka-brokers
cd /opt/kafka/bin
./kafka-console-consumer.sh --bootstrap-server events-kafka-bootstrap:9092 --topic ccm-kpis

```

## Related Videos

[OpenShift 4 Red Hat Operators](https://www.youtube.com/watch?v=HzkE7CZU7Bg)


# Apicurio Service Registry Demo

## How to run the demo

* Clone and build Apicurio registry project: https://github.com/Apicurio/apicurio-registry

`git clone git@github.com:Apicurio/apicurio-registry.git`

`mvn clean install`

* Download and run Kafka (also set KAFKA_HOME environment variable)

* Create the demo Kafka Topic

`$KAFKA_HOME/bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic AppTopic`

* Run the registry

`java -jar -Dquarkus.profile=dev $GIT_HOME/apicurio-registry/app/target/apicurio-registry-app-*-runner.jar`

* Run demo Producer App

`mvn exec:java -Dexec.mainClass="io.apicurio.registry.demo.ProducerApplication"`

* Run demo Consumer App

`mvn exec:java -Dexec.mainClass="io.apicurio.registry.demo.ConsumerApplication"`

## What does the demo do / show-case

The demo shows how to create a simple Kafka application that uses the custom Apicurio Kafka Serializers
and Deserializers (serdes) to leverage an Avro schema found in the Apicurio Registry.

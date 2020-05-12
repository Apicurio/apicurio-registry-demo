# Apicurio Service Registry Demo

## How to run the demo

* Clone and build Apicurio registry project: https://github.com/Apicurio/apicurio-registry

`git clone git@github.com:Apicurio/apicurio-registry.git`

`mvn clean install -DskipTests -Pstreams`

* Download and run Kafka (also set KAFKA_HOME environment variable)

* Run demo_setup.sh script

`https://github.com/alesj/registry-demo/blob/master/demo_setup.sh`

* Run two instances of registry

`java -jar -Dquarkus.profile=dev /Users/alesj/projects/redhat/apicurio-registry/storage/streams/target/apicurio-registry-storage-streams-1.1.1-SNAPSHOT-runner.jar`

`java -jar -Dquarkus.profile=dev -Dquarkus.http.port=8081 -D%dev.registry.streams.topology.application.server=localhost:9001 /Users/alesj/projects/redhat/apicurio-registry/storage/streams/target/apicurio-registry-storage-streams-1.0.3-SNAPSHOT-runner.jar`

* Run demo's Main (from IDE)

`https://github.com/alesj/registry-demo/blob/master/src/main/java/io/apicurio/registry/demo/Main.java`

* Run demo's TestMain (from IDE)

`https://github.com/alesj/registry-demo/blob/master/src/test/java/io/apicurio/registry/test/TestMain.java`

* Simple cURL command

`curl -d "{"foo":"bar"}" -H "Content-Type: application/json" -H "X-Registry-ArtifactType: JSON" -H "X-Registry-ArtifactId: qwerty" http://localhost:8080/api/artifacts`

## What does the demo do / show-case

Demo runs two clustered instances of registry, with its Streams storage. 
The demo's main application creates a simple Kafka stream, which uses registry's Avro deserializer for input topic.
The test main uses registry's Avro serializer to send the messages to main's input topic.
Avro serializer and deserializer use registry to get a hold of Avro schema in order to be able to properly serialize and deserialize Kafka messages. 

In the test main we can see how we register the schema at one node, 
whereas the serializer points to second node for lookup.

### What about Registry Maven Plugin?

Checkout our [pom.xml](https://github.com/Apicurio/apicurio-registry-demo/blob/master/pom.xml), its profiles, for the plugin usage.

e.g. how to register a schema

```xml
          <plugin>
            <groupId>io.apicurio</groupId>
            <artifactId>apicurio-registry-maven-plugin</artifactId>
            <version>${registry.version}</version>
            <executions>
              <execution>
                <phase>generate-sources</phase>
                <goals>
                  <goal>register</goal>
                </goals>
                <configuration>
                  <registryUrl>http://localhost:8080</registryUrl>
                  <artifactType>AVRO</artifactType>
                  <artifacts>
                    <schema1>${project.basedir}/schemas/schema1.avsc</schema1>
                  </artifacts>
                </configuration>
              </execution>
            </executions>
          </plugin>
```

## What's there to see?

In the main's console you should see `INFO: Merged logs: fst: 1572428933954 snd: 1572428934726`

And if you run `$KAFKA_HOME/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic logx-topic --from-beginning`
you should see calculated diff between two matching logs: e.g. `Log diff: 192`

This means that Streams have processed input topic with registry's Avro deserializer and forwarded + transformed the record value to logx-topic, for Kafka console consumer to handle.

## Running things in Docker

Start ZooKeeper: `docker run -p 2181:2181 --name zookeeper wurstmeister/zookeeper`

Start Kafka: `docker run --link zookeeper --name kafka -p 9092:9092 -e KAFKA_ADVERTISED_HOST_NAME=kafka -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 wurstmeister/kafka`

Add `127.0.0.1  kafka` to /etc/hosts (so that brokers map "kafka" to localhost).

Run script to setup topics: `./demo_setup.sh docker`

Start single Registry: 

`docker run -p 8080:8080 -e QUARKUS_PROFILE=prod -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 -e APPLICATION_ID=demo_app_1 --link kafka apicurio/apicurio-registry-streams`

## Running things with Docker-compose

To run a 2 node cluster (needed for this demo).

Build Demo project: `mvn clean install`

Build Demo's latest Docker image: `docker build docker -t apicurio/apicurio-registry-demo` 

Run this 3 docker-compose yamls:

* `docker-compose -f docker-kafka.yaml up`

* `docker-compose -f docker-registry.yaml up`

* `docker-compose -f docker-demo.yaml up`

Cleanup: `docker rm -f zookeeper kafka registry1 registry2 demo`

## Running things with Kubernetes (minikube)

You need to have `minikube` and `kubectl` installed locally. 

* remove any previous `minikube` instance

* simply run our `strimzi.sh` script.

* grab minikube IP: `minikube ip` --> <MINIKUBE_IP>

* run TestMain with `-Dbootstrap.servers=<MINIKUBE_IP>:32100 -Dregistry.url.1=<MINIKUBE_IP>:30080 -Dregistry.url.2=<MINIKUBE_IP>:30080`
* e.g. mvn exec:java -Dexec.mainClass="io.apicurio.registry.demo.Main" -Dbootstrap.servers=192.168.39.204:32100 -Dregistry.url.1=192.168.39.204:30080 -Dregistry.url.2=192.168.39.204:30080

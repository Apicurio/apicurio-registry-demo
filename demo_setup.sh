## Functions

setupStreams() {
    if [ -z $KAFKA_HOME ]; then
      echo 'Missing KAFKA_HOME env var'
      exit 1;
    fi
    trap "exit 1" SIGINT SIGTERM
    # ZooKeeper & Kafka
    cleanup
    echo $KAFKA_HOME
    cd $KAFKA_HOME/bin
    ./zookeeper-server-start.sh $KAFKA_HOME/config/zookeeper.properties &
    sleep 1
    ./kafka-server-start.sh $KAFKA_HOME/config/server.properties &
    sleep 2
    topics
    sleep 1
    echo 'Demo Kafka ready ...'
}

setupDocker() {
    if [ -z $KAFKA_HOME ]; then
      echo 'Missing KAFKA_HOME env var'
      exit 1;
    fi
    echo $KAFKA_HOME
    cd $KAFKA_HOME/bin
    topics
    echo 'Demo Kafka Docker ready ...'
}

topics() {
    ./kafka-topics.sh --zookeeper localhost --create --topic storage-topic --partitions 1 --replication-factor 1  --config cleanup.policy=compact
    ./kafka-topics.sh --zookeeper localhost --create --topic global-id-topic --partitions 1 --replication-factor 1 --config cleanup.policy=compact
    ./kafka-topics.sh --zookeeper localhost --create --topic input-topic --partitions 1 --replication-factor 1  --config cleanup.policy=compact
    ./kafka-topics.sh --zookeeper localhost --create --topic logx-topic --partitions 1 --replication-factor 1 --config cleanup.policy=compact
    ./kafka-topics.sh --zookeeper localhost --create --topic dbx-topic --partitions 1 --replication-factor 1 --config cleanup.policy=compact
}

cleanup() {
    ps -ax | grep kafka | awk '{print $1}' | xargs kill -9
    ps -ax | grep zookeeper | awk '{print $1}' | xargs kill -9
    rm -rf /tmp/zookeeper/
    rm -rf /tmp/kafka-*
}

## Main

if [ -z $1 ]; then
    echo 'Setting-up Demo Kafka ...'
    setupStreams
elif [ "$1" == "cleanup" ]
then
    echo 'Demo Kafka cleanup ...'
    cleanup
elif [ "$1" == "docker" ]
then
    echo 'Demo Kafka Docker ...'
    setupDocker
fi

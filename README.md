Data producer
=============

The data producer solution was created using Scala 2.11 and Play 2.5 for storing incomming files and sending a message.
It requires a kafka messaging solution to create a communication between the front-end application (data producer) and backend (data consumer)

Requires:
- Java 8

Start zookeeper
--------------
Stand alone:
bin/zookeeper-server-start.sh config/zookeeper.properties

Cluster:
download zookeeper
add to configuration 3 nodes (minimum):
server.1=localhost:2888:3888
server.2=localhost:2889:3889
server.3=localhost:2890:3890

start zk nodes:
bin/zkServer.sh start conf/zoo_sample.cfg
bin/zkServer.sh start conf/zoo_sample2.cfg
bin/zkServer.sh start conf/zoo_sample3.cfg

do not forget id files
echo "1" > /tmp/zookeeper/myid
echo "2" > /tmp/zookeeper2/myid
echo "3" > /tmp/zookeeper3/myid

Connect
bin/zkCli.sh

Start kafka
-----------
Standalone:
bin/kafka-server-start.sh config/server.properties

Cluster:
change kafka conf for each node:
config/server-1.properties:
    broker.id=1
    listeners=PLAINTEXT://:9093
    log.dir=/tmp/kafka-logs-1
    
start each node:
bin/kafka-server-start.sh config/server.properties
bin/kafka-server-start.sh config/server2.properties
bin/kafka-server-start.sh config/server3.properties

create topic
bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 2 --partitions 2 --topic datatopic

modify topic
bin/kafka-topics.sh --alter --zookeeper localhost:2181  --topic datatopic --partitions 3 

check topic
bin/kafka-topics.sh --describe --zookeeper localhost:2181 --topic datatopic

produce client
bin/kafka-console-producer.sh --broker-list localhost:9092 --topic datatopic

consume client
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --from-beginning --topic datatopic


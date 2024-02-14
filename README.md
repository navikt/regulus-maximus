# regulus maximus

### Development

1. Run the database and kafka locally with `docker compose up -d`
2. Start the development server in IntelliJ with "program argument" `-config=application-local.yaml`



### Verifying Kafka
You can verify that Kafka has started and the topics created by running the following commands:

```bash
# Exec into Kafka container
docker exec -it my_kafka_broker bash

# List topics on kafka (from kafka shell)
kafka-topics --list --bootstrap-server localhost:9092

# Check broker status (from kafka shell)
zookeeper-shell zookeeper:2181 ls /brokers/ids
```


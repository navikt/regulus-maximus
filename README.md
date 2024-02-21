# regulus maximus

### Development

1. Run the database and kafka locally with `docker compose up -d`
2. Start the development server in IntelliJ with "program argument" `-config=application-local.yaml`



### Verifying Kafka
You can verify that Kafka has started and list the existing topics by running the following commands:

```bash
# Exec into Kafka container
docker exec -it my_kafka_broker bash

# List topics on kafka (from kafka shell)
kafka-topics --list --bootstrap-server kafka:9092
```


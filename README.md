# regulus maximus

### Development

1. Run the database and kafka locally with `docker-compose up` or run the compose.yaml file manually
2. Start the development server in IntelliJ with "program argument" `--spring.profiles.active=local`
3. check out http://localhost:8080/

### Verifying Kafka
You can verify that Kafka has started and list the existing topics by running the following commands:
Testupdate
```bash
# Exec into Kafka container
docker exec -it my_kafka_broker bash

# List topics on kafka (from kafka shell)
kafka-topics --list --bootstrap-server kafka:9092
```


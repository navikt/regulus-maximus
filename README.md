# regulus maximus
works as a sluse to sykmeldinger i tsm sfæren. 
uses regulus regula to run rules 
* Digital (ny) regelvaliderte, men trengs ny validering.
* Legacy (XML) ikke regelvalidert
* Papir regelvalidert,
* Utenlandsk

### Development

1. Run the database and kafka locally with `docker-compose up` or run the compose.yaml file manually
2. Start the development server in IntelliJ with "program argument" `-config=application-local.conf` or from terminal with `./gradlew runLocal`

### Verifying Kafka
You can verify that Kafka has started and list the existing topics by running the following commands:

```bash
# Exec into Kafka container
docker exec -it my_kafka_broker bash

# List topics on kafka (from kafka shell)
kafka-topics --list --bootstrap-server kafka:9092
```


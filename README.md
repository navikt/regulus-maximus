# regulus maximus
runs rules on digitale sykmeldinger from syk-inn. 
other sykmedlinger are being saved in db and published to kafka topic
without rulevalidation in regulus maximus because they are authoritative

### Development

1. Run the database and kafka locally with `docker compose up -d` or run the compose.yaml file manually
2. Start the development server in IntelliJ with "program argument" `-config=application-local.conf` or from terminal with `./gradlew runLocal`

### Kafka
start producer and produce to tsm.sykmeldinger-input. Example producer with example message: 

ok message:
```bash
docker compose exec -T kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic tsm.sykmeldinger-input \
  --property parse.key=true \
  --property key.separator=: \
  < src/main/resources/validationMessage/kafkaMessageValidationOk.txt
 ```

invalid message:
```bash
docker compose exec -T kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic tsm.sykmeldinger-input \
  --property parse.key=true \
  --property key.separator=: \
  < src/main/resources/validationMessage/kafkaMessageValidationInvalid.txt
```


```bash
# Exec into Kafka container
docker exec -it my_kafka_broker bash

# List topics on kafka (from kafka shell)
kafka-topics --list --bootstrap-server kafka:9092
```


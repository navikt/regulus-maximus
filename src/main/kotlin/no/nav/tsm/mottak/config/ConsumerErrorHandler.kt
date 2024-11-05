package no.nav.tsm.mottak.config

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.stereotype.Component
import org.springframework.util.backoff.ExponentialBackOff

@Component
class ConsumerErrorHandler : DefaultErrorHandler(
    null,
    ExponentialBackOff(1000L, 1.5).also {
        it.maxInterval = 60_000L * 10
    },
) {

    override fun handleRemaining(
        thrownException: Exception,
        records: MutableList<ConsumerRecord<*, *>>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
    ) {
        records.forEach { record ->
            logger.error(
                "Feil i prossesseringen av record med offset: ${record.offset()}, key: ${record.key()} på topic ${record.topic()}",
            )
        }
        if (records.isEmpty()) {
            logger.error("Feil i listener uten noen records")
        }

        super.handleRemaining(thrownException, records, consumer, container)
    }

    override fun handleBatch(
        thrownException: Exception,
        data: ConsumerRecords<*, *>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
        invokeListener: Runnable,
    ) {
        data.forEach { record ->
            logger.error(
                "Feil i prossesseringen av record med offset: ${record.offset()}, key: ${record.key()} på topic ${record.topic()}"
            )
        }
        if (data.isEmpty) {
            logger.error("Feil i listener uten noen records")
        }
        super.handleBatch(thrownException, data, consumer, container, invokeListener)
    }
}
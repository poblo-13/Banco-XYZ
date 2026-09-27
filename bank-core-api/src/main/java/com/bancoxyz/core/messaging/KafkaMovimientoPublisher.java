package com.bancoxyz.core.messaging;

import com.bancoxyz.core.event.MovimientoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class KafkaMovimientoPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(KafkaMovimientoPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topicMovimientos;

    public KafkaMovimientoPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${banco.kafka.topic.movimientos}") String topicMovimientos) {

        this.kafkaTemplate = kafkaTemplate;
        this.topicMovimientos = topicMovimientos;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicarMovimiento(MovimientoEvento evento) {

        String key = evento.cuentaId().toString();

        kafkaTemplate
                .send(topicMovimientos, key, evento)
                .whenComplete((resultado, error) -> {

                    if (error != null) {
                        log.error(
                                "Error publicando evento {} de cuenta {} en Kafka",
                                evento.tipoEvento(),
                                evento.cuentaId(),
                                error
                        );
                        return;
                    }

                    log.info(
                            "Evento Kafka publicado: tipo={}, cuenta={}, topic={}, particion={}, offset={}",
                            evento.tipoEvento(),
                            evento.cuentaId(),
                            topicMovimientos,
                            resultado.getRecordMetadata().partition(),
                            resultado.getRecordMetadata().offset()
                    );
                });
    }
}
package com.bancoxyz.audit.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MovimientoAuditConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(MovimientoAuditConsumer.class);

    private final String instancia;

    public MovimientoAuditConsumer(
            @Value("${auditoria.instancia:auditoria-1}") String instancia) {
        this.instancia = instancia;
    }

    @KafkaListener(
            topics = "${banco.kafka.topic.movimientos}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void procesar(ConsumerRecord<String, String> record) {

        log.info(
                "AUDITORIA [{}] evento procesado: key={}, particion={}, offset={}, payload={}",
                instancia,
                record.key(),
                record.partition(),
                record.offset(),
                record.value()
        );
    }
}
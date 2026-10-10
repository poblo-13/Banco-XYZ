package com.bancoxyz.audit.messaging;

import java.util.Objects;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MovimientoAuditConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(MovimientoAuditConsumer.class);

    private final JdbcTemplate jdbc;
    private final String instancia;

    public MovimientoAuditConsumer(
            JdbcTemplate jdbc,
            @Value("${auditoria.instancia:auditoria-1}")
                    String instancia) {

        this.jdbc = jdbc;
        this.instancia = instancia;
    }

    @KafkaListener(
            topics = "${banco.kafka.topic.movimientos}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void procesar(ConsumerRecord<String, String> record) {

        String identificador = record.key();

        if (identificador == null ||
                identificador.isBlank() ||
                identificador.length() > 100) {

            throw new IllegalArgumentException(
                    "Identificador de evento invalido");
        }

        if (record.value() == null) {
            throw new IllegalArgumentException(
                    "Evento sin contenido");
        }

        int insertados = jdbc.update(
                """
                INSERT IGNORE INTO eventos_auditoria
                (evento_id, topico, particion, offset_kafka, payload)
                VALUES (?, ?, ?, ?, ?)
                """,
                identificador,
                record.topic(),
                record.partition(),
                record.offset(),
                record.value()
        );

        if (insertados == 0) {

            String anterior = jdbc.queryForObject(
                    """
                    SELECT payload
                    FROM eventos_auditoria
                    WHERE evento_id = ?
                    """,
                    String.class,
                    identificador
            );

            if (!Objects.equals(anterior, record.value())) {
                throw new IllegalStateException(
                        "Identificador repetido con contenido diferente");
            }

            log.info(
                    "AUDITORIA [{}] duplicado ignorado: key={}",
                    instancia,
                    identificador
            );

            return;
        }

        log.info(
                "AUDITORIA [{}] evento guardado: key={}, particion={}, offset={}",
                instancia,
                identificador,
                record.partition(),
                record.offset()
        );
    }
}

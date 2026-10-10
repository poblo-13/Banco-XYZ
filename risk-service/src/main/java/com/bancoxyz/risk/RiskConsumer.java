package com.bancoxyz.risk;

import java.math.BigDecimal;
import java.util.Objects;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class RiskConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(RiskConsumer.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();

    public RiskConsumer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @KafkaListener(
            topics = "${banco.kafka.topic.movimientos}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void procesar(ConsumerRecord<String, String> record) {

        if (record.value() == null) {
            throw new IllegalArgumentException("Evento sin contenido");
        }

        JsonNode json;

        try {
            json = mapper.readTree(record.value());
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "JSON de evento invalido", ex);
        }

        if (json == null ||
                !"MOVIMIENTO_CONFIRMADO".equals(
                        json.path("evento").asText())) {
            return;
        }

        String id = json.path("operacionId").asText("");
        long cuentaId = json.path("cuentaId").asLong(-1);
        String tipo = json.path("tipo").asText("");
        BigDecimal monto;

        try {
            monto = new BigDecimal(
                    json.path("monto").asText(""));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Monto invalido en evento", ex);
        }

        if (id.isBlank() ||
                id.length() > 100 ||
                !id.equals(record.key()) ||
                cuentaId <= 0 ||
                monto.signum() <= 0 ||
                !("DEPOSITO".equals(tipo) ||
                  "RETIRO".equals(tipo))) {

            throw new IllegalArgumentException(
                    "Datos inconsistentes en evento");
        }

        String resultado = ("RETIRO".equals(tipo) && monto.compareTo(new BigDecimal("10000.00")) >= 0) ? "ALERTA" : "NORMAL";

        boolean nuevo = true;
        try {
            jdbc.update(
                """
                INSERT INTO analisis_riesgo
                    (evento_id, cuenta_id, tipo,
                     monto, resultado, payload_hash)
                VALUES (?, ?, ?, ?, ?, SHA2(?, 256))

                """,
                id,
                cuentaId,
                tipo,
                monto,
                resultado,
                record.value()
            );
        } catch (DuplicateKeyException ex) {
            nuevo = false;
        }

        String guardado = jdbc.queryForObject(
                "SELECT payload_hash FROM analisis_riesgo WHERE evento_id = ?",
                String.class,
                id
        );

        String esperado = jdbc.queryForObject(
                "SELECT SHA2(?, 256)",
                String.class,
                record.value()
        );

        if (!Objects.equals(guardado, esperado)) {
            throw new IllegalStateException(
                    "Evento duplicado con contenido diferente");
        }

        if (nuevo) {
            log.info(
                    "risk-service: evento registrado id={}, resultado={}",
                    id, resultado);
        } else {
            log.info(
                    "risk-service: duplicado ignorado id={}",
                    id);
        }
    }
}

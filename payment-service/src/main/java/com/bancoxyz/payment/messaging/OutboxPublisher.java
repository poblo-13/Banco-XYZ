package com.bancoxyz.payment.messaging;

import com.bancoxyz.payment.repository.EventoMovimientoOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxPublisher.class);

    private final EventoMovimientoOutboxRepository repository;
    private final KafkaTemplate<String, String> kafka;
    private final String topic;

    public OutboxPublisher(
            EventoMovimientoOutboxRepository repository,
            KafkaTemplate<String, String> kafka,
            @Value("${banco.kafka.topic.movimientos:banco.movimientos}")
                    String topic) {

        this.repository = repository;
        this.kafka = kafka;
        this.topic = topic;
    }

    @Scheduled(
            fixedDelayString = "${banco.xyz.pagos.outbox.interval-ms:5000}",
            initialDelayString = "${banco.xyz.pagos.outbox.initial-delay-ms:10000}"
    )
    @Transactional
    public void publicarPendientes() {

        var pendientes = repository
                .findTop10ByEstadoOrderByCreadoEnAsc("PENDIENTE");

        for (var evento : pendientes) {

            try {
                // La clave permite mantener el mismo evento
                // en la misma particion de Kafka.
                kafka.send(
                        topic,
                        evento.getOperacionId(),
                        evento.getPayload()
                ).get(15, TimeUnit.SECONDS);

                // Solo se marca PUBLICADO despues de recibir
                // confirmacion de Kafka.
                evento.marcarPublicado();

                log.info(
                        "OUTBOX PUBLICADO: operacionId={}",
                        evento.getOperacionId()
                );

            } catch (InterruptedException ex) {

                Thread.currentThread().interrupt();
                evento.registrarFallo("Publicacion interrumpida");

                log.warn(
                        "OUTBOX INTERRUMPIDO: operacionId={}",
                        evento.getOperacionId()
                );

                break;

            } catch (Exception ex) {

                evento.registrarFallo(
                        ex.getClass().getSimpleName()
                );

                log.warn(
                        "OUTBOX PENDIENTE DE REINTENTO: operacionId={}, error={}",
                        evento.getOperacionId(),
                        ex.getClass().getSimpleName()
                );
            }
        }
    }
}

# Evidencia de escalamiento horizontal Kafka - Semana 9

Fecha de prueba: 2026-10-10 00:15:05

## Objetivo

Comprobar que tres microservicios independientes consumen eventos
desde Kafka y distribuyen el procesamiento entre dos instancias
por servicio.

## Arquitectura utilizada

- Topic: banco.movimientos
- Particiones: 3
- Grupo de auditoria: banco-auditoria
- Grupo de notificaciones: banco-notificaciones
- Grupo de riesgo: banco-riesgo
- Instancias totales: 6
- Tecnologias: Spring Boot, Kafka, MySQL y Docker Compose

## Resultados

Se publicaron 24 eventos simulados de movimientos bancarios.

| Microservicio | Eventos persistidos |
|---|---:|
| Auditoria | 24 |
| Notificaciones | 24 |
| Riesgo | 24 |

Total: 72 registros distribuidos en tres bases de datos.

## Distribucion por instancia

| Instancia | Eventos procesados |
|---|---:|
| audit-service-1-1 | 7 |
| audit-service-2-1 | 17 |
| notification-service-1 | 17 |
| notification-service-2 | 7 |
| risk-service-1 | 7 |
| risk-service-2 | 17 |

## Validaciones realizadas

- Tres grupos consumidores Kafka independientes.
- Dos instancias activas por microservicio.
- Particiones repartidas entre las instancias.
- Ambas instancias participaron en el procesamiento.
- Se verifico previamente la deduplicacion por identificador.
- Se verifico una alerta de riesgo para un retiro de 15000.

## Alcance y limitaciones

Los 24 eventos fueron simulados y no modificaron saldos bancarios.
Se publicaron en el topic de desarrollo banco.movimientos.

Esta prueba demuestra distribucion funcional de eventos, pero no
constituye una prueba de rendimiento a gran escala.

Quedan pendientes las pruebas de recuperacion ante fallo de una
replica y el endurecimiento de seguridad del broker Kafka.
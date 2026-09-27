# Diagrama de Arquitectura - Banco XYZ
## Exp3 - Semana 7

```mermaid
flowchart LR

    C[Clientes]
    WEB[BFF Web<br/>HTTPS :8081<br/>JWT WEB_ACCESS]
    MOB[BFF Mobile<br/>HTTPS :8082<br/>JWT MOBILE_ACCESS]
    ATM[BFF ATM<br/>HTTPS :8083<br/>JWT ATM_ACCESS]

    RES[Resilience4j<br/>Circuit Breaker: coreApi]
    CORE[Bank Core API<br/>HTTP :8080]
    DB[(MySQL<br/>banco_xyz_core)]

    CONFIG[Config Server<br/>:8888]
    EUREKA[Eureka Discovery Server<br/>:8761]

    KAFKA[(Apache Kafka<br/>:9092)]
    TOPIC[banco.movimientos<br/>3 particiones<br/>Key: cuentaId]
    AUD1[Audit Service<br/>Consumer Group:<br/>banco-auditoria]
    AUD2[Audit Service - Instancia 2<br/>Consumer Group:<br/>banco-auditoria]

    EV1[DEPOSITO_REALIZADO]
    EV2[RETIRO_REALIZADO]
    EV3[COMPRA_REALIZADA]

    C --> WEB
    C --> MOB
    C --> ATM

    WEB --> CORE
    MOB --> CORE
    ATM --> RES --> CORE

    CORE --> DB

    CONFIG -. configuración .-> CORE
    CORE -. registro / heartbeat .-> EUREKA

    CORE -->|AFTER_COMMIT| KAFKA
    KAFKA --> TOPIC

    EV1 --> TOPIC
    EV2 --> TOPIC
    EV3 --> TOPIC

    TOPIC --> AUD1
    TOPIC --> AUD2
```

## Flujo principal

```text
Cliente
  ↓
BFF
  ↓
Bank Core API
  ↓
MySQL
  ↓
COMMIT exitoso
  ↓
Evento interno
  ↓
KafkaMovimientoPublisher
  ↓
banco.movimientos
  ↓
Audit Service
```

## Tópico y eventos

**Tópico:**

```text
banco.movimientos
```

**Particiones:**

```text
3
```

**Key:**

```text
cuentaId
```

**Eventos:**

```text
DEPOSITO_REALIZADO
RETIRO_REALIZADO
COMPRA_REALIZADA
```

## Escalabilidad

```text
Consumer Group: banco-auditoria

Consumidor 1 -> particiones 0 y 1
Consumidor 2 -> partición 2
```

Kafka realiza automáticamente el rebalanceo cuando una nueva instancia de `audit-service` entra o sale del grupo.

## Resiliencia

```text
BFF ATM
  ↓
Resilience4j
  ↓
Bank Core API
```

Estados validados del Circuit Breaker:

```text
CLOSED -> OPEN -> HALF_OPEN -> CLOSED
```

Cuando el Core API no está disponible:

```text
HTTP 503
CORE_NO_DISPONIBLE
```

## Servicios de soporte

```text
Config Server :8888
Eureka        :8761
Kafka         :9092
Core API      :8080
BFF Web       :8081
BFF Mobile    :8082
BFF ATM       :8083
```

# Propuesta Técnica
## Banco XYZ - Arquitectura de Microservicios, Resiliencia y Eventos
### Exp3 - Semana 7

## 1. Introducción

La solución de Banco XYZ evoluciona desde la arquitectura Backend for Frontend (BFF) implementada en semanas anteriores hacia una arquitectura de microservicios con mecanismos de configuración centralizada, descubrimiento de servicios, tolerancia a fallos y comunicación asíncrona orientada a eventos.

La propuesta mantiene tres canales especializados:

- Web;
- Mobile;
- Cajero Automático (ATM).

Cada canal conserva su propio BFF y su esquema de seguridad, mientras que el `bank-core-api` centraliza las reglas principales del dominio bancario y la persistencia.

Durante Semana 7 se incorporan principalmente:

- Spring Cloud Config;
- Eureka Service Discovery;
- Resilience4j;
- Apache Kafka;
- un servicio consumidor de auditoría;
- escalabilidad mediante particiones y grupos de consumidores.

---

## 2. Objetivo técnico

El objetivo es fortalecer la arquitectura del sistema Banco XYZ incorporando mecanismos que permitan:

- desacoplar responsabilidades entre servicios;
- centralizar configuraciones;
- registrar y descubrir servicios;
- responder de forma controlada ante fallas;
- publicar eventos bancarios de manera asíncrona;
- procesar eventos en servicios independientes;
- distribuir carga entre múltiples consumidores;
- mantener seguridad diferenciada por canal.

---

## 3. Arquitectura general

La arquitectura está compuesta por los siguientes módulos:

```text
config-server
discovery-server
bank-core-api
bff-web
bff-mobile
bff-atm
audit-service
```

Flujo general:

```text
Cliente
   |
   +-----------------------------+
   |             |               |
 BFF Web      BFF Mobile      BFF ATM
  :8081          :8082          :8083
                                   |
                             Resilience4j
                                   |
                                   v
                            Bank Core API
                                :8080
                              /       \
                             /         \
                          MySQL        Kafka
                                      :9092
                                        |
                                        v
                              banco.movimientos
                               3 particiones
                                        |
                                        v
                               Audit Service
                            grupo banco-auditoria
```

Servicios de soporte:

```text
Config Server   -> :8888
Eureka Server   -> :8761
```

---

## 4. Backend for Frontend

La solución mantiene tres BFF independientes.

### BFF Web

Orientado a interfaces de navegador y respuestas más completas.

```text
https://localhost:8081
```

Scope:

```text
WEB_ACCESS
```

### BFF Mobile

Orientado a respuestas más ligeras para dispositivos móviles.

```text
https://localhost:8082
```

Scope:

```text
MOBILE_ACCESS
```

### BFF ATM

Orientado a operaciones específicas de cajero automático.

```text
https://localhost:8083
```

Scope:

```text
ATM_ACCESS
```

Los BFF no acceden directamente a MySQL. La persistencia permanece encapsulada en `bank-core-api`.

---

## 5. Core API

El módulo `bank-core-api` centraliza la lógica bancaria.

Responsabilidades principales:

- consulta de cuentas;
- consulta de movimientos;
- registro de depósitos;
- registro de retiros;
- registro de compras;
- actualización de saldos;
- validación de saldo disponible;
- persistencia mediante Spring Data JPA;
- publicación de eventos de movimientos.

Base de datos:

```text
banco_xyz_core
```

Tablas principales:

```text
cuentas
movimientos
```

---

## 6. Configuración centralizada

Se incorpora `config-server` utilizando Spring Cloud Config Server.

Puerto:

```text
8888
```

El Core API consume propiedades desde:

```text
config-repo/bank-core-api.properties
```

Entre las propiedades centralizadas se encuentran:

- URL de Eureka;
- broker Kafka;
- serializadores Kafka;
- tópico de movimientos;
- propiedades de observabilidad.

Esto permite separar parte de la configuración operacional del código de la aplicación.

---

## 7. Service Discovery

Se incorpora `discovery-server` mediante Netflix Eureka.

Puerto:

```text
8761
```

El `bank-core-api` se registra dinámicamente como:

```text
BANK-CORE-API
```

Durante las pruebas se verificó su estado:

```text
UP
```

Este mecanismo permite registrar servicios y facilita una arquitectura preparada para descubrimiento dinámico.

---

## 8. Tolerancia a fallos con Resilience4j

El BFF ATM incorpora un Circuit Breaker denominado:

```text
coreApi
```

El objetivo es evitar que las fallas de comunicación con el Core API provoquen respuestas descontroladas o llamadas repetitivas innecesarias.

Configuración utilizada:

```text
sliding-window-type: COUNT_BASED
sliding-window-size: 3
minimum-number-of-calls: 3
failure-rate-threshold: 50
wait-duration-in-open-state: 10s
permitted-number-of-calls-in-half-open-state: 1
automatic-transition-from-open-to-half-open-enabled: true
```

Cuando el Core API no está disponible, el BFF ATM responde de manera controlada:

```text
HTTP 503 Service Unavailable
CORE_NO_DISPONIBLE
```

Se verificó el ciclo completo del Circuit Breaker:

```text
CLOSED
   |
   v
OPEN
   |
   v
HALF_OPEN
   |
   v
CLOSED
```

También se configuraron excepciones funcionales que no deben contabilizarse como fallas de infraestructura, por ejemplo:

```text
AtmRecursoNoEncontradoException
AtmSaldoInsuficienteException
```

De esta forma, una cuenta inexistente continúa respondiendo con su error funcional y no abre el Circuit Breaker.

---

## 9. Arquitectura orientada a eventos

Para la mensajería asíncrona se seleccionó Apache Kafka.

La razón principal es que Kafka permite:

- desacoplar productores y consumidores;
- conservar eventos en un tópico;
- trabajar con particiones;
- distribuir procesamiento mediante Consumer Groups;
- escalar consumidores horizontalmente.

Kafka se ejecuta mediante Docker utilizando:

```text
docker-compose.kafka.yml
```

Broker:

```text
localhost:9092
```

---

## 10. Tópico y eventos

Tópico principal:

```text
banco.movimientos
```

Configuración:

```text
Particiones: 3
Factor de replicación: 1
```

Tipos de eventos implementados:

```text
DEPOSITO_REALIZADO
RETIRO_REALIZADO
COMPRA_REALIZADA
```

Contrato utilizado:

```text
MovimientoEvento
```

Campos principales:

```text
tipoEvento
movimientoId
cuentaId
tipoMovimiento
monto
saldoResultante
fechaMovimiento
fechaEvento
```

Ejemplo validado:

```json
{
  "tipoEvento": "RETIRO_REALIZADO",
  "movimientoId": 13,
  "cuentaId": 101,
  "tipoMovimiento": "retiro",
  "monto": -100.00,
  "saldoResultante": 4850.00
}
```

---

## 11. Patrón de publicación de eventos

La solución utiliza un enfoque de notificación de eventos posterior a la confirmación de la transacción.

Dentro de `MovimientoService` se publica primero un evento interno mediante:

```text
ApplicationEventPublisher
```

El componente `KafkaMovimientoPublisher` escucha ese evento con:

```text
@TransactionalEventListener(
    phase = TransactionPhase.AFTER_COMMIT
)
```

Después del COMMIT se envía el evento a Kafka.

Flujo:

```text
Operación bancaria
      |
      v
Validación
      |
      v
Persistencia MySQL
      |
      v
COMMIT exitoso
      |
      v
Evento interno
      |
      v
KafkaMovimientoPublisher
      |
      v
banco.movimientos
```

Esta decisión evita publicar el evento antes de que la operación bancaria haya sido confirmada en la base de datos.

---

## 12. Clave Kafka y orden de eventos

Cada evento utiliza como clave:

```text
cuentaId
```

Ejemplo:

```text
Key: 101
```

El uso de `cuentaId` permite que los eventos asociados a una misma cuenta sean dirigidos consistentemente a una partición, conservando su orden relativo dentro de ella.

Al mismo tiempo, diferentes cuentas pueden distribuirse entre las distintas particiones.

---

## 13. Consumidor de auditoría

Se incorpora un nuevo módulo:

```text
audit-service
```

Su responsabilidad es consumir de manera asíncrona los eventos del tópico:

```text
banco.movimientos
```

Grupo:

```text
banco-auditoria
```

El consumidor procesa:

- key;
- partition;
- offset;
- payload.

Ejemplo validado:

```text
AUDITORIA [auditoria-1] evento procesado:
key=101
particion=2
offset=0
payload={"tipoEvento":"RETIRO_REALIZADO", ...}
```

El servicio utiliza `StringDeserializer` para consumir el JSON producido por el Core API.

---

## 14. Escalabilidad horizontal

El tópico `banco.movimientos` posee tres particiones.

Se ejecutaron simultáneamente dos instancias de `audit-service` dentro del mismo grupo:

```text
banco-auditoria
```

Kafka realizó automáticamente un rebalanceo.

Distribución observada:

```text
Consumidor 1 -> particiones 0 y 1
Consumidor 2 -> partición 2
```

La inspección del grupo mostró dos `CONSUMER-ID` distintos y las particiones distribuidas entre ambos consumidores.

Esto demuestra que el procesamiento puede escalar horizontalmente al agregar nuevas instancias del mismo consumidor.

---

## 15. Prueba funcional completa

Se ejecutó un retiro real mediante el BFF ATM.

Datos:

```text
Cuenta: 101
Saldo inicial: 4950.00
Monto retirado: 100.00
Saldo resultante: 4850.00
Estado: APROBADO
```

El flujo observado fue:

```text
BFF ATM
   |
   v
Bank Core API
   |
   +--> MySQL
   |
   +--> AFTER_COMMIT
           |
           v
          Kafka
           |
           v
   banco.movimientos
           |
           v
     audit-service
```

Kafka registró:

```text
Key: 101
Partition: 2
Offset: 0
Evento: RETIRO_REALIZADO
Saldo resultante: 4850.00
```

Posteriormente, `audit-service` procesó el mismo evento.

---

## 16. Seguridad

Los BFF mantienen:

- Spring Security;
- JWT;
- OAuth2 Resource Server;
- HTTPS;
- scopes independientes;
- secretos JWT separados por canal.

Scopes:

```text
WEB_ACCESS
MOBILE_ACCESS
ATM_ACCESS
```

Comportamiento validado:

```text
Sin token           -> 401 Unauthorized
Token correcto      -> acceso permitido
Token de otro canal -> acceso rechazado
```

El certificado PKCS12 se mantiene fuera del repositorio Git.

---

## 17. Manejo de errores

La solución mantiene manejo controlado de errores funcionales y de infraestructura.

Ejemplos:

```text
400 Bad Request
401 Unauthorized
404 Not Found
409 Conflict
503 Service Unavailable
```

Ejemplos de códigos propios:

```text
SALDO_INSUFICIENTE
CORE_NO_DISPONIBLE
```

Como mejora futura se propone estandarizar una estructura común de error para todos los BFF y el Core API.

---

## 18. Observabilidad

Spring Boot Actuator permite consultar el estado de los servicios.

Ejemplo:

```text
/actuator/health
```

En el BFF ATM también se utilizaron métricas de Resilience4j para verificar los estados del Circuit Breaker:

```text
resilience4j.circuitbreaker.state
resilience4j.circuitbreaker.calls
resilience4j.circuitbreaker.failure.rate
resilience4j.circuitbreaker.not.permitted.calls
```

Estas métricas permitieron comprobar los estados `OPEN`, `HALF_OPEN` y `CLOSED`.

---

## 19. Compilación y validación

Se ejecutó:

```powershell
.\mvnw.cmd verify
```

Resultado:

```text
Banco XYZ - Exp3 Semana 7 ........ SUCCESS
Banco XYZ - Config Server ........ SUCCESS
Banco XYZ - Discovery Server ..... SUCCESS
Banco XYZ - Core API ............. SUCCESS
Banco XYZ - BFF Web .............. SUCCESS
Banco XYZ - BFF Mobile ........... SUCCESS
Banco XYZ - BFF ATM .............. SUCCESS
Banco XYZ - Audit Service ........ SUCCESS

BUILD SUCCESS
```

Esto valida la compilación integrada de los ocho módulos Maven del proyecto.

---

## 20. Decisiones técnicas

### Kafka en lugar de JMS

Se seleccionó Kafka porque el escenario requiere comunicación orientada a eventos y permite demostrar particionamiento, retención de eventos y escalabilidad mediante grupos de consumidores.

### Tres particiones

El tópico utiliza tres particiones para permitir distribución de carga entre consumidores.

### `cuentaId` como key

Permite mantener juntos los eventos de una misma cuenta y conservar su orden relativo.

### AFTER_COMMIT

Evita publicar un evento de negocio antes de que la operación haya sido confirmada en MySQL.

### Audit Service desacoplado

El consumidor no forma parte del Core API. Esto permite que el procesamiento de auditoría evolucione y escale de manera independiente.

---

## 21. Limitaciones del entorno de desarrollo

La implementación de Semana 7 se ejecuta en un entorno local de desarrollo.

Kafka utiliza:

```text
1 broker
replication-factor = 1
```

Esto es suficiente para demostrar funcionalidad, particionamiento y escalabilidad de consumidores, pero no representa una topología de alta disponibilidad para producción.

El `audit-service` actualmente demuestra consumo y procesamiento mediante logs; una evolución futura podría persistir auditorías en una base de datos independiente.

---

## 22. Mejoras futuras

Como evolución de la solución se consideran:

- propagar identidad delegable hacia el Core API para mejorar trazabilidad y auditoría;
- estandarizar códigos y estructuras de error entre BFF y Core;
- persistir auditorías procesadas por `audit-service`;
- incorporar idempotencia del consumidor;
- agregar una estrategia de Dead Letter Topic para eventos que no puedan procesarse;
- desplegar Kafka con replicación real para alta disponibilidad;
- incorporar métricas y dashboards de observabilidad;
- externalizar más configuraciones de los servicios mediante Config Server.

---

## 23. Tecnologías utilizadas

- Java;
- Spring Boot 4;
- Spring Cloud Config;
- Netflix Eureka;
- Spring Security;
- OAuth2 Resource Server;
- JWT;
- Resilience4j;
- Spring Data JPA;
- Hibernate;
- MySQL;
- Apache Kafka;
- Spring Kafka;
- Docker;
- Maven;
- HTTPS / TLS;
- Spring Boot Actuator;
- Git;
- GitHub.

---

## 24. Conclusión

La solución de Banco XYZ evoluciona desde una arquitectura BFF hacia una arquitectura de microservicios con mecanismos de configuración centralizada, descubrimiento, seguridad, resiliencia y mensajería asíncrona.

Resilience4j permite controlar fallas de comunicación entre el BFF ATM y el Core API, entregando respuestas controladas cuando el servicio central no está disponible.

Apache Kafka desacopla la generación de movimientos bancarios de su procesamiento posterior. El tópico `banco.movimientos`, junto con tres particiones y el grupo `banco-auditoria`, permite distribuir eventos entre múltiples consumidores y demostrar escalabilidad horizontal.

La publicación posterior al COMMIT mantiene coherencia entre la operación persistida y el evento emitido, mientras que la separación entre productores y consumidores mantiene una arquitectura modular, extensible y preparada para futuras mejoras.

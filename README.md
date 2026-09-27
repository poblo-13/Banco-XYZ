# Banco XYZ - Arquitectura de Microservicios, Resiliencia y Eventos

## Exp3 - Semana 7

Proyecto desarrollado para la asignatura **Desarrollo Backend III (PBY2203)**.

La solución evoluciona la arquitectura Backend for Frontend (BFF) implementada en semanas anteriores e incorpora **configuración centralizada, descubrimiento de servicios, tolerancia a fallos con Resilience4j y mensajería asíncrona con Apache Kafka**.

---

## Objetivo

Implementar una arquitectura de microservicios para Banco XYZ capaz de:

- mantener BFF independientes para Web, Mobile y ATM;
- centralizar configuraciones mediante Spring Cloud Config;
- registrar y descubrir servicios mediante Eureka;
- aplicar tolerancia a fallos con Resilience4j;
- publicar eventos bancarios mediante Apache Kafka;
- procesar eventos de manera asíncrona;
- demostrar escalabilidad mediante particiones y grupos de consumidores.

---

## Estructura del proyecto

```text
Exp3_S7_Grupo14
|
|-- config-server
|-- discovery-server
|-- bank-core-api
|-- bff-web
|-- bff-mobile
|-- bff-atm
|-- audit-service
|-- config-repo
|-- certs
|-- docker-compose.kafka.yml
|-- pom.xml
|-- README.md
|-- mvnw
`-- mvnw.cmd
```

---

## Servicios

| Servicio | Puerto | Función |
|---|---:|---|
| Config Server | 8888 | Configuración centralizada |
| Discovery Server / Eureka | 8761 | Registro y descubrimiento |
| Bank Core API | 8080 | Lógica bancaria y persistencia |
| BFF Web | 8081 | Canal Web |
| BFF Mobile | 8082 | Canal Mobile |
| BFF ATM | 8083 | Canal ATM |
| Kafka | 9092 | Broker de eventos |
| Audit Service | Sin puerto HTTP | Consumidor asíncrono Kafka |

---

## Arquitectura general

```text
                         +----------------------+
                         |    Config Server     |
                         |        :8888         |
                         +----------+-----------+
                                    |
                                    v
+-----------+     +-----------+     +------------------+     +---------+
| BFF Web   |     | BFF Mobile|     |     BFF ATM      |     | Eureka  |
| :8081     |     | :8082     |     |      :8083       |     | :8761   |
+-----+-----+     +-----+-----+     +---------+--------+     +----+----+
      |                 |                       |                   ^
      |                 |                       | Resilience4j      |
      |                 |                       v                   |
      +-----------------+----------------> +----------------+ ------+
                                         | Bank Core API  |
                                         |     :8080       |
                                         +-------+--------+
                                                 |
                              +------------------+------------------+
                              |                                     |
                              v                                     v
                         +---------+                         +---------------+
                         |  MySQL  |                         |     Kafka     |
                         | banco_  |                         |    :9092      |
                         | xyz_core|                         +-------+-------+
                         +---------+                                 |
                                                                    v
                                                         banco.movimientos
                                                           3 particiones
                                                                    |
                                                                    v
                                                           +----------------+
                                                           | Audit Service  |
                                                           | banco-auditoria|
                                                           +----------------+
```

---

## Config Server

El módulo `config-server` implementa Spring Cloud Config Server.

Puerto:

```text
8888
```

El Core API consume configuración centralizada desde:

```text
config-repo/bank-core-api.properties
```

Entre las propiedades configuradas se encuentran:

- URL de Eureka;
- configuración del productor Kafka;
- broker `localhost:9092`;
- tópico `banco.movimientos`;
- propiedades de observabilidad.

Ejemplo de consulta:

```text
http://localhost:8888/bank-core-api/default
```

---

## Service Discovery con Eureka

El módulo `discovery-server` implementa Eureka Server.

Puerto:

```text
8761
```

Dashboard:

```text
http://localhost:8761
```

El servicio `BANK-CORE-API` se registra dinámicamente y fue validado con estado:

```text
UP
```

---

## Bank Core API

El módulo `bank-core-api` centraliza la lógica bancaria y utiliza Spring Data JPA con MySQL.

Funciones principales:

- consulta de cuentas;
- consulta de movimientos;
- depósitos;
- retiros;
- compras;
- actualización de saldos;
- validación de saldo;
- persistencia de operaciones;
- publicación de eventos Kafka.

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

## BFF Web

URL base:

```text
https://localhost:8081
```

Endpoints principales:

```text
POST /auth/token
GET  /api/web/cuentas
GET  /api/web/cuentas/{cuentaId}
GET  /api/web/cuentas/{cuentaId}/movimientos
GET  /api/web/dashboard/{cuentaId}
```

Scope:

```text
WEB_ACCESS
```

---

## BFF Mobile

URL base:

```text
https://localhost:8082
```

Endpoints principales:

```text
POST /auth/token
GET  /api/mobile/cuentas/{cuentaId}
GET  /api/mobile/cuentas/{cuentaId}/movimientos-recientes
GET  /api/mobile/resumen/{cuentaId}
```

Scope:

```text
MOBILE_ACCESS
```

---

## BFF ATM

URL base:

```text
https://localhost:8083
```

Endpoints principales:

```text
POST /auth/token
GET  /api/atm/saldo/{cuentaId}
POST /api/atm/retiros
```

Scope:

```text
ATM_ACCESS
```

---

## Tolerancia a fallos con Resilience4j

El BFF ATM utiliza un Circuit Breaker denominado:

```text
coreApi
```

La protección se aplica sobre las consultas al Core API.

Configuración principal:

```text
sliding-window-size: 3
minimum-number-of-calls: 3
failure-rate-threshold: 50%
wait-duration-in-open-state: 10s
permitted-number-of-calls-in-half-open-state: 1
automatic-transition-from-open-to-half-open-enabled: true
```

Cuando el Core API no se encuentra disponible, el ATM responde de manera controlada:

```text
HTTP 503 Service Unavailable
CORE_NO_DISPONIBLE
```

Durante las pruebas se verificó el ciclo:

```text
CLOSED
  ↓
OPEN
  ↓
HALF_OPEN
  ↓
CLOSED
```

También se comprobó que los errores funcionales, como una cuenta inexistente, no se contabilizan como fallas de infraestructura.

---

## Arquitectura orientada a eventos con Kafka

Apache Kafka se ejecuta mediante Docker utilizando:

```text
docker-compose.kafka.yml
```

Broker:

```text
localhost:9092
```

Tópico:

```text
banco.movimientos
```

Configuración del tópico:

```text
Particiones: 3
Factor de replicación: 1
```

---

## Eventos bancarios

El Core API publica eventos asociados a operaciones bancarias.

Eventos implementados:

```text
DEPOSITO_REALIZADO
RETIRO_REALIZADO
COMPRA_REALIZADA
```

Ejemplo real validado:

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

La clave Kafka utilizada es:

```text
cuentaId
```

Esto permite que los eventos de una misma cuenta se mantengan dentro de una misma partición y conserven su orden relativo.

---

## Publicación posterior al COMMIT

El Core API publica primero un evento interno mediante `ApplicationEventPublisher`.

El envío a Kafka se realiza con:

```text
@TransactionalEventListener(
    phase = TransactionPhase.AFTER_COMMIT
)
```

De esta forma, el evento Kafka se publica solo después de que la transacción bancaria se confirma correctamente en la base de datos.

---

## Audit Service

El módulo:

```text
audit-service
```

consume los eventos del tópico:

```text
banco.movimientos
```

Grupo de consumidores:

```text
banco-auditoria
```

Cada evento procesado registra:

- clave;
- partición;
- offset;
- payload JSON.

Ejemplo validado:

```text
AUDITORIA [auditoria-1] evento procesado:
key=101
particion=2
offset=0
payload={"tipoEvento":"RETIRO_REALIZADO", ...}
```

---

## Escalabilidad del consumidor

Se ejecutaron simultáneamente dos instancias de `audit-service` dentro del mismo grupo:

```text
banco-auditoria
```

Kafka realizó automáticamente el rebalanceo de las tres particiones.

Distribución observada:

```text
Consumidor 1 -> particiones 0 y 1
Consumidor 2 -> partición 2
```

El comando:

```powershell
docker exec -it banco-kafka `
/opt/kafka/bin/kafka-consumer-groups.sh `
--bootstrap-server localhost:9092 `
--describe `
--group banco-auditoria
```

permitió verificar dos `CONSUMER-ID` distintos y un reparto efectivo de las particiones.

Esto demuestra escalabilidad horizontal mediante Consumer Groups.

---

## Prueba funcional ATM -> Core -> Kafka -> Audit Service

Se realizó un retiro real sobre la cuenta `101`.

Resultado:

```text
Saldo inicial:      4950.00
Monto retirado:      100.00
Saldo resultante:   4850.00
Estado:             APROBADO
```

Kafka recibió:

```text
Topic: banco.movimientos
Key: 101
Partition: 2
Offset: 0
Evento: RETIRO_REALIZADO
Saldo resultante: 4850.00
```

Posteriormente, `audit-service` procesó el mismo evento de manera asíncrona.

---

## Seguridad

Los BFF utilizan:

- Spring Security;
- JWT;
- OAuth2 Resource Server;
- HTTPS;
- autorización por scope;
- secretos JWT independientes por canal.

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

---

## HTTPS

Para desarrollo local se utiliza un certificado PKCS12.

Ruta:

```text
certs/banco-xyz-local.p12
```

El certificado y sus credenciales se mantienen fuera del repositorio mediante `.gitignore`.

---

## Variables de entorno

### Core API

```powershell
$env:DB_PASSWORD="TU_PASSWORD_MYSQL"
```

### Web

```powershell
$env:WEB_JWT_SECRET="TU_SECRETO_JWT"
$env:WEB_AUTH_USERNAME="webuser"
$env:WEB_AUTH_PASSWORD="web12345"
```

### Mobile

```powershell
$env:MOBILE_JWT_SECRET="TU_SECRETO_JWT"
$env:MOBILE_AUTH_USERNAME="mobileuser"
$env:MOBILE_AUTH_PASSWORD="mobile12345"
```

### ATM

```powershell
$env:ATM_JWT_SECRET="TU_SECRETO_JWT"
$env:ATM_AUTH_USERNAME="atmuser"
$env:ATM_AUTH_PASSWORD="atm12345"
```

### HTTPS

```powershell
$env:SSL_KEYSTORE_PASSWORD="TU_PASSWORD_SSL"
$env:SSL_KEYSTORE_PATH="file:C:/ruta/proyecto/certs/banco-xyz-local.p12"
```

---

## Requisitos

- Java 21 o superior;
- Maven Wrapper incluido;
- MySQL;
- Docker Desktop;
- PowerShell;
- `keytool` disponible desde el JDK.

Durante el desarrollo y pruebas se utilizó Java 22.

---

## Ejecución

Cada servicio puede ejecutarse en una terminal independiente.

### 1. Kafka

```powershell
docker compose -f docker-compose.kafka.yml up -d
```

Crear el tópico si no existe:

```powershell
docker exec -it banco-kafka `
/opt/kafka/bin/kafka-topics.sh `
--bootstrap-server localhost:9092 `
--create `
--if-not-exists `
--topic banco.movimientos `
--partitions 3 `
--replication-factor 1
```

### 2. Config Server

```powershell
.\mvnw.cmd -pl config-server spring-boot:run
```

### 3. Discovery Server

```powershell
.\mvnw.cmd -pl discovery-server spring-boot:run
```

### 4. Core API

Configurar previamente `DB_PASSWORD` y ejecutar:

```powershell
.\mvnw.cmd -pl bank-core-api spring-boot:run
```

### 5. BFF Web

```powershell
.\mvnw.cmd -pl bff-web spring-boot:run
```

### 6. BFF Mobile

```powershell
.\mvnw.cmd -pl bff-mobile spring-boot:run
```

### 7. BFF ATM

```powershell
.\mvnw.cmd -pl bff-atm spring-boot:run
```

### 8. Audit Service

```powershell
$env:AUDIT_INSTANCE="auditoria-1"
.\mvnw.cmd -pl audit-service spring-boot:run
```

Para una segunda instancia:

```powershell
$env:AUDIT_INSTANCE="auditoria-2"
.\mvnw.cmd -pl audit-service spring-boot:run
```

---

## Compilación general

Desde la raíz del proyecto:

```powershell
.\mvnw.cmd verify
```

Resultado validado:

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

---

## Tecnologías utilizadas

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

## Conclusión

La solución de Banco XYZ evolucionó desde una arquitectura BFF hacia una arquitectura de microservicios con configuración centralizada, descubrimiento de servicios, seguridad, resiliencia y comunicación asíncrona.

Resilience4j permite controlar fallas de comunicación entre el BFF ATM y el Core API, mientras que Kafka desacopla el procesamiento de eventos bancarios mediante el tópico `banco.movimientos`.

La utilización de tres particiones y múltiples consumidores dentro del grupo `banco-auditoria` permite distribuir la carga de procesamiento y demuestra una arquitectura extensible y preparada para escalar horizontalmente.

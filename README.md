# Banco XYZ - Arquitectura de Microservicios, OAuth2, Resiliencia y Eventos

## Exp3 - Semana 8

Proyecto desarrollado para la asignatura **Desarrollo Backend III (PBY2203)**.

Esta versión continúa el trabajo realizado en semanas anteriores sobre la arquitectura Backend for Frontend (BFF) de Banco XYZ. En Semana 8 se incorporan dos cambios principales: **seguridad centralizada con OAuth2** y **despliegue completo mediante Docker Compose**.

También se mantienen los componentes ya implementados: Spring Cloud Config, Eureka, Resilience4j, Apache Kafka, MySQL y el servicio de auditoría.

---

## Objetivo

El objetivo de esta etapa es dejar la solución preparada para funcionar como un ecosistema de microservicios integrado, capaz de:

- mantener BFF independientes para Web, Mobile y ATM;
- centralizar la emisión de tokens mediante OAuth2;
- proteger cada canal mediante scopes distintos;
- centralizar configuración mediante Spring Cloud Config;
- registrar servicios mediante Eureka;
- controlar fallas de comunicación con Resilience4j;
- persistir operaciones bancarias en MySQL;
- publicar eventos mediante Apache Kafka;
- procesar eventos mediante consumidores independientes;
- demostrar escalabilidad con dos instancias de `audit-service`;
- dockerizar los microservicios;
- levantar la solución completa mediante Docker Compose.

---

## Estructura del proyecto

```text
Exp3_S8_Pablo_Pilar
|
|-- authorization-server
|-- config-server
|-- discovery-server
|-- bank-core-api
|-- bff-web
|-- bff-mobile
|-- bff-atm
|-- audit-service
|-- config-repo
|-- certs
|-- evidencias
|-- docker-compose.yml
|-- docker-compose.kafka.yml
|-- .dockerignore
|-- .env.example
|-- pom.xml
|-- README.md
|-- PROPUESTA_TECNICA.md
|-- EVIDENCIA_EJECUCION.md
|-- DIAGRAMA_ARQUITECTURA.md
|-- mvnw
`-- mvnw.cmd
```

---

## Servicios

| Servicio | Puerto | Función |
|---|---:|---|
| Authorization Server | 9000 | Emisión centralizada de tokens OAuth2 |
| Config Server | 8888 | Configuración centralizada |
| Discovery Server / Eureka | 8761 | Registro y descubrimiento |
| Bank Core API | 8080 | Lógica bancaria y persistencia |
| BFF Web | 8081 | API específica para canal Web |
| BFF Mobile | 8082 | API específica para canal Mobile |
| BFF ATM | 8083 | API específica para canal ATM |
| MySQL | 3307 host / 3306 interno | Base de datos del Core |
| Kafka | 9092 | Broker de eventos |
| Audit Service 1 | Sin puerto HTTP | Consumidor Kafka |
| Audit Service 2 | Sin puerto HTTP | Segunda instancia del consumidor |

---

## Arquitectura general

```text
                    +---------------------------+
                    |   Authorization Server    |
                    |           :9000           |
                    | OAuth2 client_credentials |
                    +-------------+-------------+
                                  |
               +------------------+------------------+
               |                  |                  |
               v                  v                  v
        +-------------+    +-------------+    +-------------+
        |   BFF Web   |    | BFF Mobile  |    |   BFF ATM   |
        |    :8081    |    |    :8082    |    |    :8083    |
        | WEB_ACCESS  |    |MOBILE_ACCESS|    | ATM_ACCESS  |
        +------+------+    +------+------+    +------+------+
               \                  |                  /
                \                 |                 /
                 +----------------+----------------+
                                  |
                           Resilience4j
                                  |
                                  v
                          +---------------+
                          | Bank Core API |
                          |     :8080     |
                          +-------+-------+
                                  |
                      +-----------+-----------+
                      |                       |
                      v                       v
                 +---------+             +---------+
                 |  MySQL  |             |  Kafka  |
                 |  :3306  |             |  :9092  |
                 +---------+             +----+----+
                                             |
                                      banco.movimientos
                                        3 particiones
                                             |
                              +--------------+--------------+
                              |                             |
                              v                             v
                    +------------------+           +------------------+
                    | Audit Service 1  |           | Audit Service 2  |
                    | banco-auditoria  |           | banco-auditoria  |
                    +------------------+           +------------------+

          Config Server :8888             Eureka :8761
```

---

## Authorization Server y OAuth2

El módulo `authorization-server` centraliza la emisión de tokens.

Puerto:

```text
9000
```

Flujo utilizado:

```text
client_credentials
```

Este flujo se utiliza para autenticación entre clientes o servicios. En esta implementación no representa un inicio de sesión de usuario final.

Clientes configurados:

| Cliente | Scope |
|---|---|
| `banco-web` | `WEB_ACCESS` |
| `banco-mobile` | `MOBILE_ACCESS` |
| `banco-atm` | `ATM_ACCESS` |

Endpoint de token:

```text
POST /oauth2/token
```

Los secretos se reciben mediante variables de entorno y no se guardan directamente en el repositorio.

---

## Seguridad de los BFF

Los tres BFF funcionan como **OAuth2 Resource Server**.

Cada canal exige su propio scope:

```text
/api/web/**     -> SCOPE_WEB_ACCESS
/api/mobile/**  -> SCOPE_MOBILE_ACCESS
/api/atm/**     -> SCOPE_ATM_ACCESS
```

Comportamiento validado:

```text
Sin token                 -> 401 Unauthorized
Token con scope correcto  -> acceso permitido
Token de otro canal       -> 403 Forbidden
```

La generación local de JWT utilizada en versiones anteriores fue eliminada y quedó centralizada en `authorization-server`.

---

## BFF Web

Puerto:

```text
8081
```

Endpoints principales:

```text
GET /api/web/cuentas
GET /api/web/cuentas/{cuentaId}
GET /api/web/cuentas/{cuentaId}/movimientos
GET /api/web/dashboard/{cuentaId}
```

Scope requerido:

```text
WEB_ACCESS
```

El canal Web entrega una vista más completa de las cuentas y movimientos.

---

## BFF Mobile

Puerto:

```text
8082
```

Endpoints principales:

```text
GET /api/mobile/cuentas/{cuentaId}
GET /api/mobile/cuentas/{cuentaId}/movimientos-recientes
GET /api/mobile/resumen/{cuentaId}
```

Scope requerido:

```text
MOBILE_ACCESS
```

El canal Mobile entrega una respuesta más resumida y orientada a consultas rápidas.

---

## BFF ATM

Puerto:

```text
8083
```

Endpoints principales:

```text
GET  /api/atm/saldo/{cuentaId}
POST /api/atm/retiros
```

Scope requerido:

```text
ATM_ACCESS
```

El canal ATM expone solamente las operaciones necesarias para un cajero automático.

---

## Bank Core API

El módulo `bank-core-api` concentra la lógica bancaria.

Entre sus responsabilidades se encuentran:

- consulta de cuentas;
- consulta de movimientos;
- depósitos;
- retiros;
- compras;
- validación de saldo;
- actualización de saldos;
- persistencia mediante Spring Data JPA;
- publicación de eventos bancarios.

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

## Config Server

El módulo `config-server` implementa Spring Cloud Config Server.

Puerto:

```text
8888
```

El Core obtiene parte de su configuración desde:

```text
config-repo/bank-core-api.properties
```

Entre las propiedades configuradas se encuentran:

- URL de Eureka;
- broker Kafka;
- serializadores Kafka;
- tópico `banco.movimientos`;
- propiedades de observabilidad.

---

## Eureka

El módulo `discovery-server` implementa Eureka Server.

Puerto:

```text
8761
```

Dashboard:

```text
http://localhost:8761
```

El servicio `BANK-CORE-API` se registra dinámicamente.

Durante la validación final se observó:

```text
registration status: 204
```

y estado:

```text
UP
```

---

## Resilience4j

Los BFF Web, Mobile y ATM utilizan el Circuit Breaker:

```text
coreApi
```

Configuración principal:

```text
sliding-window-type: COUNT_BASED
sliding-window-size: 3
minimum-number-of-calls: 3
failure-rate-threshold: 50
wait-duration-in-open-state: 10s
permitted-number-of-calls-in-half-open-state: 1
automatic-transition-from-open-to-half-open-enabled: true
```

Cuando el Core no está disponible, los BFF responden de manera controlada:

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

Los errores funcionales esperados, como un recurso inexistente o saldo insuficiente, no se consideran una caída de infraestructura.

---

## Kafka y eventos

Kafka se utiliza para desacoplar el procesamiento posterior de los movimientos bancarios.

Tópico:

```text
banco.movimientos
```

Configuración:

```text
Particiones: 3
Factor de replicación: 1
```

Eventos implementados:

```text
DEPOSITO_REALIZADO
RETIRO_REALIZADO
COMPRA_REALIZADA
```

El contrato del evento contiene información como:

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

La key utilizada es:

```text
cuentaId
```

Esto permite mantener el orden relativo de los movimientos pertenecientes a una misma cuenta.

---

## Publicación después del COMMIT

Los eventos se publican solamente después de confirmar la transacción en MySQL.

Se utiliza:

```text
@TransactionalEventListener(
    phase = TransactionPhase.AFTER_COMMIT
)
```

Flujo:

```text
Operación bancaria
      |
      v
Persistencia MySQL
      |
      v
COMMIT
      |
      v
Evento
      |
      v
Kafka
```

---

## Audit Service

El módulo `audit-service` consume los eventos del tópico:

```text
banco.movimientos
```

Grupo:

```text
banco-auditoria
```

Docker Compose ejecuta dos instancias:

```text
audit-service-1
audit-service-2
```

Durante las pruebas, Kafka distribuyó las tres particiones entre ambas instancias.

Distribución observada:

```text
audit-service-1 -> partición 2
audit-service-2 -> particiones 0 y 1
```

Esto permite demostrar escalabilidad horizontal mediante Consumer Groups.

---

## Prueba funcional ATM -> Core -> Kafka -> Audit

Se realizó un retiro sobre la cuenta `101`.

Datos:

```text
Saldo inicial:      100000.00
Monto retirado:      10000.00
Saldo resultante:    90000.00
Estado:              APROBADO
```

Respuesta del ATM:

```json
{
  "cuentaId": 101,
  "montoRetirado": 10000,
  "saldoDisponible": 90000.00,
  "estado": "APROBADO"
}
```

Después de la operación se confirmó el nuevo saldo desde el Core y se observó el evento:

```text
RETIRO_REALIZADO
```

con:

```text
key = 101
partition = 2
offset = 0
saldoResultante = 90000.00
```

El evento fue consumido por una de las instancias de `audit-service`.

---

## Dockerización

Cada microservicio principal posee su propio `Dockerfile`.

Imágenes utilizadas:

```text
banco-xyz-authorization-server:semana8
banco-xyz-config-server:semana8
banco-xyz-discovery-server:semana8
banco-xyz-bank-core-api:semana8
banco-xyz-bff-web:semana8
banco-xyz-bff-mobile:semana8
banco-xyz-bff-atm:semana8
banco-xyz-audit-service:semana8
mysql:8.0
apache/kafka:4.3.1
```

Los Dockerfile utilizan construcción multi-stage con Maven y Java.

---

## Docker Compose

El archivo:

```text
docker-compose.yml
```

levanta los siguientes servicios:

```text
mysql
kafka
config-server
discovery-server
authorization-server
bank-core-api
bff-web
bff-mobile
bff-atm
audit-service-1
audit-service-2
```

En total se levantan once contenedores.

Para iniciar:

```powershell
docker compose up -d --build
```

Para revisar:

```powershell
docker compose ps
```

Para detener conservando los volúmenes:

```powershell
docker compose down
```

Para eliminar también los volúmenes:

```powershell
docker compose down -v
```

---

## Healthchecks

MySQL y Kafka poseen `healthcheck`.

Durante la validación final se observó:

```text
mysql -> healthy
kafka -> healthy
```

El Core inicia después de que estos servicios están disponibles.

Logs validados:

```text
HikariPool-1 - Start completed.
Started BankCoreApiApplication
registration status: 204
```

No volvió a aparecer el error de detección del dialecto de Hibernate observado en un arranque anterior.

---

## Variables de entorno

El archivo real:

```text
.env
```

se mantiene fuera del repositorio.

El proyecto incluye:

```text
.env.example
```

Variables principales:

```text
MYSQL_ROOT_PASSWORD
OAUTH_WEB_CLIENT_SECRET
OAUTH_MOBILE_CLIENT_SECRET
OAUTH_ATM_CLIENT_SECRET
```

Para preparar un entorno local:

```powershell
Copy-Item .env.example .env
```

Luego se deben reemplazar los valores de ejemplo por credenciales locales.

---

## HTTPS

Para ejecución local, los BFF pueden utilizar HTTPS mediante un certificado PKCS12.

Ruta utilizada:

```text
certs/banco-xyz-local.p12
```

El certificado está excluido de Git.

Variables relacionadas:

```text
SSL_ENABLED
SSL_KEYSTORE_PATH
SSL_KEYSTORE_PASSWORD
```

Dentro de Docker Compose los BFF se ejecutan mediante HTTP en la red interna:

```text
SSL_ENABLED=false
```

La autorización de los endpoints continúa protegida mediante OAuth2 y JWT.

---

## Obtención de token OAuth2

### Web

```powershell
$pair = "banco-web:$env:OAUTH_WEB_CLIENT_SECRET"
$basic = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))

$tokenWeb = (Invoke-RestMethod `
    -Method Post `
    -Uri "http://127.0.0.1:9000/oauth2/token" `
    -Headers @{ Authorization = "Basic $basic" } `
    -ContentType "application/x-www-form-urlencoded" `
    -Body "grant_type=client_credentials&scope=WEB_ACCESS").access_token
```

### Mobile

```powershell
$pair = "banco-mobile:$env:OAUTH_MOBILE_CLIENT_SECRET"
$basic = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))

$tokenMobile = (Invoke-RestMethod `
    -Method Post `
    -Uri "http://127.0.0.1:9000/oauth2/token" `
    -Headers @{ Authorization = "Basic $basic" } `
    -ContentType "application/x-www-form-urlencoded" `
    -Body "grant_type=client_credentials&scope=MOBILE_ACCESS").access_token
```

### ATM

```powershell
$pair = "banco-atm:$env:OAUTH_ATM_CLIENT_SECRET"
$basic = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))

$tokenAtm = (Invoke-RestMethod `
    -Method Post `
    -Uri "http://127.0.0.1:9000/oauth2/token" `
    -Headers @{ Authorization = "Basic $basic" } `
    -ContentType "application/x-www-form-urlencoded" `
    -Body "grant_type=client_credentials&scope=ATM_ACCESS").access_token
```

---

## Ejemplos de prueba

### Web

```powershell
Invoke-RestMethod `
    -Uri "http://127.0.0.1:8081/api/web/cuentas" `
    -Headers @{ Authorization = "Bearer $tokenWeb" }
```

### Mobile

```powershell
Invoke-RestMethod `
    -Uri "http://127.0.0.1:8082/api/mobile/cuentas/101" `
    -Headers @{ Authorization = "Bearer $tokenMobile" }
```

### ATM

```powershell
Invoke-RestMethod `
    -Uri "http://127.0.0.1:8083/api/atm/saldo/101" `
    -Headers @{ Authorization = "Bearer $tokenAtm" }
```

---

## Requisitos

- Java 21 o superior;
- Maven 3.9.x o Maven Wrapper;
- Docker Desktop;
- PowerShell;
- `keytool` para pruebas HTTPS locales.

Durante las pruebas finales se utilizó:

```text
Java 22.0.1
Maven 3.9.9
```

El proyecto compila con:

```text
release 21
```

---

## Compilación general

Desde la raíz:

```powershell
mvn clean verify
```

Resultado validado:

```text
Banco XYZ - Exp3 Semana 8 .......................... SUCCESS
Banco XYZ - Config Server .......................... SUCCESS
Banco XYZ - Discovery Server ....................... SUCCESS
Banco XYZ - Core API ............................... SUCCESS
Banco XYZ - BFF Web ................................ SUCCESS
Banco XYZ - BFF Mobile ............................. SUCCESS
Banco XYZ - BFF ATM ................................ SUCCESS
Banco XYZ - Audit Service .......................... SUCCESS
Banco XYZ - Authorization Server ................... SUCCESS

BUILD SUCCESS
```

---

## Seguridad de archivos sensibles

Antes de preparar la entrega se verificó que Git no estuviera rastreando:

```text
.env
*.p12
*.jks
*.keystore
```

También se revisó que los valores reales almacenados en `.env` no aparecieran en otros archivos del proyecto.

Resultado:

```text
OK - No se encontraron secretos del .env fuera del archivo .env
```

---

## Tecnologías utilizadas

- Java 21;
- Spring Boot 4.0.7;
- Spring Cloud 2025.1.2;
- Spring Cloud Config;
- Netflix Eureka;
- Spring Security;
- Spring Authorization Server;
- OAuth2;
- JWT;
- Resilience4j;
- Spring Data JPA;
- Hibernate;
- MySQL 8;
- Apache Kafka 4.3.1;
- Spring Kafka;
- Docker;
- Docker Compose;
- Maven;
- HTTPS / TLS;
- Spring Boot Actuator;
- Git;
- GitHub.

---

## Documentación complementaria

El proyecto incluye:

```text
PROPUESTA_TECNICA.md
EVIDENCIA_EJECUCION.md
DIAGRAMA_ARQUITECTURA.md
```

Estos archivos complementan este README con la explicación de las decisiones técnicas, las pruebas realizadas y el esquema de arquitectura.

---

## Conclusión

La versión de Semana 8 integra en una misma solución los componentes trabajados anteriormente y agrega seguridad OAuth2 centralizada y despliegue mediante Docker Compose.

Los tres BFF mantienen funciones diferentes para Web, Mobile y ATM. El Core concentra la lógica bancaria y la persistencia. Resilience4j controla fallas de comunicación y Kafka permite procesar eventos de manera asíncrona.

La ejecución de dos instancias de auditoría demuestra el reparto de particiones dentro de un mismo Consumer Group, mientras que Docker Compose permite levantar el ecosistema de forma ordenada y reproducible.

Con la compilación final, las pruebas de OAuth2, los healthchecks y el flujo ATM -> Core -> Kafka -> Audit se valida el funcionamiento integrado de la solución de Semana 8.

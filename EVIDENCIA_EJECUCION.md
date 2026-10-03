# Evidencias de Ejecución
## Banco XYZ - OAuth2, Resiliencia, Kafka y Docker Compose
### Exp3 - Semana 8

Este documento reúne las principales pruebas realizadas sobre la solución Banco XYZ durante Semana 8 de la asignatura **Desarrollo Backend III (PBY2203)**.

La versión de esta semana mantiene la arquitectura trabajada anteriormente y agrega principalmente:

- un Authorization Server centralizado;
- autenticación OAuth2 mediante `client_credentials`;
- scopes separados para Web, Mobile y ATM;
- Resilience4j en los tres BFF;
- Dockerfile para los microservicios;
- ejecución completa mediante Docker Compose;
- healthchecks para MySQL y Kafka;
- dos instancias de `audit-service`;
- compilación integrada de todos los módulos.

Las pruebas anteriores de configuración, Eureka, Kafka y resiliencia se mantienen como base, pero en esta semana se volvió a validar el funcionamiento dentro de la solución completa.

---

# Evidencias Semana 8

## Evidencia 01 - Authorization Server operativo

Se incorporó el módulo:

```text
authorization-server
```

El servicio se ejecuta en:

```text
http://localhost:9000
```

y centraliza la emisión de tokens para los tres canales.

Clientes configurados:

```text
banco-web
banco-mobile
banco-atm
```

Scopes:

```text
WEB_ACCESS
MOBILE_ACCESS
ATM_ACCESS
```

El flujo utilizado es:

```text
client_credentials
```

Durante las pruebas se obtuvieron tokens correctamente para Web, Mobile y ATM.

Esto confirma que la generación de JWT ya no depende de cada BFF por separado.

---

## Evidencia 02 - Acceso protegido por scope

Cada BFF valida el token recibido y exige el scope correspondiente.

Reglas implementadas:

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

Durante la prueba cruzada se utilizó un token Mobile contra un endpoint Web y la solicitud fue rechazada con `403`.

Esto demuestra que no basta con tener un token válido: también debe contener el permiso correspondiente al canal.

---

## Evidencia 03 - Docker Compose levanta el ecosistema completo

Se ejecutó:

```powershell
docker compose up -d
```

Posteriormente:

```powershell
docker compose ps
```

La salida mostró los siguientes servicios activos:

```text
audit-service-1
audit-service-2
authorization-server
bank-core-api
bff-atm
bff-mobile
bff-web
config-server
discovery-server
kafka
mysql
```

En total se levantaron once contenedores relacionados con la solución.

Los puertos publicados fueron:

```text
Authorization Server -> 9000
Core API             -> 8080
BFF Web              -> 8081
BFF Mobile           -> 8082
BFF ATM              -> 8083
Config Server        -> 8888
Eureka               -> 8761
Kafka                -> 9092
MySQL                -> 3307
```

---

## Evidencia 04 - MySQL y Kafka con healthcheck

Dentro de `docker-compose.yml` se agregaron comprobaciones de salud para MySQL y Kafka.

Al consultar el estado con:

```powershell
docker compose ps
```

se obtuvo:

```text
kafka   Up (...) (healthy)
mysql   Up (...) (healthy)
```

Los servicios que dependen de ellos iniciaron después.

Esto permitió evitar que el Core intentara conectarse a MySQL o Kafka antes de que estuvieran disponibles.

---

## Evidencia 05 - Arranque correcto del Core API

Después de incorporar los healthchecks se revisaron los logs del Core:

```powershell
docker compose logs bank-core-api |
    Select-String "Unable to determine Dialect|HikariPool-1 - Start completed|Started BankCoreApiApplication|registration status"
```

La salida fue:

```text
HikariPool-1 - Start completed.
Started BankCoreApiApplication
registration status: 204
```

No apareció:

```text
Unable to determine Dialect
```

Esto confirma que el Core logró conectarse correctamente a MySQL y registrarse en Eureka durante el arranque.

---

## Evidencia 06 - BFF Web funcionando con OAuth2

Se generó un token con:

```text
scope = WEB_ACCESS
```

y se consultó:

```text
GET /api/web/cuentas
```

El endpoint respondió correctamente utilizando el token emitido por `authorization-server`.

Después de crear la cuenta de prueba, la respuesta incluyó la cuenta:

```text
cuentaId = 101
```

Esto valida el flujo:

```text
Authorization Server
        ->
Token WEB_ACCESS
        ->
BFF Web
        ->
Bank Core API
```

---

## Evidencia 07 - BFF Mobile funcionando con OAuth2

Se generó un token con:

```text
scope = MOBILE_ACCESS
```

y se consultó:

```text
GET /api/mobile/cuentas/101
```

La solicitud fue autorizada y el BFF Mobile obtuvo correctamente los datos desde el Core.

Esto confirma que el canal Mobile utiliza el mismo Authorization Server, pero mantiene permisos propios.

---

## Evidencia 08 - BFF ATM funcionando con OAuth2

Se generó un token con:

```text
scope = ATM_ACCESS
```

y se consultó:

```text
GET /api/atm/saldo/101
```

La respuesta entregó el saldo disponible de la cuenta.

Luego se ejecutó un retiro mediante:

```text
POST /api/atm/retiros
```

Datos utilizados:

```text
Cuenta: 101
Monto: 10000
```

Respuesta obtenida:

```json
{
  "cuentaId": 101,
  "montoRetirado": 10000,
  "saldoDisponible": 90000.00,
  "estado": "APROBADO"
}
```

El Core confirmó posteriormente:

```text
saldoActual = 90000.00
```

---

## Evidencia 09 - Resilience4j en Web, Mobile y ATM

La protección mediante Resilience4j se aplicó a los tres BFF.

Circuit Breaker:

```text
coreApi
```

Configuración principal:

```text
sliding-window-size = 3
minimum-number-of-calls = 3
failure-rate-threshold = 50
wait-duration-in-open-state = 10s
permitted-number-of-calls-in-half-open-state = 1
```

Con el Core no disponible, los BFF entregan una respuesta controlada:

```text
HTTP 503
CORE_NO_DISPONIBLE
```

Se validó el ciclo:

```text
CLOSED
   ->
OPEN
   ->
HALF_OPEN
   ->
CLOSED
```

También se comprobó que errores funcionales como recurso no encontrado o saldo insuficiente no sean tratados como caída de infraestructura.

---

## Evidencia 10 - Kafka mantiene tres particiones

El tópico utilizado es:

```text
banco.movimientos
```

Configuración:

```text
PartitionCount: 3
ReplicationFactor: 1
```

Las particiones disponibles son:

```text
0
1
2
```

La key utilizada para los movimientos es:

```text
cuentaId
```

Esto permite mantener el orden relativo de los eventos pertenecientes a una misma cuenta.

---

## Evidencia 11 - Evento RETIRO_REALIZADO publicado

Después del retiro ejecutado sobre la cuenta `101`, el Core publicó el evento:

```text
RETIRO_REALIZADO
```

Datos observados:

```text
key = 101
partition = 2
offset = 0
```

El evento contenía, entre otros datos:

```text
cuentaId = 101
monto = -10000
saldoResultante = 90000.00
```

Esto comprueba el flujo:

```text
BFF ATM
   ->
Bank Core API
   ->
MySQL
   ->
COMMIT
   ->
Kafka
```

La publicación se realiza después del COMMIT mediante:

```text
@TransactionalEventListener(
    phase = TransactionPhase.AFTER_COMMIT
)
```

---

## Evidencia 12 - Dos instancias de Audit Service

Docker Compose ejecuta:

```text
audit-service-1
audit-service-2
```

Ambas instancias pertenecen al grupo:

```text
banco-auditoria
```

Durante la prueba se observó el reparto de las tres particiones entre las dos instancias.

Distribución observada:

```text
audit-service-1 -> partición 2
audit-service-2 -> particiones 0 y 1
```

El evento `RETIRO_REALIZADO` de la cuenta `101` fue consumido por:

```text
auditoria-1
```

con:

```text
partition = 2
offset = 0
```

Esto demuestra que Kafka realiza el rebalanceo y distribuye el trabajo entre los consumidores del mismo grupo.

---

## Evidencia 13 - Flujo completo ATM -> Core -> Kafka -> Audit

La prueba del retiro permitió validar el flujo completo de la solución:

```text
Authorization Server
        |
        v
Token ATM_ACCESS
        |
        v
BFF ATM
        |
        v
Bank Core API
      /     \
     /       \
  MySQL      Kafka
               |
               v
      banco.movimientos
               |
               v
        Audit Service
```

Resultado final:

```text
Saldo inicial:      100000.00
Retiro:              10000.00
Saldo final:         90000.00
Estado:              APROBADO
Evento:              RETIRO_REALIZADO
Consumidor:          auditoria-1
```

Esta prueba permitió comprobar en una sola operación seguridad, lógica bancaria, persistencia, publicación de eventos y consumo asíncrono.

---

## Evidencia 14 - Compilación completa del proyecto

Antes de cerrar la implementación se configuró Maven con Java 22.0.1 y se ejecutó:

```powershell
mvn clean verify
```

El Reactor Summary mostró:

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
```

Resultado final:

```text
BUILD SUCCESS
```

Tiempo total observado:

```text
30.232 s
```

Esto valida la compilación y empaquetado integrado de los nueve elementos del reactor Maven.

---

## Evidencia 15 - Revisión de archivos sensibles

Antes de preparar el commit se verificó que Git no estuviera rastreando archivos sensibles.

Se revisaron:

```text
.env
*.p12
*.jks
*.keystore
```

La consulta no mostró archivos sensibles rastreados.

También se comprobó que los valores reales almacenados en `.env` no aparecieran copiados en otros archivos del proyecto.

Resultado:

```text
OK - No se encontraron secretos del .env fuera del archivo .env
```

El repositorio incluye únicamente:

```text
.env.example
```

con valores de referencia.

---

# Resumen de las pruebas

Las evidencias realizadas durante Semana 8 permiten comprobar:

- emisión centralizada de tokens mediante OAuth2;
- separación de permisos mediante scopes;
- rechazo de tokens pertenecientes a otro canal;
- funcionamiento de los BFF Web, Mobile y ATM;
- tolerancia a fallos mediante Resilience4j;
- funcionamiento del Circuit Breaker;
- configuración centralizada mediante Config Server;
- registro del Core en Eureka;
- persistencia en MySQL;
- publicación de eventos mediante Kafka;
- tópico con tres particiones;
- publicación posterior al COMMIT;
- consumo asíncrono mediante `audit-service`;
- ejecución de dos consumidores del mismo grupo;
- distribución de particiones entre consumidores;
- Dockerfile para los microservicios;
- ejecución completa mediante Docker Compose;
- healthchecks de MySQL y Kafka;
- compilación integrada de los nueve módulos;
- protección de secretos fuera del repositorio Git.

---

# Relación con los criterios de Semana 8

## OAuth2

La seguridad se centraliza mediante `authorization-server`.

Se utilizan:

```text
client_credentials
WEB_ACCESS
MOBILE_ACCESS
ATM_ACCESS
```

Los BFF funcionan como OAuth2 Resource Server y validan tokens emitidos por el servicio central.

---

## Dockerización

Los módulos principales cuentan con Dockerfile:

```text
authorization-server
config-server
discovery-server
bank-core-api
bff-web
bff-mobile
bff-atm
audit-service
```

Se utilizaron imágenes multi-stage para separar la etapa de compilación de la imagen final de ejecución.

---

## Docker Compose

`docker-compose.yml` permite levantar la solución completa:

```text
11 contenedores
```

Incluye servicios de negocio, seguridad, configuración, descubrimiento, persistencia y mensajería.

---

## Resilience4j

La comunicación entre los BFF y el Core se protege mediante el Circuit Breaker:

```text
coreApi
```

Se validaron:

```text
CLOSED
OPEN
HALF_OPEN
503 CORE_NO_DISPONIBLE
```

---

## Kafka

`bank-core-api` publica eventos y `audit-service` los consume mediante:

```text
banco.movimientos
```

El tópico posee tres particiones y las dos instancias del consumidor comparten el grupo:

```text
banco-auditoria
```

---

# Conclusión

Las pruebas realizadas muestran que los componentes de Banco XYZ pueden ejecutarse de manera integrada y no solamente como servicios aislados.

OAuth2 permite centralizar la emisión de tokens y mantener permisos separados para cada BFF. Resilience4j evita que una caída del Core provoque respuestas sin controlar, mientras que Kafka permite procesar los movimientos de forma asíncrona.

Docker Compose facilita el levantamiento del entorno completo y los healthchecks ayudan a respetar el orden real de disponibilidad de MySQL y Kafka.

Finalmente, la compilación completa y las pruebas funcionales realizadas permiten comprobar que la versión de Semana 8 mantiene funcionando la base de semanas anteriores e incorpora correctamente los nuevos componentes de seguridad y despliegue.

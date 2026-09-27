# Evidencias de Ejecución
## Banco XYZ - Arquitectura de Microservicios, Resiliencia y Eventos
### Exp3 - Semana 7

Este documento reúne las evidencias principales de ejecución de la solución Banco XYZ para la actividad de Semana 7 de la asignatura **Desarrollo Backend III (PBY2203)**.

La solución mantiene la base funcional de la arquitectura Backend for Frontend desarrollada anteriormente e incorpora:

- configuración centralizada con Spring Cloud Config;
- descubrimiento de servicios con Eureka;
- tolerancia a fallos con Resilience4j;
- mensajería asíncrona con Apache Kafka;
- publicación de eventos bancarios;
- procesamiento mediante `audit-service`;
- escalabilidad horizontal mediante Consumer Groups;
- compilación integrada de los módulos del proyecto.

Las evidencias de Semana 5 se mantienen en la carpeta:

```text
evidencias/Evidencias S5/
```

Las evidencias específicas de Semana 7 se encuentran en:

```text
evidencias/Evidencias S7/
```

---

# Evidencias Semana 7

## Evidencia 01 - Config Server entrega la configuración del Core API

Se consulta:

```text
http://localhost:8888/bank-core-api/default
```

La respuesta confirma que `config-server` entrega la configuración centralizada de `bank-core-api`, incluyendo propiedades de Eureka y Kafka.

Entre las propiedades visibles se encuentran:

```text
banco.xyz.config.origen = Config Server Semana 7
eureka.client.serviceUrl.defaultZone = http://localhost:8761/eureka/
spring.kafka.bootstrap-servers = localhost:9092
banco.kafka.topic.movimientos = banco.movimientos
```

![Config Server](evidencias/Evidencias%20S7/01_Config_Server_Bank_Core_API.png)

---

## Evidencia 02 - Core API registrado en Eureka

Se observa el dashboard de Eureka en:

```text
http://localhost:8761
```

El servicio:

```text
BANK-CORE-API
```

aparece registrado con estado:

```text
UP
```

Esto demuestra el funcionamiento del mecanismo de Service Discovery.

![Eureka](evidencias/Evidencias%20S7/02_Eureka_BANK_CORE_API_UP.png)

---

## Evidencia 03 - Resilience4j con Core API disponible

Con el Core API operativo, se consulta:

```text
GET https://localhost:8083/api/atm/saldo/101
```

La respuesta es:

```text
HTTP/1.1 200
```

y entrega:

```json
{
  "cuentaId": 101,
  "saldoDisponible": 4850.00
}
```

Esto demuestra el funcionamiento normal del BFF ATM cuando su dependencia se encuentra disponible.

![Core disponible](evidencias/Evidencias%20S7/03_Resilience_Core_Disponible_200.png)

---

## Evidencia 04 - Core API no disponible y respuesta controlada

Se detiene temporalmente `bank-core-api`, manteniendo el BFF ATM operativo.

Al consultar nuevamente:

```text
GET https://localhost:8083/api/atm/saldo/101
```

se obtiene:

```text
HTTP/1.1 503
```

con el error:

```text
CORE_NO_DISPONIBLE
```

y el mensaje:

```text
El servicio central del banco no se encuentra disponible temporalmente
```

Esto demuestra que la indisponibilidad del Core es manejada de forma controlada.

![Core no disponible](evidencias/Evidencias%20S7/04_BFF_ATM_Resiliencia_CORE_NO_DISPONIBLE.png)

---

## Evidencia 05 - Circuit Breaker en estado CLOSED

Después de recuperar el Core API se consulta la métrica:

```text
resilience4j.circuitbreaker.state
```

para:

```text
name=coreApi
state=closed
```

La medición devuelve:

```json
"value": 1.0
```

confirmando que el Circuit Breaker se encuentra en estado `CLOSED`.

![Circuit Breaker CLOSED](evidencias/Evidencias%20S7/05_Circuit_Breaker_CLOSED.png)

---

## Evidencia 06 - Circuit Breaker en estado OPEN

Con el Core API detenido se realizan múltiples consultas al BFF ATM.

Las solicitudes responden con:

```text
503 CORE_NO_DISPONIBLE
```

Luego se consulta la métrica:

```text
name=coreApi
state=open
```

obteniendo:

```json
"value": 1.0
```

Esto confirma que Resilience4j abrió el circuito después de alcanzar el umbral de fallas configurado.

![Circuit Breaker OPEN](evidencias/Evidencias%20S7/06_Circuit_Breaker_OPEN.png)

---

## Evidencia 07 - Circuit Breaker en estado HALF_OPEN

Después del tiempo de espera configurado, se consulta:

```text
name=coreApi
state=half_open
```

La respuesta devuelve:

```json
"value": 1.0
```

confirmando la transición automática del Circuit Breaker al estado `HALF_OPEN`.

![Circuit Breaker HALF OPEN](evidencias/Evidencias%20S7/07_Circuit_Breaker_HALF_OPEN.png)

---

## Evidencia 08 - Tópico Kafka con tres particiones

Se describe el tópico:

```text
banco.movimientos
```

La configuración observada es:

```text
PartitionCount: 3
ReplicationFactor: 1
```

y se muestran las particiones:

```text
0
1
2
```

Esto permite distribuir eventos entre múltiples consumidores.

![Kafka tópico](evidencias/Evidencias%20S7/08_Kafka_Topic_3_Particiones.png)

---

## Evidencia 09 - Retiro aprobado mediante BFF ATM

Se ejecuta un retiro de:

```text
100.00
```

sobre la cuenta:

```text
101
```

La respuesta es:

```text
HTTP/1.1 200
```

con:

```json
{
  "cuentaId": 101,
  "montoRetirado": 100.00,
  "saldoDisponible": 4750.00,
  "estado": "APROBADO"
}
```

Esto confirma que la operación bancaria continúa funcionando correctamente con la arquitectura de Semana 7.

![Retiro ATM](evidencias/Evidencias%20S7/09_ATM_Retiro_APROBADO.png)

---

## Evidencia 10 - Evento RETIRO_REALIZADO publicado en Kafka

Se consume el tópico `banco.movimientos` y se observa el evento correspondiente al retiro anterior.

Datos principales:

```text
Partition: 2
Offset: 1
Key: 101
```

Payload:

```json
{
  "tipoEvento": "RETIRO_REALIZADO",
  "movimientoId": 14,
  "cuentaId": 101,
  "tipoMovimiento": "retiro",
  "monto": -100.00,
  "saldoResultante": 4750.00
}
```

Esto demuestra el flujo:

```text
BFF ATM
   ->
Bank Core API
   ->
MySQL
   ->
AFTER_COMMIT
   ->
Kafka
```

![Evento Kafka](evidencias/Evidencias%20S7/10_Kafka_Evento_RETIRO_REALIZADO.png)

---

## Evidencia 11 - Audit Service procesa el evento

Se inicia una instancia de:

```text
audit-service
```

con:

```text
AUDIT_INSTANCE=auditoria-1
```

El consumidor se suscribe al tópico:

```text
banco.movimientos
```

dentro del grupo:

```text
banco-auditoria
```

y procesa correctamente el evento:

```text
AUDITORIA [auditoria-1] evento procesado:
key=101
particion=2
offset=1
```

El payload corresponde a:

```text
RETIRO_REALIZADO
saldoResultante=4750.00
```

Esto demuestra comunicación asíncrona funcional entre el productor y el consumidor.

![Audit Service](evidencias/Evidencias%20S7/11_Audit_Service_Consume_Evento.png)

---

## Evidencia 12 - Escalabilidad horizontal con dos consumidores

Se ejecutan dos instancias de `audit-service` dentro del mismo grupo:

```text
banco-auditoria
```

La descripción del grupo muestra dos `CONSUMER-ID` distintos.

La distribución observada es:

```text
Consumidor 1 -> particiones 0 y 1
Consumidor 2 -> partición 2
```

Además, las particiones con eventos procesados presentan:

```text
LAG = 0
```

Esto demuestra que Kafka realiza el rebalanceo y distribuye las particiones entre múltiples consumidores del mismo grupo.

![Escalabilidad Kafka](evidencias/Evidencias%20S7/12_Kafka_Escalabilidad_2_Consumidores.png)

---

## Evidencia 13 - Compilación final de los ocho módulos

Se ejecuta desde la raíz:

```powershell
.\mvnw.cmd verify
```

El Reactor Summary muestra:

```text
Banco XYZ - Exp3 Semana 7 ........ SUCCESS
Banco XYZ - Config Server ........ SUCCESS
Banco XYZ - Discovery Server ..... SUCCESS
Banco XYZ - Core API ............. SUCCESS
Banco XYZ - BFF Web .............. SUCCESS
Banco XYZ - BFF Mobile ........... SUCCESS
Banco XYZ - BFF ATM .............. SUCCESS
Banco XYZ - Audit Service ........ SUCCESS
```

y finaliza con:

```text
BUILD SUCCESS
```

Esta evidencia demuestra compilación y empaquetado integrado exitoso de los módulos Maven del proyecto.

![Build Success](evidencias/Evidencias%20S7/13_BUILD_SUCCESS_8_Modulos.png)

---

# Resumen de evidencias

Las pruebas de Semana 7 permiten comprobar:

- funcionamiento de Spring Cloud Config;
- configuración centralizada de `bank-core-api`;
- registro del Core API en Eureka;
- operación normal del BFF ATM con el Core disponible;
- respuesta controlada cuando el Core no está disponible;
- transición del Circuit Breaker por los estados `CLOSED`, `OPEN` y `HALF_OPEN`;
- funcionamiento del tópico Kafka `banco.movimientos`;
- configuración de tres particiones;
- publicación de un evento real `RETIRO_REALIZADO`;
- procesamiento asíncrono mediante `audit-service`;
- distribución de particiones entre dos consumidores;
- escalabilidad horizontal mediante Consumer Groups;
- compilación integrada de los ocho módulos del proyecto.

---

# Relación con los criterios de evaluación

## 1. Arquitectura orientada a eventos

`bank-core-api` actúa como productor de eventos y `audit-service` como consumidor desacoplado.

El flujo implementado es:

```text
Operación bancaria
   ->
Persistencia MySQL
   ->
COMMIT
   ->
Evento
   ->
Kafka
   ->
Audit Service
```

## 2. Tópicos, mensajes y eventos

Tópico:

```text
banco.movimientos
```

Eventos implementados:

```text
DEPOSITO_REALIZADO
RETIRO_REALIZADO
COMPRA_REALIZADA
```

Clave Kafka:

```text
cuentaId
```

Particiones:

```text
3
```

## 3. Tolerancia a fallos con Resilience4j

El BFF ATM protege las llamadas al Core API mediante el Circuit Breaker:

```text
coreApi
```

Se demuestran los estados:

```text
CLOSED
OPEN
HALF_OPEN
```

además de la respuesta controlada:

```text
HTTP 503
CORE_NO_DISPONIBLE
```

## 4. Mensajería asíncrona y escalabilidad

Kafka procesa eventos de forma asíncrona mediante el grupo:

```text
banco-auditoria
```

La ejecución simultánea de dos consumidores permite comprobar el reparto de las tres particiones y el rebalanceo automático.

---

## Conclusión

Las evidencias demuestran que Banco XYZ evoluciona hacia una arquitectura de microservicios con configuración centralizada, descubrimiento de servicios, tolerancia a fallos y comunicación asíncrona.

Resilience4j permite gestionar de forma controlada la indisponibilidad del Core API, mientras que Kafka desacopla la publicación y el procesamiento posterior de los movimientos bancarios.

La integración entre `bank-core-api`, `banco.movimientos` y `audit-service`, junto con la ejecución de múltiples consumidores, demuestra una solución funcional y preparada para escalar horizontalmente.

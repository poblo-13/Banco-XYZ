# Evidencias de Ejecución
## Banco XYZ - Arquitectura de Microservicios, Resiliencia y Eventos
### Exp3 - Semana 7

Este documento reúne las evidencias de ejecución de la solución Banco XYZ desarrollada para la asignatura **Desarrollo Backend III (PBY2203)**.

La implementación conserva la arquitectura Backend for Frontend trabajada anteriormente e incorpora en Semana 7:

- configuración centralizada mediante Spring Cloud Config;
- descubrimiento de servicios con Eureka;
- tolerancia a fallos con Resilience4j;
- mensajería asíncrona con Apache Kafka;
- publicación de eventos bancarios;
- procesamiento mediante `audit-service`;
- escalabilidad horizontal con múltiples consumidores;
- compilación integrada de todos los módulos.

Las evidencias de Semana 5 se mantienen como respaldo de la base funcional y de seguridad sobre la que se construyó esta evolución.

---

# Evidencias heredadas de Semana 5

## Evidencia 1 - Estructura del proyecto BFF

Se observa la organización modular original del proyecto mediante un Core API y tres BFF independientes para Web, Mobile y ATM.

![Estructura del proyecto](evidencias/Evidencias%20S5/01_Estructura_Proyecto_BFF.png)

---

## Evidencia 2 - Ejecución del Core API

Se observa el inicio exitoso del servicio central del Banco XYZ en el puerto 8080, utilizando Spring Data JPA y MySQL.

![Core API](evidencias/Evidencias%20S5/02_Core_API_Puerto_8080.png)

---

## Evidencia 3 - BFF Web mediante HTTPS

Se observa el inicio exitoso del BFF Web en el puerto 8081 utilizando HTTPS.

![BFF Web HTTPS](evidencias/Evidencias%20S5/03_BFF_Web_HTTPS_8081.png)

---

## Evidencia 4 - BFF Mobile mediante HTTPS

Se observa el inicio exitoso del BFF Mobile en el puerto 8082 utilizando HTTPS.

![BFF Mobile HTTPS](evidencias/Evidencias%20S5/04_BFF_Mobile_HTTPS_8082.png)

---

## Evidencia 5 - BFF ATM mediante HTTPS

Se observa el inicio exitoso del BFF ATM en el puerto 8083 utilizando HTTPS.

![BFF ATM HTTPS](evidencias/Evidencias%20S5/05_BFF_ATM_HTTPS_8083.png)

---

## Evidencia 6 - Autenticación JWT del BFF Web

Se observa la generación correcta de un token Bearer para el canal Web con scope `WEB_ACCESS`.

![JWT Web](evidencias/Evidencias%20S5/06_JWT_Web_WEB_ACCESS.png)

---

## Evidencia 7 - Endpoint protegido del BFF Web

Se observa el acceso exitoso mediante JWT al endpoint `/api/web/cuentas`.

![Endpoint Web](evidencias/Evidencias%20S5/07_Web_Endpoint_Protegido_Cuentas.png)

---

## Evidencia 8 - Autenticación JWT del BFF Mobile

Se observa la generación correcta de un token Bearer para el canal Mobile con scope `MOBILE_ACCESS`.

![JWT Mobile](evidencias/Evidencias%20S5/08_JWT_Mobile_MOBILE_ACCESS.png)

---

## Evidencia 9 - Endpoint protegido del BFF Mobile

Se observa el acceso exitoso mediante JWT al endpoint `/api/mobile/cuentas/101`.

![Endpoint Mobile](evidencias/Evidencias%20S5/09_Mobile_Endpoint_Protegido_Cuenta.png)

---

## Evidencia 10 - Autenticación JWT del BFF ATM

Se observa la generación correcta de un token Bearer para el canal ATM con scope `ATM_ACCESS`.

![JWT ATM](evidencias/Evidencias%20S5/10_JWT_ATM_ATM_ACCESS.png)

---

## Evidencia 11 - Consulta de saldo desde BFF ATM

Se observa el acceso exitoso mediante JWT al endpoint protegido `/api/atm/saldo/101`.

![Consulta saldo ATM](evidencias/Evidencias%20S5/11_ATM_Consulta_Saldo_Protegida.png)

---

## Evidencia 12 - Retiro exitoso mediante BFF ATM

Se observa la ejecución exitosa de un retiro mediante el endpoint `/api/atm/retiros`.

![Retiro ATM](evidencias/Evidencias%20S5/12_ATM_Retiro_Exitoso.png)

---

## Evidencia 13 - Manejo de saldo insuficiente

Se observa el rechazo controlado de una operación mediante `HTTP 409 Conflict` y el código `SALDO_INSUFICIENTE`.

![Saldo insuficiente](evidencias/Evidencias%20S5/13_ATM_Error_Saldo_Insuficiente_409.png)

---

## Evidencia 14 - Acceso sin token JWT

Se observa que una solicitud sin credenciales a un endpoint protegido es rechazada mediante `HTTP 401 Unauthorized`.

![Sin token](evidencias/Evidencias%20S5/14_Seguridad_Sin_Token_401.png)

---

## Evidencia 15 - Token Web rechazado por BFF ATM

Se demuestra que un token generado para Web no puede utilizarse en ATM.

![Token Web rechazado](evidencias/Evidencias%20S5/15_Seguridad_Token_Web_Rechazado_ATM_401.png)

---

## Evidencia 16 - Token Mobile rechazado por BFF ATM

Se demuestra que un token generado para Mobile no puede utilizarse en ATM.

![Token Mobile rechazado](evidencias/Evidencias%20S5/16_Seguridad_Token_Mobile_Rechazado_ATM_401.png)

---

## Evidencia 17 - Observabilidad mediante Spring Boot Actuator

Se observa el endpoint `/actuator/health` respondiendo con estado `UP`.

![Actuator Health](evidencias/Evidencias%20S5/17_Actuator_Health_BFF_Web.png)

---

## Evidencia 18 - Compilación general de Semana 5

Se observa la compilación exitosa de los módulos originales mediante Maven Reactor.

![Build Success S5](evidencias/Evidencias%20S5/18_Compilacion_General_BUILD_SUCCESS.png)

---

## Evidencia 19 - Persistencia en MySQL

Se observa la persistencia de una operación ATM en la base de datos `banco_xyz_core`.

![Persistencia MySQL](evidencias/Evidencias%20S5/19_Base_Datos_Persistencia_Retiro_ATM.png)

---

# Evidencias Semana 7

> Las siguientes capturas deben guardarse en `evidencias/Evidencias S7/` utilizando los nombres indicados para mantener consistencia con este documento.

## Evidencia 20 - Config Server operativo

Se verifica que `config-server` está disponible en el puerto `8888` y entrega la configuración de `bank-core-api`.

URL utilizada:

```text
http://localhost:8888/bank-core-api/default
```

La respuesta debe mostrar propiedades centralizadas como:

- `eureka.client.serviceUrl.defaultZone`;
- `spring.kafka.bootstrap-servers`;
- serializadores Kafka;
- `banco.kafka.topic.movimientos`.

Captura sugerida:

```text
evidencias/Evidencias S7/20_Config_Server_Bank_Core_API.png
```

---

## Evidencia 21 - Registro del Core API en Eureka

Se verifica el funcionamiento de Eureka en el puerto `8761`.

El servicio:

```text
BANK-CORE-API
```

queda registrado con estado:

```text
UP
```

También se validó desde consola el registro exitoso con código `204`.

Captura sugerida:

```text
evidencias/Evidencias S7/21_Eureka_BANK_CORE_API_UP.png
```

---

## Evidencia 22 - Resilience4j con Core disponible

Con el Core API disponible, la consulta:

```text
GET https://localhost:8083/api/atm/saldo/101
```

responde correctamente:

```json
{
  "cuentaId": 101,
  "saldoDisponible": 4950.00
}
```

Esto demuestra el funcionamiento normal del BFF ATM cuando la dependencia está disponible.

Captura sugerida:

```text
evidencias/Evidencias S7/22_Resilience_Core_Disponible_200.png
```

---

## Evidencia 23 - Resilience4j con Core no disponible

Se detiene temporalmente el Core API y se verifica que el BFF ATM permanece operativo.

La solicitud de saldo responde de forma controlada con:

```text
HTTP 503 Service Unavailable
CORE_NO_DISPONIBLE
```

Esto evidencia tolerancia a fallos y manejo controlado de indisponibilidad.

Captura sugerida:

```text
evidencias/Evidencias S7/23_Resilience_Core_No_Disponible_503.png
```

---

## Evidencia 24 - Circuit Breaker en estado OPEN

Después de superar el umbral configurado de fallas, se consulta la métrica:

```text
resilience4j.circuitbreaker.state
```

y se verifica el estado:

```text
OPEN = 1.0
```

Esto confirma la apertura del Circuit Breaker.

Captura sugerida:

```text
evidencias/Evidencias S7/24_Circuit_Breaker_OPEN.png
```

---

## Evidencia 25 - Circuit Breaker en estado HALF_OPEN

Luego del tiempo de espera configurado, Resilience4j transiciona automáticamente a:

```text
HALF_OPEN
```

La métrica correspondiente devuelve:

```text
HALF_OPEN = 1.0
```

Captura sugerida:

```text
evidencias/Evidencias S7/25_Circuit_Breaker_HALF_OPEN.png
```

---

## Evidencia 26 - Recuperación del Circuit Breaker

Se reinicia `bank-core-api` y se realiza nuevamente la consulta de saldo.

La respuesta vuelve a ser exitosa:

```text
HTTP 200
```

y la métrica del Circuit Breaker confirma:

```text
CLOSED = 1.0
```

Esto demuestra el ciclo completo:

```text
CLOSED -> OPEN -> HALF_OPEN -> CLOSED
```

Captura sugerida:

```text
evidencias/Evidencias S7/26_Circuit_Breaker_Recuperado_CLOSED.png
```

---

## Evidencia 27 - Kafka operativo y tópico configurado

Kafka se ejecuta mediante Docker utilizando:

```text
docker-compose.kafka.yml
```

Se verifica el tópico:

```text
banco.movimientos
```

con:

```text
3 particiones
replication-factor = 1
```

Captura sugerida:

```text
evidencias/Evidencias S7/27_Kafka_Topic_3_Particiones.png
```

---

## Evidencia 28 - Retiro real desde BFF ATM

Se realiza un retiro de `100.00` sobre la cuenta `101`.

Saldo inicial:

```text
4950.00
```

Respuesta:

```json
{
  "cuentaId": 101,
  "montoRetirado": 100.00,
  "saldoDisponible": 4850.00,
  "estado": "APROBADO"
}
```

Esto comprueba que la operación bancaria continúa funcionando después de integrar Kafka.

Captura sugerida:

```text
evidencias/Evidencias S7/28_ATM_Retiro_100_APROBADO.png
```

---

## Evidencia 29 - Evento RETIRO_REALIZADO publicado en Kafka

Después del retiro se observa en el tópico `banco.movimientos` el evento real:

```text
Partition: 2
Offset: 0
Key: 101
```

Payload:

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

Esto demuestra el flujo:

```text
BFF ATM -> Core API -> MySQL -> AFTER_COMMIT -> Kafka
```

Captura sugerida:

```text
evidencias/Evidencias S7/29_Kafka_Evento_RETIRO_REALIZADO.png
```

---

## Evidencia 30 - Audit Service consume el evento

El nuevo módulo `audit-service` se conecta a Kafka mediante el grupo:

```text
banco-auditoria
```

y procesa correctamente el evento:

```text
AUDITORIA [auditoria-1] evento procesado:
key=101
particion=2
offset=0
```

La evidencia demuestra comunicación asíncrona funcional entre productor y consumidor.

Captura sugerida:

```text
evidencias/Evidencias S7/30_Audit_Service_Consume_Evento.png
```

---

## Evidencia 31 - Escalabilidad horizontal con dos consumidores

Se ejecutan dos instancias simultáneas de `audit-service` dentro del mismo grupo:

```text
banco-auditoria
```

Kafka realiza un rebalanceo automático.

Distribución observada:

```text
Consumidor 1 -> particiones 0 y 1
Consumidor 2 -> partición 2
```

El comando `kafka-consumer-groups.sh --describe` muestra dos `CONSUMER-ID` diferentes y las particiones repartidas entre ambos.

Esto demuestra escalabilidad horizontal mediante Consumer Groups.

Captura sugerida:

```text
evidencias/Evidencias S7/31_Kafka_Escalabilidad_2_Consumidores.png
```

---

## Evidencia 32 - Compilación final de todos los módulos

Se ejecuta desde la raíz:

```powershell
.\mvnw.cmd verify
```

El Reactor Summary final muestra:

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

Esto confirma que los ocho módulos Maven compilan correctamente de forma integrada.

Captura sugerida:

```text
evidencias/Evidencias S7/32_BUILD_SUCCESS_8_Modulos.png
```

---

# Resumen de evidencias Semana 7

Las pruebas realizadas permiten demostrar:

- funcionamiento de Config Server;
- configuración centralizada del Core API;
- registro del Core API en Eureka;
- tolerancia a fallos mediante Resilience4j;
- respuesta controlada ante indisponibilidad del Core;
- ciclo `CLOSED -> OPEN -> HALF_OPEN -> CLOSED`;
- funcionamiento de Apache Kafka;
- tópico `banco.movimientos` con tres particiones;
- publicación de eventos después del COMMIT;
- evento real `RETIRO_REALIZADO`;
- procesamiento asíncrono mediante `audit-service`;
- escalabilidad horizontal con dos consumidores;
- rebalanceo de particiones;
- compilación integrada de ocho módulos;
- continuidad de las medidas de seguridad JWT y HTTPS implementadas anteriormente.

---

## Relación con los criterios de evaluación

### Arquitectura orientada a eventos

Se implementa un flujo basado en eventos donde `bank-core-api` actúa como productor y `audit-service` como consumidor desacoplado.

### Diagrama, tópicos y mensajes

La arquitectura considera explícitamente:

```text
Topic: banco.movimientos
Eventos:
- DEPOSITO_REALIZADO
- RETIRO_REALIZADO
- COMPRA_REALIZADA
Key: cuentaId
Particiones: 3
```

### Tolerancia a fallos

Resilience4j protege la comunicación entre BFF ATM y Core API y se demuestra con fallas reales, apertura del Circuit Breaker y recuperación posterior.

### Mensajería asíncrona y escalabilidad

Kafka permite publicar y consumir eventos de forma asíncrona. La ejecución de dos consumidores dentro de `banco-auditoria` demuestra distribución de particiones y escalabilidad horizontal.

---

## Conclusión

Las evidencias de Semana 7 demuestran la evolución de Banco XYZ hacia una arquitectura de microservicios con configuración centralizada, descubrimiento de servicios, tolerancia a fallos y comunicación asíncrona.

Resilience4j permite mantener respuestas controladas frente a indisponibilidad del Core API, mientras que Kafka desacopla el procesamiento posterior de los movimientos bancarios.

La publicación de eventos posterior al COMMIT mantiene consistencia entre la operación persistida y el evento emitido. Finalmente, el uso de tres particiones y dos consumidores del mismo grupo permite comprobar una estrategia básica de escalabilidad horizontal.

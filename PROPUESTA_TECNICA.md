# Propuesta Técnica
## Banco XYZ - Arquitectura de Microservicios, OAuth2, Resiliencia y Contenedores
### Exp3 - Semana 8

## 1. Introducción

Durante esta semana se continúa trabajando sobre la arquitectura de Banco XYZ desarrollada anteriormente. La base del proyecto se mantiene: existe un Core API que concentra la lógica bancaria y tres BFF separados para los canales Web, Mobile y ATM.

El cambio principal de esta etapa es que la solución deja de depender solamente de una ejecución manual de cada servicio y pasa a incorporar seguridad centralizada con OAuth2 y un despliegue completo mediante Docker Compose.

También se mantienen los componentes trabajados en la semana anterior, como Spring Cloud Config, Eureka, Resilience4j, Kafka y el servicio de auditoría.

La idea es que todos estos componentes puedan funcionar juntos como una sola solución y que cada servicio conserve una responsabilidad clara.

---

## 2. Objetivo técnico

El objetivo de Semana 8 es dejar la arquitectura de Banco XYZ preparada para ejecutarse como un ecosistema de microservicios completo.

Para esto se busca:

- centralizar la emisión de tokens mediante OAuth2;
- mantener permisos separados para Web, Mobile y ATM;
- proteger los BFF como Resource Server;
- mantener un Core API único para la lógica bancaria;
- aplicar tolerancia a fallos en la comunicación entre los BFF y el Core;
- mantener la mensajería de movimientos mediante Kafka;
- ejecutar más de una instancia del consumidor de auditoría;
- dockerizar los microservicios;
- levantar todos los componentes mediante Docker Compose;
- evitar incluir claves o contraseñas directamente en el repositorio.

---

## 3. Arquitectura propuesta

La solución queda formada por los siguientes módulos:

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

Además se utilizan:

```text
MySQL
Apache Kafka
Docker
Docker Compose
```

El flujo general es el siguiente:

```text
                  Authorization Server
                         :9000
                           |
            +--------------+--------------+
            |              |              |
            v              v              v
         BFF Web       BFF Mobile      BFF ATM
          :8081          :8082          :8083
            \              |              /
             \             |             /
              +------- Resilience4j -----+
                           |
                           v
                    Bank Core API
                        :8080
                       /     \
                      /       \
                  MySQL       Kafka
                               |
                               v
                      banco.movimientos
                               |
                    +----------+----------+
                    |                     |
                    v                     v
             Audit Service 1       Audit Service 2
```

Como servicios de apoyo también se mantienen:

```text
Config Server -> 8888
Eureka        -> 8761
```

---

## 4. Backend for Frontend

La arquitectura mantiene tres BFF porque cada canal necesita exponer operaciones diferentes.

### BFF Web

El canal Web entrega una vista más completa de la información bancaria.

Entre sus operaciones se encuentran:

```text
GET /api/web/cuentas
GET /api/web/cuentas/{cuentaId}
GET /api/web/cuentas/{cuentaId}/movimientos
GET /api/web/dashboard/{cuentaId}
```

Su scope es:

```text
WEB_ACCESS
```

### BFF Mobile

El canal Mobile entrega información más resumida y enfocada en consultas rápidas.

Entre sus operaciones se encuentran:

```text
GET /api/mobile/cuentas/{cuentaId}
GET /api/mobile/cuentas/{cuentaId}/movimientos-recientes
GET /api/mobile/resumen/{cuentaId}
```

Su scope es:

```text
MOBILE_ACCESS
```

### BFF ATM

El canal ATM se mantiene limitado a las operaciones propias de un cajero automático.

```text
GET  /api/atm/saldo/{cuentaId}
POST /api/atm/retiros
```

Su scope es:

```text
ATM_ACCESS
```

La separación de los BFF evita entregar la misma interfaz a todos los clientes y permite controlar de forma independiente qué puede realizar cada canal.

Los BFF no acceden directamente a MySQL. Toda operación bancaria pasa por `bank-core-api`.

---

## 5. Authorization Server

En Semana 8 se incorpora el módulo:

```text
authorization-server
```

Su función es centralizar la emisión de tokens OAuth2.

El servicio funciona en:

```text
http://localhost:9000
```

Se configuraron tres clientes:

```text
banco-web
banco-mobile
banco-atm
```

Cada uno tiene acceso solamente al scope que corresponde a su canal:

```text
banco-web    -> WEB_ACCESS
banco-mobile -> MOBILE_ACCESS
banco-atm    -> ATM_ACCESS
```

El flujo utilizado es:

```text
client_credentials
```

Este flujo fue elegido porque en esta implementación se valida el acceso de los clientes o servicios al backend. No corresponde a un inicio de sesión de una persona.

El endpoint utilizado para solicitar tokens es:

```text
POST /oauth2/token
```

---

## 6. Seguridad de los BFF

Los tres BFF se configuran como OAuth2 Resource Server.

Esto significa que ya no generan sus propios JWT. En su lugar, reciben un token emitido por `authorization-server`, validan su firma y revisan los scopes incluidos.

Las reglas principales son:

```text
/api/web/**     -> SCOPE_WEB_ACCESS
/api/mobile/**  -> SCOPE_MOBILE_ACCESS
/api/atm/**     -> SCOPE_ATM_ACCESS
```

Durante las pruebas se comprobó el siguiente comportamiento:

```text
Sin token                 -> 401 Unauthorized
Token con scope correcto  -> acceso permitido
Token de otro canal       -> 403 Forbidden
```

Con este cambio se eliminó la generación local de JWT que existía anteriormente en cada BFF.

---

## 7. Manejo de secretos

Las credenciales utilizadas por OAuth2 y MySQL no se dejan escritas directamente en el código ni se suben a Git.

El proyecto utiliza un archivo local:

```text
.env
```

Este archivo está excluido mediante `.gitignore`.

Como referencia se incluye:

```text
.env.example
```

Las variables principales son:

```text
MYSQL_ROOT_PASSWORD
OAUTH_WEB_CLIENT_SECRET
OAUTH_MOBILE_CLIENT_SECRET
OAUTH_ATM_CLIENT_SECRET
```

También se verificó antes de preparar la entrega que `.env` y los certificados PKCS12 no estuvieran siendo rastreados por Git.

---

## 8. Core API

El módulo `bank-core-api` continúa siendo el servicio central de negocio.

Entre sus responsabilidades se encuentran:

- consultar cuentas;
- consultar movimientos;
- registrar depósitos;
- registrar retiros;
- registrar compras;
- validar saldo disponible;
- actualizar saldos;
- persistir operaciones;
- publicar eventos de movimientos.

La persistencia se realiza en MySQL mediante Spring Data JPA.

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

## 9. Configuración centralizada

Se mantiene `config-server` mediante Spring Cloud Config.

Puerto:

```text
8888
```

El Core API obtiene parte de su configuración desde:

```text
config-repo/bank-core-api.properties
```

Desde este repositorio se definen propiedades relacionadas con Eureka, Kafka y observabilidad.

En Docker, el repositorio de configuración se monta dentro del contenedor para que `config-server` pueda leerlo sin depender de una ruta local de Windows.

---

## 10. Service Discovery

El proyecto mantiene `discovery-server` utilizando Netflix Eureka.

Puerto:

```text
8761
```

El `bank-core-api` se registra como:

```text
BANK-CORE-API
```

Durante la prueba final se comprobó un registro exitoso:

```text
registration status: 204
```

También se validó el estado `UP` desde Eureka.

---

## 11. Resilience4j

En la versión anterior la principal prueba de tolerancia a fallos estaba concentrada en ATM. Para Semana 8 la protección mediante Resilience4j se extiende a los tres BFF.

El Circuit Breaker utilizado se llama:

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

Cuando el Core no se encuentra disponible, los BFF responden de manera controlada en vez de dejar que la excepción de comunicación llegue directamente al cliente.

Respuesta esperada:

```text
HTTP 503 Service Unavailable
CORE_NO_DISPONIBLE
```

Durante las pruebas se verificó el ciclo:

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

También se mantienen fuera del conteo del Circuit Breaker algunos errores propios del negocio, por ejemplo una cuenta inexistente o saldo insuficiente. Esos casos no representan una caída del Core.

---

## 12. Kafka y arquitectura orientada a eventos

Apache Kafka se mantiene como broker para los eventos generados después de una operación bancaria.

Tópico:

```text
banco.movimientos
```

Configuración:

```text
3 particiones
replication-factor = 1
```

Los eventos utilizados son:

```text
DEPOSITO_REALIZADO
RETIRO_REALIZADO
COMPRA_REALIZADA
```

El contrato `MovimientoEvento` contiene información como:

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

De esta forma, los eventos de una misma cuenta son enviados a una misma partición y mantienen su orden relativo.

---

## 13. Publicación después del COMMIT

Los eventos no se envían a Kafka antes de confirmar la operación bancaria.

El Core utiliza:

```text
@TransactionalEventListener(
    phase = TransactionPhase.AFTER_COMMIT
)
```

El flujo es:

```text
Operación bancaria
      |
      v
Validación
      |
      v
Persistencia en MySQL
      |
      v
COMMIT
      |
      v
Evento interno
      |
      v
Kafka
```

Esto evita publicar un movimiento que finalmente no haya quedado guardado en la base de datos.

---

## 14. Audit Service

El módulo `audit-service` consume los eventos generados por el Core.

Tópico:

```text
banco.movimientos
```

Grupo:

```text
banco-auditoria
```

Para Semana 8 se levantan dos instancias:

```text
audit-service-1
audit-service-2
```

Ambas pertenecen al mismo Consumer Group.

Durante las pruebas Kafka repartió las particiones entre las dos instancias, por lo que no ambas procesan el mismo evento dentro del mismo grupo.

Esto permite demostrar escalabilidad horizontal del consumidor.

---

## 15. Prueba funcional del flujo de eventos

Se realizó un retiro mediante el BFF ATM utilizando la cuenta `101`.

Datos de la prueba:

```text
Saldo inicial:     100000.00
Monto retirado:     10000.00
Saldo resultante:   90000.00
Estado:             APROBADO
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

Después de la operación se comprobó el saldo desde el Core y se observó el evento `RETIRO_REALIZADO` en Kafka.

El evento fue consumido por una de las instancias de `audit-service`.

---

## 16. Dockerización

Todos los microservicios principales cuentan con un `Dockerfile`.

Se generaron imágenes para:

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

Los Dockerfile utilizan una construcción multi-stage. La primera etapa utiliza Maven para compilar el módulo correspondiente y la segunda etapa utiliza una imagen Java para ejecutar el JAR generado.

---

## 17. Docker Compose

El archivo principal de despliegue es:

```text
docker-compose.yml
```

Este archivo levanta:

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

En total se ejecutan once contenedores dentro del mismo ecosistema.

Los servicios se comunican utilizando los nombres definidos por Docker Compose. De esta forma no dependen de `localhost` para comunicarse entre contenedores.

---

## 18. Orden de arranque y healthchecks

Durante las primeras pruebas se detectó que `bank-core-api` podía intentar conectarse a MySQL antes de que la base estuviera lista.

Para corregir esto se agregaron `healthcheck` a:

```text
mysql
kafka
```

El Core espera que ambos servicios estén saludables antes de iniciar.

En la prueba final se obtuvo:

```text
mysql -> healthy
kafka -> healthy
```

y luego el Core inició correctamente.

En los logs se comprobó:

```text
HikariPool-1 - Start completed.
Started BankCoreApiApplication
registration status: 204
```

Después de aplicar este cambio no volvió a aparecer el error de detección de dialecto que se había producido durante el primer arranque.

---

## 19. HTTPS

Para la ejecución local se mantiene la posibilidad de utilizar HTTPS en los BFF mediante un certificado PKCS12.

El certificado local se encuentra fuera de Git:

```text
certs/banco-xyz-local.p12
```

La configuración utiliza variables como:

```text
SSL_ENABLED
SSL_KEYSTORE_PATH
SSL_KEYSTORE_PASSWORD
```

En Docker Compose los BFF utilizan HTTP dentro de la red interna:

```text
SSL_ENABLED=false
```

La autorización de los endpoints continúa protegida mediante OAuth2 y JWT.

---

## 20. Manejo de errores

La solución mantiene respuestas controladas para errores funcionales y de infraestructura.

Entre los códigos utilizados se encuentran:

```text
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
503 Service Unavailable
```

Ejemplos de errores propios del sistema:

```text
SALDO_INSUFICIENTE
CORE_NO_DISPONIBLE
```

Como mejora futura todavía se puede avanzar hacia una estructura de error común para todos los módulos.

---

## 21. Compilación y validación final

Se ejecutó desde la raíz:

```powershell
mvn clean verify
```

El resultado fue:

```text
Banco XYZ - Exp3 Semana 8 ................. SUCCESS
Banco XYZ - Config Server ................. SUCCESS
Banco XYZ - Discovery Server .............. SUCCESS
Banco XYZ - Core API ...................... SUCCESS
Banco XYZ - BFF Web ....................... SUCCESS
Banco XYZ - BFF Mobile .................... SUCCESS
Banco XYZ - BFF ATM ....................... SUCCESS
Banco XYZ - Audit Service ................. SUCCESS
Banco XYZ - Authorization Server .......... SUCCESS

BUILD SUCCESS
```

Esto valida la compilación integrada de los nueve elementos del reactor Maven.

También se comprobó el funcionamiento del ecosistema con:

```powershell
docker compose ps
```

MySQL y Kafka aparecieron en estado `healthy` y el resto de los servicios permaneció en ejecución.

---

## 22. Decisiones técnicas

### OAuth2 centralizado

Se prefirió un Authorization Server independiente en vez de mantener generación de JWT en cada BFF. Esto deja la emisión de tokens en un único lugar y permite que cada BFF se concentre en validar el token y aplicar sus permisos.

### Scopes separados

Se mantienen:

```text
WEB_ACCESS
MOBILE_ACCESS
ATM_ACCESS
```

porque los tres canales no ofrecen las mismas operaciones.

### Resilience4j en los tres BFF

La dependencia hacia el Core existe en los tres canales. Por este motivo se decidió aplicar tolerancia a fallos en Web, Mobile y ATM.

### Kafka

Kafka se mantiene porque permite desacoplar el procesamiento de eventos y demostrar distribución mediante particiones y Consumer Groups.

### `cuentaId` como key

La cuenta se utiliza como clave para conservar el orden relativo de sus movimientos dentro de una partición.

### AFTER_COMMIT

Se mantiene esta estrategia para no publicar un evento antes de confirmar la operación en MySQL.

### Docker Compose

Se utiliza Docker Compose para evitar iniciar manualmente cada componente y para dejar definida en un solo archivo la infraestructura necesaria para ejecutar el proyecto.

---

## 23. Limitaciones actuales

La implementación corresponde a un entorno académico y local.

Kafka utiliza:

```text
1 broker
replication-factor = 1
```

por lo que no representa una configuración de alta disponibilidad.

El flujo OAuth2 utilizado es `client_credentials`. Esto permite demostrar autenticación entre clientes y servicios, pero no representa autenticación de usuarios bancarios finales.

El `audit-service` procesa los eventos y los registra en logs. Actualmente no mantiene una base de datos de auditoría propia.

MySQL y Kafka utilizan volúmenes Docker para conservar datos entre reinicios, pero al ejecutar:

```powershell
docker compose down -v
```

estos volúmenes se eliminan.

---

## 24. Mejoras futuras

A partir de esta versión se podrían incorporar las siguientes mejoras:

- agregar autenticación de usuarios finales además de `client_credentials`;
- propagar identidad hacia el Core para mejorar la trazabilidad;
- estandarizar completamente los DTO de error;
- incorporar Bean Validation en todos los contratos de entrada;
- agregar idempotencia en los consumidores Kafka;
- implementar un Dead Letter Topic para eventos que no puedan procesarse;
- persistir las auditorías en una base de datos propia;
- agregar métricas y dashboards de observabilidad;
- desplegar Kafka con más de un broker para alta disponibilidad;
- utilizar un API Gateway o proxy reverso para centralizar HTTPS en un ambiente productivo.

---

## 25. Tecnologías utilizadas

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

Durante las pruebas finales se utilizó Java 22.0.1 y Maven 3.9.9, manteniendo el proyecto compilado con `release 21`.

---

## 26. Conclusión

La versión de Semana 8 reúne en una misma solución los componentes trabajados durante las semanas anteriores y agrega dos cambios importantes: la seguridad OAuth2 centralizada y el despliegue completo con Docker Compose.

Los tres BFF mantienen funciones distintas para Web, Mobile y ATM, mientras que el Core continúa concentrando las reglas bancarias y el acceso a MySQL.

Resilience4j permite controlar fallas en la comunicación con el Core y Kafka mantiene desacoplado el procesamiento posterior de los movimientos. Las dos instancias de auditoría muestran que el consumo puede repartirse utilizando particiones y Consumer Groups.

Finalmente, Docker Compose permite iniciar el conjunto de servicios de forma ordenada y reproducible, incluyendo MySQL, Kafka, seguridad, configuración, descubrimiento, Core, BFF y auditoría.

# Banco XYZ — Desarrollo Backend III | Semana 9

**Asignatura:** Desarrollo Backend III (PBY2203)\
**Evaluación:** Experiencia 3, Semana 9\
**Proyecto:** Banco XYZ\
**Rama de entrega:** [`semana9-pablo`](https://github.com/poblo-13/Banco-XYZ/tree/semana9-pablo)\
**Referencia revisada:** commit `50953c8`

## 1. Descripción

Banco XYZ es un proyecto académico de backend bancario basado en **Java 21, Spring Boot y microservicios**. La versión de Semana 9 evoluciona la arquitectura de las semanas anteriores: separa las responsabilidades de **cuentas, clientes y pagos**, mantiene tres canales BFF (Web, Mobile y ATM), utiliza **OAuth2** para controlar el acceso, **Kafka** para distribuir eventos y **Spring Batch** para migrar datos históricos.

La solución también incorpora Spring Cloud Config, Eureka, Resilience4j, MySQL y Docker Compose. El módulo `bank-core-api` se conserva como implementación anterior y se incluye en el perfil Docker `legacy`; **no es el backend principal de los BFF en Semana 9**.

## 2. Arquitectura de Semana 9

```text
                       +--------------------------+
                       | Authorization Server     |
                       | OAuth2 / JWT (:9000)     |
                       +--------------------------+
                                    |
                        Tokens y scopes por canal
                                    |
              +---------------------+---------------------+
              |                     |                     |
         +----v----+           +----v-----+          +----v----+
         | BFF Web |           | BFF      |          | BFF ATM |
         | :8181   |           | Mobile   |          | :8083   |
         | (host)  |           | :8082    |          |         |
         +----+----+           +----+-----+          +----+----+
              |                     |                     |
              +---------------------+---------------------+
                                    |
                        Servicios especializados
              +---------------------+---------------------+
              |                     |                     |
         +----v-----+         +-----v------+         +----v-----+
         | Cuentas  |         | Clientes   |         | Pagos    |
         | :8084    |         | :8085      |         | :8086    |
         +----+-----+         +------------+         +----+-----+
              |                                          |
              +------------ MySQL (BD por dominio) ------+
                                                         |
                                                   Outbox / Kafka
                                                         |
                                                banco.movimientos
                                                         |
                            +----------------------------+----------------+
                            |                            |                |
                      +-----v-----+               +------v------+   +-----v-----+
                      | Auditoría |               | Notificación|   | Riesgo     |
                      +-----------+               +-------------+   +-----------+

             Infraestructura transversal: Config Server + Eureka
             Migración histórica independiente: Spring Batch
```

> Esquema lógico simplificado. Kafka permite a auditoría, notificaciones y riesgo procesar los mismos eventos mediante **grupos de consumidores independientes**. La configuración y las dependencias reales están en `docker-compose.yml`.

## 3. Módulos Maven

El `pom.xml` raíz declara **14 módulos**:

| Módulo | Responsabilidad |
|---|---|
| `authorization-server` | Emisión centralizada de tokens OAuth2 |
| `config-server` | Configuración externa centralizada |
| `discovery-server` | Descubrimiento y registro de servicios con Eureka |
| `bank-core-api` | Core de versiones previas, conservado para modo `legacy` |
| `account-service` | Gestión de cuentas y operaciones sobre cuentas |
| `customer-service` | Gestión de clientes |
| `payment-service` | Gestión de movimientos/pagos y publicación mediante outbox |
| `batch-migration` | Migración de archivos históricos mediante Spring Batch |
| `bff-web` | API específica para el canal Web |
| `bff-mobile` | API específica para el canal Mobile |
| `bff-atm` | API específica para cajeros automáticos |
| `audit-service` | Consumo y registro de eventos para auditoría |
| `notification-service` | Procesamiento de eventos para notificaciones |
| `risk-service` | Procesamiento de eventos y reglas de riesgo |

### Servicios accesibles localmente

| Componente | Dirección o puerto de host | Observación |
|---|---|---|
| OAuth2 Authorization Server | `http://localhost:9000` | Token: `/oauth2/token` |
| Config Server | `http://localhost:8888` | Configuración centralizada |
| Eureka | `http://localhost:8761` | Panel de descubrimiento |
| Account Service | `http://localhost:8084` | Cuentas |
| Customer Service | `http://localhost:8085` | Clientes |
| Payment Service | `http://localhost:8086` | Movimientos/pagos |
| BFF Web | `http://localhost:8181` | Mapeo Docker **8181:8081** |
| BFF Mobile | `http://localhost:8082` | Canal Mobile |
| BFF ATM | `http://localhost:8083` | Canal ATM |
| MySQL | `localhost:3307` | Contenedor en puerto 3306 |
| Kafka | `localhost:9092` | Internamente se usa `kafka:29092` |
| Bank Core API (legacy) | `http://localhost:8080` | Solo con perfil `legacy` |

Los consumidores de auditoría, notificaciones y riesgo funcionan en segundo plano: no requieren exponer un puerto HTTP al host. `batch-migration` forma parte de Maven, pero **no se levanta como servicio permanente** en el Compose principal.

## 4. Tecnologías

- **Lenguaje y build:** Java 21, Maven y Maven Wrapper.
- **Framework:** Spring Boot 4.0.7 y Spring Cloud 2025.1.2, según el `pom.xml` raíz revisado.
- **Servicios:** Spring Web, Spring Data JPA, Spring Security y Spring Cloud.
- **Seguridad:** OAuth2, JWT y Authorization Server.
- **Infraestructura:** Spring Cloud Config, Eureka, Resilience4j.
- **Persistencia:** MySQL 8; H2 se utiliza en pruebas aisladas de Spring Batch.
- **Mensajería:** Apache Kafka 4.3.1 y Spring Kafka.
- **Migración:** Spring Batch.
- **Contenedores:** Docker y Docker Compose.

## 5. Requisitos previos

- JDK 21 compatible con el proyecto.
- Docker Desktop iniciado, con Docker Compose disponible.
- Maven o el Maven Wrapper incluido (`mvnw.cmd` en Windows).
- Git y PowerShell (para los ejemplos de esta guía).
- Puertos de la tabla anterior disponibles en el equipo.

## 6. Preparación del entorno

Clonar la rama de Semana 9:

```powershell
git clone --branch semana9-pablo --single-branch https://github.com/poblo-13/Banco-XYZ.git
cd Banco-XYZ
```

Preparar las variables de entorno:

```powershell
Copy-Item .env.example .env
notepad .env
```

Completar con valores **locales** las variables que solicita `.env.example`, incluyendo la contraseña de MySQL y los secretos de los clientes OAuth2. **No subir `.env` al repositorio.** La configuración de Compose de Semana 9 utiliza, entre otras, estas variables:

```text
MYSQL_ROOT_PASSWORD
OAUTH_WEB_CLIENT_SECRET
OAUTH_MOBILE_CLIENT_SECRET
OAUTH_ATM_CLIENT_SECRET
OAUTH_BACKOFFICE_CLIENT_SECRET
OAUTH_PAYMENT_CLIENT_SECRET
OAUTH_OPERADOR_CLIENT_SECRET
OAUTH_ATM_INTERNAL_CLIENT_SECRET
```

> No colocar secretos reales en capturas, informes, commits ni salidas compartidas.

## 7. Compilación y pruebas

Desde la raíz del repositorio:

```powershell
.\mvnw.cmd clean verify
```

Si Maven ya está instalado:

```powershell
mvn clean verify
```

Para ejecutar específicamente las pruebas del módulo de migración y sus dependencias Maven:

```powershell
mvn -pl batch-migration -am test
```

El último registro de trabajo de Semana 9 informó **17 pruebas exitosas de Spring Batch**, incluyendo tres pruebas de recuperación ante errores transitorios simulados; se ejecutaron con una base **H2 temporal**, sin afectar los datos de MySQL. Este resultado corresponde a una ejecución registrada, **no equivale a una compilación nueva realizada al redactar este documento**.

## 8. Inicio con Docker Compose

Construir y arrancar la arquitectura principal:

```powershell
docker compose up -d --build
```

Consultar los contenedores y los logs:

```powershell
docker compose ps
docker compose logs --tail 100 account-service customer-service payment-service
```

Consultar los consumidores de eventos:

```powershell
docker compose logs --tail 100 audit-service-1 audit-service-2 notification-service risk-service
```

Para detener los contenedores **sin borrar las bases de datos persistidas en volúmenes**:

```powershell
docker compose down
```

**Precaución:** `docker compose down -v` elimina también los volúmenes definidos por el proyecto, por lo que puede borrar datos locales de MySQL y Kafka. No ejecutarlo si se necesitan conservar evidencias o registros.

### Modo legacy

El `bank-core-api` de las semanas anteriores se mantiene bajo perfil `legacy`. Para activarlo explícitamente junto con el Compose:

```powershell
docker compose --profile legacy up -d --build
```

El flujo principal de Semana 9 utiliza `account-service`, `customer-service` y `payment-service`, no depende de activar el perfil `legacy`.

## 9. Seguridad OAuth2

El Authorization Server publica el endpoint:

```text
POST http://localhost:9000/oauth2/token
```

Se utiliza el flujo `client_credentials` para clientes/aplicaciones y comunicación de servicios: **no representa un inicio de sesión de una persona**. Los BFF validan los tokens como Resource Servers y diferencian los canales mediante scopes.

| Canal | Cliente OAuth2 | Scope |
|---|---|---|
| Web | `banco-web` | `WEB_ACCESS` |
| Mobile | `banco-mobile` | `MOBILE_ACCESS` |
| ATM | `banco-atm` | `ATM_ACCESS` |

Existen además credenciales internas para operaciones entre servicios según las variables usadas por Compose. Para comprobar las rutas y permisos actuales, la fuente de referencia son los `SecurityConfig.java` y controladores de cada módulo.

### Ejemplo: obtener token Web con PowerShell

Tras cargar los secretos en la sesión (por ejemplo, desde la configuración local de desarrollo):

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

> PowerShell **no importa automáticamente** las variables de `.env` a `$env:`. Antes de probar la solicitud, asegurarse de que `OAUTH_WEB_CLIENT_SECRET` esté definida en la sesión y coincida con el valor del contenedor.

Consulta Web de ejemplo:

```powershell
Invoke-RestMethod `
    -Uri "http://127.0.0.1:8181/api/web/cuentas" `
    -Headers @{ Authorization = "Bearer $tokenWeb" }
```

La ruta Web que anteriormente se consultaba en el host por `8081` se publica en **`8181`** en el Compose de Semana 9.

## 10. Servicios de negocio y persistencia

La arquitectura desacopla tres dominios principales:

- **Cuentas:** `account-service`, respaldado por la base `banco_xyz_cuentas`.
- **Clientes:** `customer-service`, respaldado por `banco_xyz_clientes`.
- **Pagos/movimientos:** `payment-service`, respaldado por `banco_xyz_pagos`.

Cada servicio tiene código propio, entidades y repositorios. `payment-service` incluye `EventoMovimientoOutbox` y `OutboxPublisher`, componentes destinados a registrar y publicar eventos de movimiento, separando la operación de negocio de su difusión asíncrona. Para conocer las rutas HTTP exactas y validaciones de negocio, consultar los controladores Java dentro de cada servicio.

## 11. Eventos Kafka y escalabilidad

El tópico utilizado en las pruebas es:

```text
banco.movimientos
```

Según la evidencia incluida en el repositorio, se configuraron **3 particiones** y **3 grupos de consumidores independientes**:

| Servicio consumidor | Grupo |
|---|---|
| Auditoría | `banco-auditoria` |
| Notificaciones | `banco-notificaciones` |
| Riesgo | `banco-riesgo` |

El Compose contiene `audit-service-1` y `audit-service-2`. La prueba de escalamiento de Semana 9 utilizó **dos instancias por cada servicio consumidor**, para un total de seis instancias. Para reproducir esa configuración a partir del Compose actual, se pueden escalar los servicios de nombre único:

```powershell
docker compose up -d --scale notification-service=2 --scale risk-service=2
```

> Este comando crea dos réplicas de notificaciones y riesgo en la ejecución local. Conviene validar la distribución con `docker compose ps` y los logs correspondientes; el comportamiento depende de que las réplicas estén saludables y del reparto del grupo Kafka.

### Evidencia de Kafka incluida

El archivo [`docs/EVIDENCIA-KAFKA-SEMANA9.md`](docs/EVIDENCIA-KAFKA-SEMANA9.md) documenta una ejecución del **10 de octubre de 2026** en la que se publicaron **24 eventos simulados**:

| Dominio consumidor | Eventos persistidos |
|---|---:|
| Auditoría | 24 |
| Notificaciones | 24 |
| Riesgo | 24 |
| **Total de registros en los tres dominios** | **72** |

La prueba verificó que **ambas instancias de cada servicio participaron** en el consumo, utilizando grupos independientes y particiones distribuidas. También se documentó la deduplicación por identificador y una alerta de riesgo por un retiro simulado de 15.000.

**Alcance:** los 24 eventos fueron **simulados**; no modificaron saldos bancarios y la prueba no constituye un benchmark de carga masiva. La evidencia indica como pendientes la recuperación ante fallo de una réplica y el endurecimiento de seguridad del broker Kafka.

## 12. Spring Batch: migración histórica

`batch-migration` contiene tres configuraciones de jobs:

```text
MovimientosJobConfig.java
InteresesJobConfig.java
EstadosJobConfig.java
```

También incluye `ParallelBatchFlows.java` para organizar el procesamiento paralelo y pruebas automatizadas en:

```text
batch-migration/src/test/java/com/bancoxyz/batch/
```

El registro de validación de Semana 9 reporta que **las tres migraciones procesaron 3.054 registros sin pérdidas**, y que posteriormente se comprobaron los reintentos de escritura ante un error transitorio simulado. Los casos se ejecutaron de manera aislada con H2.

| Job | Prueba de fallo transitorio | Resultado registrado |
|---|---|---|
| Movimientos | Un fallo simulado en escritura | Recuperación exitosa |
| Intereses | Un fallo simulado en escritura | Recuperación exitosa |
| Estados financieros | Un fallo simulado en escritura | Recuperación exitosa |

En la ejecución registrada del módulo se obtuvo: **17 pruebas, 0 fallidas, 0 errores**. No se deduce de ello tolerancia automática a una caída real de MySQL o Kafka en producción.

## 13. Configuración, descubrimiento y resiliencia

- **Spring Cloud Config:** servicio `config-server`, con configuración externa en `config-repo/`.
- **Eureka:** servicio `discovery-server`, con interfaz de consulta en `http://localhost:8761`.
- **Resilience4j:** utilizado en los BFF para gestionar fallos de comunicación y ofrecer respuestas controladas ante servicios no disponibles.

El código y la configuración del repositorio son la referencia para parámetros concretos de circuit breakers, tiempos de espera, políticas de reintento y propiedades por entorno.

## 14. Estructura principal

```text
Banco-XYZ/
├── account-service/
├── customer-service/
├── payment-service/
├── bff-web/
├── bff-mobile/
├── bff-atm/
├── authorization-server/
├── config-server/
├── discovery-server/
├── audit-service/
├── notification-service/
├── risk-service/
├── batch-migration/
├── bank-core-api/             # Legacy
├── config-repo/
├── docs/
│   └── EVIDENCIA-KAFKA-SEMANA9.md
├── evidencias/                # Capturas y respaldos de etapas anteriores
├── docker-compose.yml
├── docker-compose.kafka.yml
├── .env.example
├── pom.xml
├── mvnw
├── mvnw.cmd
├── DIAGRAMA_ARQUITECTURA.md
├── EVIDENCIA_EJECUCION.md
├── PROPUESTA_TECNICA.md
└── README.md
```

## 15. Evidencias y documentación adicional

- [`docs/EVIDENCIA-KAFKA-SEMANA9.md`](docs/EVIDENCIA-KAFKA-SEMANA9.md): distribución de eventos y consumidores Kafka en Semana 9.
- [`DIAGRAMA_ARQUITECTURA.md`](DIAGRAMA_ARQUITECTURA.md): material de arquitectura de etapas anteriores; interpretar junto con este README actualizado.
- [`EVIDENCIA_EJECUCION.md`](EVIDENCIA_EJECUCION.md): registros de ejecución previos.
- [`PROPUESTA_TECNICA.md`](PROPUESTA_TECNICA.md): propuesta técnica original.
- [`evidencias/`](evidencias/): capturas organizadas por semanas anteriores.

## 16. Alcances y pendientes

**Implementaciones verificadas en la estructura del repositorio:** separación de microservicios, BFF por canal, Authorization Server, Config Server, Eureka, consumidores Kafka, jobs Spring Batch, Dockerfiles y Compose.

**Resultados informados en evidencias existentes:** prueba de 24 eventos por dominio consumidor, escalamiento de seis instancias de consumo, migración de 3.054 registros y 17 pruebas Batch exitosas con fallos transitorios simulados.

**No se afirma como validado en esta documentación:** despliegue productivo en AWS, pruebas de caída real de infraestructura, endurecimiento completo de Kafka ni pruebas de carga de producción. Esos escenarios requieren evidencias específicas adicionales.

## 17. Conclusión

La entrega de **Semana 9** demuestra una evolución de Banco XYZ desde un Core centralizado hacia una arquitectura con servicios especializados de **cuentas, clientes y pagos**, canales BFF independientes y procesamiento asíncrono de movimientos mediante Kafka. La incorporación de servicios consumidores para auditoría, notificaciones y riesgo permite separar tareas posteriores a cada movimiento. Spring Batch complementa la solución con la migración y validación de datos históricos.

La documentación distingue la **implementación presente en el código** de las **pruebas efectivamente registradas**, conservando el Core antiguo como opción `legacy` y facilitando la reproducción local mediante Maven, Docker Compose y variables de entorno.

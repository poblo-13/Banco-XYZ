# Propuesta Técnica — Banco XYZ

**Proyecto:** Arquitectura bancaria basada en microservicios y eventos\
**Asignatura:** Desarrollo Backend III (PBY2203)\
**Evaluación:** Experiencia 3 — Semana 9\
**Rama:** [`semana9-pablo`](https://github.com/poblo-13/Banco-XYZ/tree/semana9-pablo)\
**Versión de referencia:** commit `50953c8`, 10 de octubre de 2026

## 1. Introducción

Banco XYZ evoluciona una arquitectura anterior basada en un núcleo bancario común hacia servicios con responsabilidades independientes. En **Semana 9**, los dominios principales quedan separados en **Cuentas, Clientes y Pagos**. La solución conserva tres Backend for Frontend (BFF) especializados, un servidor OAuth2, configuración centralizada y descubrimiento de servicios. Agrega tres destinos independientes de eventos Kafka y consolida el tratamiento de archivos heredados con Spring Batch.

El objetivo es demostrar una arquitectura más mantenible y trazable, con controles de seguridad, manejo de duplicados y recuperación frente a ciertos errores transitorios, dentro de un entorno académico reproducible.

## 2. Problema y necesidades del negocio

Se identifican tres necesidades que guían las decisiones implementadas:

**Necesidad 1 — Separar responsabilidades y canales.** La concentración de cuentas, información de clientes y movimientos en un único servicio dificulta su evolución independiente. Los consumidores Web, Mobile y ATM también requieren interfaces y permisos distintos.

**Necesidad 2 — Procesar operaciones y eventos sin acoplar todos los sistemas.** Después de una operación bancaria, distintos procesos —auditar, notificar y detectar riesgos— necesitan la información sin bloquear innecesariamente la respuesta transaccional y sin guardar duplicados.

**Necesidad 3 — Incorporar datos legados con validación y tolerancia a fallos.** Los archivos históricos deben procesarse en lotes, detectar datos inválidos, mantener trazabilidad y recuperarse ante errores de acceso transitorios sin perder registros.

## 3. Objetivos técnicos

- Establecer servicios de dominio independientes para **Cuentas**, **Clientes** y **Pagos**.
- Conservar interfaces BFF específicas para Web, Mobile y ATM, protegidas mediante scopes OAuth2.
- Centralizar información de configuración y registro de servicios mediante Config Server y Eureka.
- Aislar fallos de comunicación mediante mecanismos de resiliencia en los BFF.
- Publicar movimientos confirmados utilizando **Transactional Outbox**.
- Distribuir eventos Kafka a **Auditoría**, **Notificaciones** y **Riesgo** mediante grupos consumidores independientes.
- Persistir el procesamiento con controles de duplicados e identificar movimientos de riesgo mediante reglas simples.
- Ejecutar tres migraciones Spring Batch, con validación, paralelismo y reintentos.
- Documentar resultados, limitaciones y procedimientos de ejecución local con Docker Compose.

## 4. Arquitectura por capas

| Capa | Componentes | Función |
|---|---|---|
| Entrada | `bff-web`, `bff-mobile`, `bff-atm` | APIs adaptadas a cada cliente |
| Identidad técnica | `authorization-server` | Tokens OAuth2 con scopes y flujo `client_credentials` |
| Negocio | `account-service`, `customer-service`, `payment-service` | Cuentas, clientes, movimientos |
| Plataforma | `config-server`, `discovery-server` | Configuración y registro |
| Eventos | Kafka (`banco.movimientos`) | Comunicación asíncrona |
| Procesamiento | `audit-service`, `notification-service`, `risk-service` | Tres usos independientes de los eventos |
| Migraciones | `batch-migration` | Ingesta y validación de archivos CSV |
| Persistencia | MySQL 8 | Bases de datos lógicas separadas |
| Operación | Docker Compose, Maven | Arranque local y compilación |

El archivo Maven raíz reúne **14 módulos**, incluyendo `bank-core-api` como **servicio legado**. Este último está bajo el perfil Docker Compose `legacy` y no representa el flujo principal por defecto.

El detalle gráfico está en [`DIAGRAMA_ARQUITECTURA.md`](DIAGRAMA_ARQUITECTURA.md).

## 5. Separación de los dominios bancarios

### 5.1 Cuentas (`account-service`)

Administración de cuentas, consulta de saldos y aplicación de operaciones por identificador. Expone el puerto `8084` y utiliza `banco_xyz_cuentas`. La idempotencia de operaciones ayuda a controlar reintentos y respuestas repetidas.

### 5.2 Clientes (`customer-service`)

Gestión y consulta de datos de clientes. Expone el puerto `8085` y utiliza `banco_xyz_clientes`. La separación evita mezclar datos personales con la lógica de movimientos y saldos.

### 5.3 Pagos (`payment-service`)

Registra movimientos y mantiene sus estados operacionales. Expone el puerto `8086` y utiliza `banco_xyz_pagos`. Procesa operaciones de tipo `DEPOSITO` y `RETIRO` en la implementación nueva, y se coordina con `account-service` mediante llamadas internas autorizadas.

El servicio usa un **`operacionId` UUID** como identificador de operación. Cuando la confirmación en Cuentas no puede determinarse, la operación puede permanecer pendiente para evitar asumir que el saldo fue modificado o revertido sin evidencia.

## 6. Backend for Frontend

Se conservan tres interfaces especializadas:

| BFF | Puerto host | Operaciones principales | Scope |
|---|---:|---|---|
| Web | **8181** | Listar cuentas, detalle, movimientos y dashboard | `WEB_ACCESS` |
| Mobile | 8082 | Cuenta, movimientos recientes y resumen | `MOBILE_ACCESS` |
| ATM | 8083 | Consultar saldo y efectuar retiro | `ATM_ACCESS` |

Los BFF utilizan los servicios de dominio según la operación solicitada; el puerto Web `8181` corresponde al mapeo `8181:8081` de Docker Compose. Las consultas descendentes cuentan con respuestas de error controladas y mecanismos de resiliencia. La API del cliente no queda acoplada directamente a las tablas de MySQL.

## 7. Autorización OAuth2

`authorization-server` emite tokens mediante `client_credentials`. Cada BFF exige el scope correspondiente al canal. La versión Semana 9 incorpora además los siguientes clientes técnicos:

| Cliente | Scopes configurados | Propósito |
|---|---|---|
| `banco-web` | `WEB_ACCESS` | Acceso al BFF Web |
| `banco-mobile` | `MOBILE_ACCESS` | Acceso al BFF Mobile |
| `banco-atm` | `ATM_ACCESS` | Acceso al BFF ATM |
| `banco-backoffice` | `CLIENTES_WRITE` | Administración autorizada de clientes |
| `banco-payment` | `CUENTAS_OPERAR` | Operaciones internas sobre cuentas |
| `banco-operador` | `PAGOS_WRITE`, `PAGOS_READ` | Acceso técnico al servicio de pagos |
| `banco-atm-interno` | `PAGOS_WRITE` | Llamadas internas desde ATM |

Las contraseñas de clientes y de MySQL se configuran mediante variables de entorno, nunca mediante valores reales añadidos a Git. `client_credentials` **no equivale** a autenticación individual de clientes bancarios; ese aspecto requeriría un mecanismo de identidad adicional para un sistema productivo.

## 8. Manejo de movimientos: estados e idempotencia

`payment-service` registra o identifica una solicitud por `operacionId`, valida su contenido, solicita a `account-service` el efecto sobre el saldo y registra el resultado. Los estados principales en código son `PENDIENTE`, `CONFIRMADO` y `RECHAZADO`.

El mismo identificador con parámetros distintos se rechaza. La confirmación duplicada de una operación ya completada recupera su resultado, en lugar de aplicar una segunda vez la misma operación. Errores funcionales como cuenta inexistente o saldo insuficiente se distinguen de fallos de comunicación de infraestructura.

**Límite de diseño:** las bases de Cuentas y Pagos no comparten una única transacción ACID distribuida; el diseño depende de identificadores, estados y manejo de reintentos para tratar resultados inciertos.

## 9. Patrón Transactional Outbox

Durante la confirmación local, Pagos guarda el movimiento confirmado **y** una entrada pendiente en `eventos_movimiento_outbox` en la misma transacción de base de datos. Un proceso `@Scheduled` consulta lotes de hasta diez eventos pendientes, los envía a Kafka y los marca publicados después de recibir confirmación.

```text
payment-service
    -> MySQL Pagos: movimiento CONFIRMADO + Outbox PENDIENTE
    -> publicador programado: lee Outbox
    -> Kafka: tópico banco.movimientos
    -> MySQL Pagos: Outbox PUBLICADO
```

En el código, el envío usa **`operacionId` como key Kafka**. Los mensajes de Semana 9 declaran `evento = MOVIMIENTO_CONFIRMADO`. No deben confundirse con los eventos del `bank-core-api` anterior, que utilizaba otro contrato y otro mecanismo de publicación.

El patrón disminuye el riesgo de perder eventos después de confirmar una transacción local, aunque una entrega puede repetirse; por eso la idempotencia de los consumidores sigue siendo indispensable.

## 10. Auditoría, Notificaciones y Riesgo

Los tres servicios se suscriben al mismo tópico `banco.movimientos`, pero usan grupos consumidores diferentes:

| Servicio | Grupo Kafka | Base de datos | Función |
|---|---|---|---|
| `audit-service` | `banco-auditoria` | `banco_xyz_auditoria` | Registrar eventos y metadatos del consumo |
| `notification-service` | `banco-notificaciones` | `banco_xyz_notificaciones` | Persistir notificaciones |
| `risk-service` | `banco-riesgo` | `banco_xyz_riesgo` | Persistir evaluaciones de riesgo |

Los consumidores validan los eventos y utilizan restricciones e identificadores para no duplicar entradas persistidas. También contrastan el contenido para detectar la reutilización indebida de un ID con otro payload.

En Riesgo, la lógica implementada etiqueta como `ALERTA` un retiro con monto **mayor o igual a 10.000**; es una regla académica simple y no un motor de detección de fraude real.

### Prueba de escalamiento

La evidencia [`docs/EVIDENCIA-KAFKA-SEMANA9.md`](docs/EVIDENCIA-KAFKA-SEMANA9.md) consigna **24 eventos simulados**, procesados por los tres grupos: 24 registros de auditoría, 24 notificaciones y 24 de riesgo (**72 persistencias**). Durante la prueba se ejecutaron **dos instancias por consumidor** (seis instancias); los contadores individuales reflejan el reparto de particiones. El archivo Compose por defecto tiene **dos instancias de auditoría**, pero solo **una de notificaciones** y **una de riesgo**, que fueron escaladas para esa prueba.

Los resultados verifican distribución funcional, no rendimiento bajo carga ni recuperación automática de fallas de brokers.

## 11. Spring Batch para datos legados

`batch-migration` implementa tres jobs:

1. `movimientosJob` — movimientos financieros diarios.
2. `interesesJob` — intereses trimestrales.
3. `estadosJob` — estados financieros anuales.

Cada job lee archivos de tres carpetas (`semana_1`, `semana_2`, `semana_3`), procesa registros por chunks y utiliza flujos paralelos con límite de tres tareas simultáneas. Los errores de validación se registran con el valor original, estado `RECHAZADO` y motivo; la escritura por `(semana, linea)` permite repetir la ingesta sin duplicar esa clave.

El código configura reintentos para excepciones transitorias de acceso a datos. Las validaciones previas de ejecución registraron **3.054 registros procesados sin pérdidas** entre las tres migraciones.

## 12. Pruebas de recuperación controlada

La prueba `ReintentosBatchIntegrationTest.java` utiliza una **base H2 temporal** y un `DataSource` instrumentado que provoca un fallo transitorio en la primera escritura de cada job. Se ejecutan tres casos separados con archivos sintéticos; cada job debe recuperarse y almacenar tres filas correspondientes a las tres semanas.

La última ejecución informada finalizó con:

```text
17 pruebas ejecutadas
17 correctas
0 fallos / 0 errores
BUILD SUCCESS
```

Esto respalda la recuperación **ante errores simulados**, pero no valida una desconexión real de MySQL en producción.

## 13. Configuración y despliegue local

La solución está preparada para ejecución con **Java 21**, Maven, Docker y Docker Compose. Se utiliza un solo contenedor MySQL con varias bases de datos lógicas y un broker Kafka con tres particiones. Config Server centraliza propiedades de componentes configurados, y Eureka permite publicar servicios registrados.

Comandos de referencia desde la raíz del proyecto:

```powershell
# Crear el archivo de variables y asignar secretos locales propios
Copy-Item .env.example .env

# Validar el archivo de orquestación
# (evitar mostrar o compartir salidas que expongan secretos)
docker compose config --quiet

# Compilar y ejecutar pruebas
mvn clean verify

# Levantar el entorno habitual sin el Core legado
docker compose up -d --build

# Revisar servicios
docker compose ps
```

**Precaución:** los comandos son un procedimiento de reproducción, no una declaración de que se hayan vuelto a ejecutar durante la redacción. `docker compose down -v` elimina volúmenes persistentes y puede borrar datos de pruebas; no se recomienda usarlo como rutina.

## 14. Decisiones técnicas y justificación

| Decisión | Justificación | Limitación |
|---|---|---|
| Dividir Cuentas, Clientes y Pagos | Responsabilidades separadas y evolución independiente | Introduce comunicación y consistencia entre servicios |
| Mantener tres BFF | Contratos ajustados a Web, Mobile y ATM | Más servicios que configurar y mantener |
| OAuth2 por scopes | Permisos explícitos para clientes técnicos | No representa identidad de clientes finales |
| Transactional Outbox | Registrar eventos pendientes junto con la operación local | No ofrece entrega exactamente una vez extremo a extremo |
| Tres grupos Kafka | Auditar, notificar y evaluar riesgos sin depender unos de otros | Un solo broker no da alta disponibilidad |
| Idempotencia y hash de payload | Tolerar duplicados y detectar discrepancias | Requiere persistencia y reglas de negocio coherentes |
| Spring Batch y validación | Migrar datos heredados con trazabilidad | Las fuentes heredadas pueden requerir limpieza posterior |
| Pruebas H2 con error inyectado | Repetibilidad sin tocar MySQL real | No sustituyen pruebas reales de infraestructura |
| Docker Compose | Facilitar ejecución académica reproducible | No representa por sí mismo un despliegue productivo |

## 15. Resultados acreditados y pendientes

### Resultados documentados

- Arquitectura de **14 módulos Maven** presente en la rama Semana 9.
- Prueba Kafka con **24 eventos simulados por grupo** y **72 registros** en total.
- Participación de **seis instancias** en el ejercicio de consumo distribuido.
- Regla de alerta de riesgo demostrada para un retiro simulado de **15.000**.
- Migración de **3.054 registros** de las tres fuentes heredadas, según la validación anterior.
- **17 pruebas Spring Batch** exitosas, incluidas tres de recuperación ante fallo transitorio simulado.
- Cambios de test publicados en el commit `50953c8`.

### Pendientes o fuera del alcance probado

- Pruebas de resiliencia ante caída real de MySQL, Kafka o de una réplica.
- Ensayos de carga, throughput y latencia con métricas reproducibles.
- Kafka con múltiples brokers y configuración de transporte autenticado/cifrado.
- Autenticación de usuarios bancarios finales y gestión productiva de secretos.
- Despliegue efectivo en AWS: **no demostrado por la evidencia consultada**.
- Validación formal de un video o capturas específicas: depende de los requisitos de entrega de la asignatura.

## 16. Documentación complementaria

- [`README.md`](README.md): visión general e instrucciones iniciales.
- [`DIAGRAMA_ARQUITECTURA.md`](DIAGRAMA_ARQUITECTURA.md): vistas Mermaid y responsabilidades.
- [`EVIDENCIA_EJECUCION.md`](EVIDENCIA_EJECUCION.md): resultados y trazabilidad.
- [`docs/EVIDENCIA-KAFKA-SEMANA9.md`](docs/EVIDENCIA-KAFKA-SEMANA9.md): mediciones de Kafka en la prueba documentada.
- [`docker-compose.yml`](docker-compose.yml): servicios, puertos y redes.
- [`batch-migration/src/test/java/com/bancoxyz/batch/ReintentosBatchIntegrationTest.java`](batch-migration/src/test/java/com/bancoxyz/batch/ReintentosBatchIntegrationTest.java): prueba de recuperación controlada.

## 17. Conclusión

La propuesta implementada en Semana 9 mejora la separación de responsabilidades del proyecto Banco XYZ y permite verificar, mediante pruebas académicas, el flujo de operaciones entre servicios, la propagación independiente de eventos y la migración paralela de información heredada. La seguridad OAuth2, los identificadores idempotentes, el patrón Outbox y Spring Batch aportan controles concretos, sin ocultar las limitaciones propias de una infraestructura local de desarrollo.

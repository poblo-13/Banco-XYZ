# Banco XYZ - Procesos Batch

Proyecto desarrollado con **Spring Boot 4.0.7, Java 21, Spring Batch y MySQL** para modernizar tres procesos batch del Banco XYZ a partir de archivos CSV.

## Objetivo

Implementar una solución batch que permita leer, validar, transformar y persistir información bancaria, incorporando procesamiento paralelo, tolerancia a fallos y auditoría de registros rechazados.

Los Jobs implementados son:

- `transaccionesJob`: procesamiento de transacciones diarias.
- `interesesJob`: cálculo de intereses mensuales.
- `cuentasAnualesJob`: generación de estados de cuenta anuales.

## Características principales

- Lectura de archivos CSV con `FlatFileItemReader`.
- Procesamiento orientado a chunks.
- Procesamiento paralelo con `ThreadPoolTaskExecutor`.
- Lectura segura mediante `SynchronizedItemStreamReader`.
- Persistencia en MySQL mediante Spring Data JPA.
- `CustomSkipPolicy` para registros inválidos.
- `RetryPolicy` para errores transitorios.
- Registro de errores mediante SLF4J.
- Auditoría de rechazados en `output/registros_rechazados.csv`.
- Pool de conexiones configurado con HikariCP.

## Configuración de procesamiento

Los principales parámetros se encuentran externalizados en `application.properties`:

```properties
batch.chunk-size=5
batch.core-pool-size=2
batch.max-pool-size=2
batch.queue-capacity=10
batch.max-skips=10

batch.retry-limit=3
batch.retry-initial-interval=500
batch.retry-multiplier=2.0
batch.retry-max-interval=2000
```

También se configura HikariCP:

```properties
spring.datasource.hikari.maximum-pool-size=8
spring.datasource.hikari.minimum-idle=3
spring.datasource.hikari.connection-timeout=30000
```

## Comparación de rendimiento

Se probaron distintas combinaciones de hilos y tamaño de chunk utilizando `transaccionesJob`.

| Configuración | Step principal | Job completo |
|---|---:|---:|
| 1 hilo / chunk 5 | 174 ms | 649 ms |
| **2 hilos / chunk 5** | **144 ms** | **622 ms** |
| 3 hilos / chunk 5 | 148 ms | 631 ms |
| 2 hilos / chunk 2 | 170 ms | 645 ms |
| 2 hilos / chunk 1 | 209 ms | 693 ms |

La mejor configuración observada fue **2 hilos con chunks de 5 registros**.

Debido al tamaño reducido de los archivos utilizados en la actividad, los tiempos son referenciales. Sin embargo, las pruebas permiten comprobar que aumentar la cantidad de hilos o disminuir demasiado el tamaño del chunk no necesariamente mejora el rendimiento.

## Tolerancia a fallos

La aplicación diferencia entre errores de datos y errores transitorios.

La `CustomSkipPolicy` permite omitir registros que generan `IllegalArgumentException`, con un máximo configurable de 10 rechazos por Step.

Los errores transitorios derivados de `TransientDataAccessException` utilizan una `RetryPolicy` con hasta 3 reintentos y espera incremental.

Si el error persiste después de los reintentos, el Step falla en lugar de ignorar el problema.

Los registros rechazados son almacenados en:

`output/registros_rechazados.csv`

El archivo registra fecha, fase del procesamiento, elemento rechazado y motivo del error.

## Base de datos

Crear la base de datos MySQL:

```sql
CREATE DATABASE bank_batch_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

La contraseña no se almacena directamente en el proyecto.

Antes de ejecutar, configurar en PowerShell:

```powershell
$env:DB_PASSWORD="CONTRASEÑA_MYSQL"
```

## Ejecución

Transacciones diarias:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=transaccionesJob ejecucion=1"
```

Intereses mensuales:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=interesesJob ejecucion=1"
```

Estados de cuenta anuales:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=cuentasAnualesJob ejecucion=1"
```

Para volver a ejecutar un Job se debe utilizar un valor diferente en el parámetro `ejecucion`.

## Resultados validados

### Transacciones

- Registros válidos: **8**
- Débitos: **4**
- Créditos: **4**
- Monto total procesado: **$9.400**
- Registros rechazados: **2**

### Intereses

- Cuentas procesadas: **7**
- Cuentas de ahorro: **4**
- Cuentas de préstamo: **3**
- Interés total calculado: **$625**

### Cuentas anuales

- Movimientos incluidos: **8**
- Registro inválido rechazado: **1**
- Archivo generado: `output/reporte_anual_auditoria.csv`

## Pruebas

Para ejecutar las pruebas:

```powershell
.\mvnw.cmd test
```

Resultado validado:

```text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Las pruebas incluyen la carga del contexto de Spring Boot y la validación de la política de Retry para errores transitorios y no transitorios.

## Documentación

La justificación de la arquitectura, estrategia de paralelismo, tolerancia a fallos y selección de parámetros se encuentra en `PROPUESTA_TECNICA.md`.
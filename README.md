# Banco XYZ - Migración de Procesos Batch

Proyecto desarrollado con **Spring Boot 4.0.7, Java 21 y Spring Batch** para modernizar tres procesos legacy del Banco XYZ a partir de archivos CSV.

## Objetivo

Implementar una solución batch que permita leer, validar, transformar y persistir información bancaria, manteniendo la integridad de los datos, tolerando errores controlados y utilizando procesamiento paralelo.

Los tres Jobs implementados son:

1. `transaccionesJob` — reporte de transacciones diarias.
2. `interesesJob` — cálculo de intereses mensuales.
3. `cuentasAnualesJob` — generación de estados de cuenta anuales.

## Requisitos técnicos de la actividad

| Requisito | Implementación |
|---|---|
| Spring Batch | Jobs y Steps independientes para cada proceso |
| CSV | `FlatFileItemReader` |
| Transformación y validación | `ItemProcessor` por proceso |
| Base de datos relacional | MySQL + Spring Data JPA |
| Manejo de errores | Validaciones y excepciones controladas |
| Tolerancia a fallos | `faultTolerant()` + `CustomSkipPolicy` |
| Auditoría de errores | `BatchSkipListener` |
| Chunk | Tamaño fijo de 5 registros |
| Escalamiento | 3 hilos concurrentes mediante `SimpleAsyncTaskExecutor` |
| Lectura paralela segura | `SynchronizedItemStreamReader` |
| Reporte anual | `output/reporte_anual_auditoria.csv` |

## Estructura

```text
src/main/java/com/bancoxyz/batch
├── batch
│   ├── BatchSkipListener.java
│   └── CustomSkipPolicy.java
├── config
│   ├── BatchInfrastructureConfig.java
│   ├── CuentasAnualesJobConfig.java
│   ├── InteresesJobConfig.java
│   └── TransaccionesJobConfig.java
├── model
├── processor
├── repository
└── BankBatchApplication.java

src/main/resources
├── data
│   ├── cuentas_anuales.csv
│   ├── intereses.csv
│   └── transacciones.csv
└── application.properties
```

## Procesamiento de errores

Los `ItemProcessor` validan identificadores, montos, fechas, tipos de operación, edades, descripciones y consistencia de movimientos.

Cuando se encuentra un error de datos, se lanza `IllegalArgumentException`. La `CustomSkipPolicy` permite omitir hasta 10 registros inválidos por Step. Los errores de infraestructura o persistencia no son ocultados por esta política.

El `BatchSkipListener` registra en consola los elementos omitidos, indicando si el error ocurrió durante lectura, procesamiento o escritura.

### Reglas relevantes

- Débitos con monto negativo: se normalizan a valor positivo.
- Montos cero en transacciones: se rechazan.
- Créditos con monto negativo: se rechazan.
- Fechas: se aceptan `yyyy-MM-dd` y `yyyy/MM/dd`.
- Tipos de transacción no reconocidos: se rechazan.
- Movimientos duplicados en el proceso diario: se rechazan.
- Cuentas hipotecarias: se excluyen del cálculo mensual porque el proceso solicitado contempla ahorro y préstamo.
- Depósitos: deben tener monto mayor que cero.
- Retiros y compras: deben tener monto menor que cero.
- Descripciones vacías: se rechazan.

## Procesamiento paralelo

La actividad solicita **3 hilos de ejecución paralela** y **chunks de tamaño 5**.

La configuración se centraliza en `BatchInfrastructureConfig`:

```java
public static final int NUMERO_HILOS = 3;
public static final int CHUNK_SIZE = 5;
```

El `SimpleAsyncTaskExecutor` limita la concurrencia a 3 tareas y cada Step orientado a chunks utiliza:

```java
.chunk(5)
.taskExecutor(batchTaskExecutor)
```

Como `FlatFileItemReader` no es thread-safe, se utiliza `SynchronizedItemStreamReader` para proteger la lectura del CSV durante el procesamiento concurrente.

## Base de datos MySQL

Crear la base de datos:

```sql
CREATE DATABASE bank_batch_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

La contraseña no se almacena en el código fuente. Antes de ejecutar:

### PowerShell

```powershell
$env:DB_PASSWORD="CONTRASEÑA_MYSQL"
```

## Ejecución

### Transacciones diarias

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=transaccionesJob ejecucion=1"
```

### Intereses mensuales

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=interesesJob ejecucion=1"
```

### Estados de cuenta anuales

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=cuentasAnualesJob ejecucion=1"
```

Para volver a ejecutar un Job se debe cambiar el valor del parámetro `ejecucion`.

## Resultados esperados con los datos incluidos

### Transacciones

El archivo contiene dos anomalías principales: un monto cero y un registro duplicado. Además, un débito negativo es corregido automáticamente.

Resultado esperado:

- Registros válidos almacenados: **8**
- Débitos: **4**
- Créditos: **4**
- Anomalías omitidas: **2**
- Monto total procesado: **$9.400**

### Intereses

Las cuentas hipotecarias se filtran del cálculo.

Resultado esperado:

- Cuentas procesadas: **7**
- Ahorro: **4**
- Préstamo: **3**
- Interés total calculado: **$625,00**

### Estados de cuenta anuales

El registro con depósito de monto cero se omite por incumplir la regla de consistencia.

Resultado esperado:

- Movimientos incluidos: **8**
- Archivo generado: `output/reporte_anual_auditoria.csv`

## Evidencia de ejecución

La carpeta `evidencias/` conserva las capturas de la actividad anterior y `EVIDENCIA_EJECUCION.md` documenta los resultados que deben verificarse en consola al ejecutar la versión actual.

## Propuesta técnica

La justificación de arquitectura, manejo de errores, tolerancia a fallos y escalamiento se encuentra en `PROPUESTA_TECNICA.md`.

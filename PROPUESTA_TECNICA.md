# Propuesta Técnica - Banco XYZ

## 1. Objetivo

La propuesta consiste en modernizar tres procesos batch legacy del Banco XYZ mediante Spring Batch, incorporando procesamiento por chunks, ejecución paralela, validación de datos, persistencia, auditoría y tolerancia a fallos.

Los procesos implementados son:

- Reporte de transacciones diarias.
- Cálculo de intereses mensuales.
- Generación de estados de cuenta anuales.

## 2. Arquitectura

Cada proceso se implementa como un Job independiente compuesto por Steps especializados.

El flujo principal es:

```text
CSV
 ↓
ItemReader
 ↓
ItemProcessor
 ↓
ItemWriter
 ↓
MySQL
```

Para el proceso anual se incorpora además un Step encargado de generar:

```text
output/reporte_anual_auditoria.csv
```

Los principales componentes utilizados son:

- `FlatFileItemReader`: lectura de archivos CSV.
- `SynchronizedItemStreamReader`: lectura segura durante la ejecución concurrente.
- `ItemProcessor`: validación y transformación de datos.
- Spring Data JPA: persistencia en MySQL.
- `ThreadPoolTaskExecutor`: procesamiento paralelo.
- `CustomSkipPolicy`: manejo de registros inválidos.
- `RetryPolicy`: recuperación frente a errores transitorios.
- `BatchSkipListener`: registro y auditoría de elementos rechazados.

## 3. Procesos implementados

### Transacciones diarias

`transaccionesJob` procesa `transacciones.csv`.

Se validan identificadores, fechas, tipos de transacción, montos y registros duplicados.

Los débitos negativos son normalizados y los registros inválidos son omitidos mediante la política de Skip.

Resultado validado:

- 8 registros válidos.
- 4 débitos.
- 4 créditos.
- Monto total de $9.400.
- 2 registros rechazados.

### Intereses mensuales

`interesesJob` procesa `intereses.csv`.

Se validan datos de la cuenta, saldo, edad y tipo de producto. Las cuentas hipotecarias se excluyen del cálculo solicitado.

Resultado validado:

- 7 cuentas procesadas.
- 4 cuentas de ahorro.
- 3 cuentas de préstamo.
- Interés total calculado de $625.

### Estados de cuenta anuales

`cuentasAnualesJob` procesa `cuentas_anuales.csv`.

Se validan fechas, tipos de movimiento, descripción y consistencia del monto.

Los movimientos válidos se almacenan en MySQL y posteriormente se genera:

```text
output/reporte_anual_auditoria.csv
```

Resultado validado:

- 8 movimientos incluidos.
- 1 registro inválido rechazado.

## 4. Procesamiento paralelo

La ejecución paralela se implementa mediante `ThreadPoolTaskExecutor`.

Los parámetros se externalizan en `application.properties`:

```properties
batch.chunk-size=5
batch.core-pool-size=2
batch.max-pool-size=2
batch.queue-capacity=10
```

Los Steps utilizan `.taskExecutor(batchTaskExecutor)` para ejecutar chunks concurrentemente.

Como `FlatFileItemReader` no es thread-safe, se utiliza `SynchronizedItemStreamReader` para proteger la lectura y mantener la integridad de los datos.

## 5. Comparación y selección de parámetros

Para seleccionar la configuración se realizaron distintas pruebas sobre `transaccionesJob`.

| Configuración | Step principal | Job completo |
|---|---:|---:|
| 1 hilo / chunk 5 | 174 ms | 649 ms |
| **2 hilos / chunk 5** | **144 ms** | **622 ms** |
| 3 hilos / chunk 5 | 148 ms | 631 ms |
| 2 hilos / chunk 2 | 170 ms | 645 ms |
| 2 hilos / chunk 1 | 209 ms | 693 ms |

La mejor configuración observada fue utilizar **2 hilos y chunks de 5 registros**.

Las pruebas también permitieron comprobar que aumentar la cantidad de hilos o reducir demasiado el tamaño del chunk no necesariamente mejora el rendimiento.

Debido al tamaño reducido de los archivos utilizados, los tiempos obtenidos se consideran referenciales.

## 6. Tolerancia a fallos

Los Steps de procesamiento utilizan `faultTolerant()` y diferencian entre errores de datos y errores transitorios.

### Skip Policy

La clase `CustomSkipPolicy` permite omitir únicamente errores de validación representados por `IllegalArgumentException`.

El límite se configura mediante:

```properties
batch.max-skips=10
```

Cuando el límite es superado, el Step deja de continuar con nuevos rechazos.

Los errores de infraestructura o persistencia no son ocultados mediante esta política.

### Retry Policy

Los errores transitorios derivados de `TransientDataAccessException` utilizan una política de reintentos.

La configuración es:

```properties
batch.retry-limit=3
batch.retry-initial-interval=500
batch.retry-multiplier=2.0
batch.retry-max-interval=2000
```

Esto permite realizar hasta 3 reintentos con espera incremental ante una falla temporal.

Si el problema persiste después de los reintentos, el Step falla en lugar de ignorar el error.

No se utiliza Retry para registros inválidos, ya que volver a procesar el mismo dato no corrige su contenido.

## 7. Auditoría de errores

`BatchSkipListener` utiliza SLF4J para registrar los elementos rechazados con nivel `WARN`.

Además, los registros omitidos quedan almacenados en:

```text
output/registros_rechazados.csv
```

El archivo registra:

- Fecha y hora.
- Fase del procesamiento.
- Elemento rechazado.
- Motivo del error.

La escritura se encuentra sincronizada para evitar conflictos entre los hilos de ejecución.

Esto permite conservar evidencia de los registros problemáticos para auditoría y posible reproceso.

## 8. Pool de conexiones

Se utiliza HikariCP para controlar las conexiones disponibles hacia MySQL.

Configuración:

```properties
spring.datasource.hikari.maximum-pool-size=8
spring.datasource.hikari.minimum-idle=3
spring.datasource.hikari.connection-timeout=30000
```

De esta forma, el procesamiento concurrente dispone de un pool de conexiones administrado y limitado.

## 9. Persistencia e integridad

Los resultados se almacenan en MySQL mediante Spring Data JPA.

Las principales tablas utilizadas son:

- `transacciones_procesadas`
- `intereses_calculados`
- `movimientos_anuales`

Además, Spring Batch mantiene sus propias tablas de metadata para controlar las ejecuciones de Jobs y Steps.

Los valores monetarios utilizan `BigDecimal` y las fechas `LocalDate`.

Cada Job limpia sus resultados anteriores antes de iniciar una nueva ejecución, evitando inconsistencias entre distintas pruebas.

## 10. Seguridad de configuración

La contraseña de MySQL no se almacena directamente en el código.

Se utiliza:

```properties
spring.datasource.password=${DB_PASSWORD}
```

La variable debe configurarse localmente antes de ejecutar el proyecto.

## 11. Pruebas

Se implementaron pruebas para validar la carga del contexto de Spring Boot y la política de Retry.

El test de Retry verifica:

- Recuperación frente a un error transitorio.
- Ausencia de reintentos frente a un error no transitorio.

Resultado final:

```text
Tests run: 3
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

## 12. Conclusión

La solución moderniza los tres procesos batch del Banco XYZ mediante una arquitectura basada en Jobs y Steps independientes.

Se incorpora procesamiento paralelo configurable, selección de parámetros mediante pruebas comparativas, tolerancia a fallos mediante Skip y Retry, auditoría de registros rechazados, persistencia en MySQL y administración de conexiones mediante HikariCP.

La configuración final de 2 hilos y chunks de 5 registros presentó el mejor resultado observado en las pruebas realizadas, manteniendo correctamente la integridad y los resultados de los tres procesos.
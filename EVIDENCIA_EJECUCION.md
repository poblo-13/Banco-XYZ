# Evidencia de ejecución - Exp1_S2_Grupo14

Las capturas en `evidencias/` corresponden a la versión final del proyecto, con la política personalizada de skip y el escalamiento a 3 hilos con chunks de 5 ya en funcionamiento. Todos los Jobs fueron ejecutados con `ejecucion=2` sobre MySQL activo.

## 1. Reporte de transacciones diarias

Comando:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=transaccionesJob ejecucion=2"
```

Resultado obtenido en consola:

```text
[CLEANUP] Registros anteriores eliminados.
[CORRECCION] Débito negativo normalizado. ID=3
[SKIP][PROCESS] Monto cero para transacción ID 4 | item=TransaccionCsv{...}
[SKIP][PROCESS] Transacción duplicada detectada para ID 8 | item=TransaccionCsv{...}
====================================
 REPORTE DE TRANSACCIONES DIARIAS
====================================
Registros válidos almacenados: 8
Débitos: 4
Créditos: 4
Monto total: $9400.00
====================================
```

El Job finaliza con estado `COMPLETED` a pesar de los dos registros inválidos/duplicados, confirmando la tolerancia a fallos.

**Evidencia:** `evidencias/01_job_transacciones_consola.png`

## 2. Cálculo de intereses mensuales

Comando:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=interesesJob ejecucion=2"
```

Resultado obtenido en consola:

```text
====================================
 CÁLCULO DE INTERESES MENSUALES
====================================
Cuentas procesadas: 7
Cuentas de ahorro: 4
Cuentas de préstamo: 3
Interés total calculado: $625.00
====================================
```

La cuenta hipotecaria (ID 105) queda excluida del cálculo, tal como exige el requerimiento.

**Evidencia:** `evidencias/02_job_intereses_consola.png`

## 3. Estados de cuenta anuales

Comando:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=cuentasAnualesJob ejecucion=2"
```

Resultado obtenido en consola:

```text
====================================
 ESTADOS DE CUENTA ANUALES
====================================
Movimientos incluidos: 8
Archivo generado: ...\output\reporte_anual_auditoria.csv
====================================
```

**Evidencia:** `evidencias/03_job_cuentas_anuales_consola.png`

## 4. Persistencia en MySQL

Se verificó que los tres Jobs efectivamente persisten los datos en la base relacional `bank_batch_db`:

```sql
SELECT * FROM transacciones_procesadas;   -- 8 filas
SELECT * FROM intereses_calculados;       -- 7 filas
SELECT * FROM movimientos_anuales;        -- 8 filas
```

**Evidencia:**
- `evidencias/04_mysql_transacciones_procesadas.png`
- `evidencias/05_mysql_intereses_calculados.png`
- `evidencias/06_mysql_movimientos_anuales.png`

## 5. Archivo de reporte anual generado

Se verificó el contenido de `output/reporte_anual_auditoria.csv`, confirmando el saldo acumulado por cuenta calculado en `reporteAnualStep`.

**Evidencia:** `evidencias/07_reporte_anual_csv.png`

## 6. Evidencia de escalamiento (3 hilos, chunk 5)

La configuración se centraliza en `BatchInfrastructureConfig.java`:

```java
public static final int NUMERO_HILOS = 3;
public static final int CHUNK_SIZE = 5;
```

Y se aplica a los tres Steps principales mediante:

```java
.chunk(BatchInfrastructureConfig.CHUNK_SIZE)
.taskExecutor(batchTaskExecutor)
```

**Evidencia:** `evidencias/08_config_escalamiento.png`

## 7. Evidencia de tolerancia a fallos

Los mensajes `[SKIP][PROCESS]` visibles en la consola del punto 1 confirman que `CustomSkipPolicy` (`src/main/java/com/bancoxyz/batch/batch/CustomSkipPolicy.java`) permite omitir registros inválidos sin detener el Job, mientras `BatchSkipListener` registra cada omisión para auditoría.
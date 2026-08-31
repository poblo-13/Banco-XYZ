# Evidencia de ejecución - Exp1_S3_Pablo_Pilar_Rojas

Este documento resume las evidencias correspondientes a la Semana 3 del proyecto Banco XYZ.

## 1. Configuración de Semana 3

Se implementó procesamiento paralelo con `ThreadPoolTaskExecutor`, parámetros externalizados para chunks, hilos y cola de ejecución, tolerancia a fallos mediante Retry y Skip, y configuración del pool de conexiones HikariCP.

**Evidencia:** `evidencias/Evidencias S3/01_configuracion_semana3.png`

## 2. Benchmark de escalamiento

Se probaron distintas combinaciones de cantidad de hilos y tamaño de chunk. La mejor configuración observada fue:

- 2 hilos
- chunk de 5 registros

**Evidencia:** `evidencias/Evidencias S3/02_benchmark_escalamiento.png`

## 3. Reporte de transacciones diarias

El Job `transaccionesJob` procesa correctamente los registros, normaliza valores y omite mediante Skip los registros inválidos.

Resultado:

- 8 registros válidos
- 4 débitos
- 4 créditos
- monto total: $9400.00

**Evidencia:** `evidencias/Evidencias S3/03_transacciones_final.png`

## 4. Cálculo de intereses mensuales

El Job `interesesJob` procesa las cuentas y calcula los intereses correspondientes.

Resultado:

- 7 cuentas procesadas
- 4 cuentas de ahorro
- 3 cuentas de préstamo
- interés total calculado: $625.00

**Evidencia:** `evidencias/Evidencias S3/04_intereses_final.png`

## 5. Estados de cuenta anuales

El Job `cuentasAnualesJob` procesa los movimientos anuales y genera el archivo de auditoría.

Resultado:

- 8 movimientos incluidos
- archivo `output/reporte_anual_auditoria.csv` generado correctamente

**Evidencia:** `evidencias/Evidencias S3/05_cuentas_anuales_final.png`

## 6. Auditoría de registros rechazados

Los registros descartados durante el procesamiento son almacenados en:

`output/registros_rechazados.csv`

Se registran casos como:

- monto cero
- transacción duplicada
- depósito inválido

**Evidencia:** `evidencias/Evidencias S3/06_registros_rechazados.png`

## 7. Pruebas automatizadas

Se ejecutaron las pruebas del proyecto, incluyendo pruebas específicas para la política de Retry.

Resultado:

- Tests ejecutados: 3
- Failures: 0
- Errors: 0
- Skipped: 0
- BUILD SUCCESS

**Evidencia:** `evidencias/Evidencias S3/07_pruebas_retry.png`
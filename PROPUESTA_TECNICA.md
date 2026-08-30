# Propuesta Técnica - Banco XYZ

## 1. Objetivo

La propuesta consiste en modernizar tres procesos batch legacy del Banco XYZ mediante Spring Batch. La solución reemplaza un procesamiento secuencial tradicional por una arquitectura basada en Jobs, Steps y procesamiento por chunks, incorporando validación de datos, tolerancia a fallos y ejecución paralela.

Los procesos migrados son:

- Reporte de transacciones diarias.
- Cálculo de intereses mensuales.
- Generación de estados de cuenta anuales.

## 2. Arquitectura

Cada proceso se estructura como un Job independiente. Los Steps de procesamiento utilizan el patrón:

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

Para el reporte anual se agrega:

```text
MySQL
 ↓
Step de reporte
 ↓
reporte_anual_auditoria.csv
```

### Componentes principales

- **ItemReader:** `FlatFileItemReader` para leer los archivos CSV.
- **SynchronizedItemStreamReader:** protege la lectura del archivo en los Steps paralelos.
- **ItemProcessor:** valida, normaliza y transforma los registros.
- **ItemWriter:** persiste los resultados mediante Spring Data JPA.
- **JobRepository:** mantiene la metadata de Jobs y Steps.
- **CustomSkipPolicy:** determina qué errores de datos pueden omitirse.
- **BatchSkipListener:** registra los registros omitidos.
- **SimpleAsyncTaskExecutor:** permite ejecutar chunks de forma concurrente.

## 3. Job de reporte de transacciones diarias

El `transaccionesJob` utiliza `transacciones.csv`.

### Validaciones y transformaciones

El processor valida:

- ID numérico.
- Fecha válida.
- Tipo `debito` o `credito`.
- Monto diferente de cero.
- Consistencia del signo del monto.
- Duplicados.

Los débitos negativos son normalizados mediante valor absoluto. Los registros que no cumplen las reglas se envían a la política de skip.

### Resultado

Los datos válidos se almacenan en `transacciones_procesadas`. Al finalizar se genera un resumen con cantidad de registros, débitos, créditos y monto total.

Con el archivo incluido, se esperan 8 registros válidos y 2 registros omitidos.

## 4. Job de cálculo de intereses mensuales

El `interesesJob` procesa `intereses.csv`.

Se validan:

- Identificador de cuenta.
- Nombre.
- Saldo.
- Edad.
- Tipo de cuenta.

Las cuentas hipotecarias son filtradas porque el requerimiento solicita aplicar intereses sobre cuentas de ahorro y préstamos.

Para la simulación se mantienen las tasas definidas en la actividad anterior:

- Ahorro: 0,5 % mensual.
- Préstamo: 1,5 % mensual.

Los resultados se almacenan en `intereses_calculados`, incluyendo saldo inicial, tasa, interés calculado y saldo final. De esta forma el nuevo saldo queda persistido en la base de datos para su consulta posterior.

## 5. Job de estados de cuenta anuales

El `cuentasAnualesJob` procesa `cuentas_anuales.csv`.

Se aplican reglas de consistencia:

- Depósito > 0.
- Retiro < 0.
- Compra < 0.
- Descripción obligatoria.
- Fecha válida.
- Tipo de movimiento permitido.

Los movimientos válidos se almacenan en `movimientos_anuales`.

Luego, un Step independiente ordena los movimientos por cuenta y fecha y genera:

`output/reporte_anual_auditoria.csv`

El reporte incluye el saldo acumulado por cuenta, facilitando su utilización para auditoría.

## 6. Manejo de errores y excepciones

La solución utiliza `faultTolerant()` en los Steps de procesamiento.

En lugar de detener todo el Job frente a un dato incorrecto, los errores de validación son evaluados por `CustomSkipPolicy`.

La política permite:

- Omitir excepciones de tipo `IllegalArgumentException`.
- Permitir hasta 10 omisiones por Step.
- Detener el procesamiento cuando se supera el límite.
- No ocultar errores de infraestructura o persistencia.

Esto permite separar los errores producidos por datos incorrectos de los errores técnicos que requieren detener el proceso.

## 7. Política personalizada de tolerancia a fallos

La clase `CustomSkipPolicy` implementa `SkipPolicy`.

La decisión se basa en dos condiciones:

```text
¿El error corresponde a una validación de datos?
        │
        ├── No → no se omite y el Step falla
        │
        └── Sí
             │
             ├── skipCount < 10 → omitir y continuar
             │
             └── skipCount >= 10 → detener el Step
```

Además, `BatchSkipListener` registra los registros omitidos para facilitar el diagnóstico y la auditoría.

No se utiliza retry para datos inválidos porque volver a procesar exactamente el mismo registro no corrige su contenido.

## 8. Política de escalamiento

La actividad solicita tres hilos de ejecución paralela con chunks de tamaño 5.

La configuración utilizada es:

```text
Hilos concurrentes: 3
Chunk: 5 registros
```

El `SimpleAsyncTaskExecutor` establece un límite de concurrencia de 3. Los Steps utilizan `.taskExecutor(batchTaskExecutor)` y `.chunk(5)`.

Debido a que `FlatFileItemReader` no es thread-safe, se utiliza `SynchronizedItemStreamReader`, evitando que dos hilos lean simultáneamente el mismo estado interno del archivo.

El paralelismo se aplica al procesamiento de los chunks, mientras la lectura del archivo se sincroniza para mantener la integridad de la secuencia de entrada.

## 9. Persistencia

La base de datos seleccionada es MySQL.

Spring Data JPA permite persistir:

- `transacciones_procesadas`
- `intereses_calculados`
- `movimientos_anuales`

Spring Batch utiliza además sus tablas de metadata para controlar las ejecuciones.

Los valores monetarios se representan mediante `BigDecimal`, evitando errores de precisión propios de tipos de punto flotante.

Las fechas se representan mediante `LocalDate`.

## 10. Integridad y consistencia

Las reglas de validación se aplican antes de la escritura.

Esto permite que solamente los registros que cumplen las reglas de negocio sean almacenados.

Además:

- Cada Job inicia limpiando sus resultados anteriores para evitar duplicados al ejecutar una nueva instancia.
- Las transacciones se procesan por chunks.
- Los errores de datos quedan registrados.
- Los errores no recuperables provocan el fallo del Step.
- El reporte anual se genera después de completar la persistencia.

## 11. Seguridad de configuración

La contraseña de MySQL no se almacena en el código fuente.

Se utiliza:

```properties
spring.datasource.password=${DB_PASSWORD}
```

La variable de entorno debe configurarse localmente antes de ejecutar el proyecto.

## 12. Conclusión

La solución permite migrar los tres procesos legacy del Banco XYZ hacia una implementación moderna con Spring Batch.

La arquitectura incorpora los elementos solicitados en la actividad: lectura de CSV, procesamiento y validación mediante `ItemProcessor`, persistencia en una base de datos relacional, manejo de errores, política personalizada de tolerancia a fallos y escalamiento mediante tres hilos concurrentes con chunks de cinco registros.

Con esto se obtiene una solución más controlada y mantenible, capaz de continuar procesando datos válidos cuando encuentra errores aislados y de detenerse ante situaciones que comprometan la integridad del procesamiento.

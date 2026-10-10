# Evidencias de Ejecución — Banco XYZ

**Asignatura:** Desarrollo Backend III (PBY2203)\
**Evaluación:** Experiencia 3 — Semana 9\
**Rama:** [`semana9-pablo`](https://github.com/poblo-13/Banco-XYZ/tree/semana9-pablo)\
**Estado de referencia:** commit `50953c8` (10 de octubre de 2026)

## 1. Propósito y criterio de respaldo

Este documento resume las **pruebas ya registradas** y las relaciona con los elementos de código y documentación existentes. Se distingue entre resultados de ejecución reportados en el trabajo anterior, evidencia persistida en el repositorio y elementos aún pendientes de demostrar. No se presentan nuevas capturas como si ya hubieran sido obtenidas.

La **Semana 9** comprende la separación de Cuentas, Clientes y Pagos, consumidores Kafka independientes de Auditoría, Notificaciones y Riesgo, migraciones con Spring Batch y mecanismos de recuperación. Los resultados anteriores de Semana 8 se mantienen como antecedentes, sin confundirlos con las pruebas de la arquitectura nueva.

## 2. Resumen de evidencias verificables

| ID | Prueba / componente | Resultado registrado | Respaldo |
|---|---|---|---|
| S9-01 | Separación en microservicios | Módulos Cuentas, Clientes y Pagos existentes | `pom.xml`, código de los tres servicios y `docker-compose.yml` |
| S9-02 | Seguridad por scopes | Clientes OAuth2 para canales y operaciones internas definidos | `authorization-server/src/main/resources/application.properties` y configuraciones de seguridad |
| S9-03 | Mensajería Kafka con tres consumidores | Auditoría, Notificaciones y Riesgo escuchan `banco.movimientos` en grupos distintos | Código de los consumidores y sus `application.properties` |
| S9-04 | Escalamiento funcional de Kafka | 24 eventos simulados procesados por cada servicio; 72 registros en total | `docs/EVIDENCIA-KAFKA-SEMANA9.md` |
| S9-05 | Distribución entre seis instancias | Dos instancias activas por cada uno de los tres servicios consumidores durante la prueba | `docs/EVIDENCIA-KAFKA-SEMANA9.md` |
| S9-06 | Alerta de riesgo | Reportada para un retiro simulado de 15.000 | Documento Kafka y regla en `RiskConsumer.java` |
| S9-07 | Migración de datos heredados | Tres migraciones, 3.054 registros procesados sin pérdidas según validación previa | Resultado de ejecución aportado en la continuación del proyecto; jobs en `batch-migration` |
| S9-08 | Spring Batch: pruebas automatizadas | **17 pruebas exitosas, 0 fallos, 0 errores** | Resultado previo de `mvn -pl batch-migration -am test` y fuentes de test |
| S9-09 | Recuperación ante errores transitorios | Tres pruebas de integración superadas, con un fallo simulado por job | `ReintentosBatchIntegrationTest.java`; base temporal H2 |
| S9-10 | Confirmación en GitHub | Commit `50953c8` publicado en `semana9-pablo`; directorio limpio al terminar el paso 318 | Historial Git informado en la conversación y rama publicada |

> **Importante:** la inspección del repositorio verifica que el código y los documentos están versionados, pero **no equivale a volver a ejecutar** aquí los contenedores o las pruebas. Los resultados de ejecución se atribuyen a las validaciones ya compartidas.

## 3. Prueba Kafka — Tres microservicios independientes

**Fuente:** [`docs/EVIDENCIA-KAFKA-SEMANA9.md`](docs/EVIDENCIA-KAFKA-SEMANA9.md).\
**Fecha anotada en esa evidencia:** 10-10-2026, 00:15:05.

**Configuración:**

```text
Tópico: banco.movimientos
Particiones: 3
Grupos: banco-auditoria / banco-notificaciones / banco-riesgo
Réplicas de prueba: 2 por servicio
Instancias consumidoras: 6
```

Se publicaron **24 eventos simulados**, sin cambiar saldos bancarios. Cada grupo consumidor los procesó de forma independiente:

| Servicio | Eventos persistidos | Instancia 1 | Instancia 2 |
|---|---:|---:|---:|
| Auditoría | 24 | 7 | 17 |
| Notificaciones | 24 | 17 | 7 |
| Riesgo | 24 | 7 | 17 |
| **Total en las tres bases** | **72** | **31** | **41** |

Estas cifras muestran reparto de carga funcional en el ejercicio realizado; **no son una medición de rendimiento**, disponibilidad ni capacidad a escala de producción. La base `docker-compose.yml` define dos instancias de auditoría y una de notificaciones y riesgo; durante la prueba se ejecutaron dos por servicio.

Se registró también una alerta de riesgo para un retiro de `15.000`. El código del consumidor de Riesgo marca `ALERTA` cuando se trata de un retiro cuyo monto es igual o superior a `10.000`.

## 4. Protección frente a duplicados y consistencia

Los consumidores persisten el identificador del evento y contrastan el contenido mediante un hash; esto sirve para no generar registros repetidos ante entregas duplicadas y para detectar un mismo ID con contenido distinto.

En el flujo transaccional de Semana 9:

1. `payment-service` registra o recupera una operación por `operacionId` y consulta el resultado a `account-service`.
2. Una vez confirmada, guarda el estado del movimiento y un registro Outbox en su base de datos.
3. `OutboxPublisher` toma eventos pendientes, los publica en Kafka y marca `PUBLICADO` tras recibir la confirmación.
4. Los tres consumidores procesan el evento `MOVIMIENTO_CONFIRMADO` de forma independiente.

**Evidencia de implementación:** `MovimientoService.java`, `EventoMovimientoOutbox.java` y `OutboxPublisher.java` dentro de `payment-service`, además de los tres consumidores. Este apartado describe el código; **no afirma una nueva prueba de caída real de Kafka**.

## 5. Migraciones de datos heredados — Spring Batch

Los tres jobs implementados son:

| Job | Archivo por semana | Tabla destino |
|---|---|---|
| `movimientosJob` | `movimientos_financieros_diarios.csv` | `legacy_movimientos` |
| `interesesJob` | `intereses_trimestrales.csv` | `legacy_intereses` |
| `estadosJob` | `estados_financieros_anuales.csv` | `legacy_estados` |

Cada job procesa archivos de `semana_1`, `semana_2` y `semana_3`, con flujos paralelos. Los registros inválidos se almacenan como `RECHAZADO` y se anota el motivo para su posterior revisión. La clave compuesta `(semana, linea)` y la operación de actualización en conflicto permiten repetir la carga sin duplicar filas por esa clave.

**Resultado informado y validado en la continuación del trabajo:** las tres migraciones procesaron **3.054 registros sin pérdidas**. No se consigna un desglose por tabla porque no consta en los antecedentes consultados.

## 6. Recuperación Spring Batch — 17 pruebas

Se incorporó `ReintentosBatchIntegrationTest.java`, con tres verificaciones que ejecutan jobs reales contra una base **H2 temporal**, utilizando archivos CSV sintéticos. Un `DataSource` de prueba provoca una excepción SQL transitoria al intentar escribir por primera vez para cada proceso.

| Job | Fallo provocado | Resultado esperado y reportado |
|---|---|---|
| Movimientos | Primer intento de escritura | El job se recupera y almacena 3 filas de prueba |
| Intereses | Primer intento de escritura | El job se recupera y almacena 3 filas de prueba |
| Estados financieros | Primer intento de escritura | El job se recupera y almacena 3 filas de prueba |

Resultado registrado:

```text
Pruebas automatizadas Spring Batch: 17
Exitosas: 17
Fallos: 0
Errores: 0
BUILD SUCCESS
```

Comando para **repetir la validación** desde la raíz del repositorio (esto no indica que se haya vuelto a ejecutar ahora):

```powershell
mvn -pl batch-migration -am test
```

**Alcance real:** se probó un error transitorio **simulado**, no una desconexión real de MySQL ni una interrupción de infraestructura productiva.

## 7. Verificaciones de despliegue y seguridad

| Elemento | Sustento actual | Precaución al presentar |
|---|---|---|
| Docker Compose | Definición de servicios y dependencias en `docker-compose.yml` | Mostrar con `docker compose ps` durante el video si se quiere demostrar ejecución actual |
| Puerto BFF Web | `8181:8081` | No confundir con la publicación `8081` de Semana 8 |
| Core anterior | Perfil `legacy` | No presentarlo como núcleo predeterminado de Semana 9 |
| Bases de datos | Una instancia MySQL con bases lógicas separadas | No describirlas como servidores físicos diferentes |
| OAuth2 | Definición de clientes y scopes en Authorization Server | Probar 401/403 en vivo si la pauta solicita seguridad funcional |
| Outbox | Implementado en Pagos | No equipararlo a transacción distribuida entre Cuentas y Pagos |
| AWS | Sin ejecución remota documentada en estas evidencias | Mencionar como pendiente, a menos que se aporte un registro adicional |

Las capturas de semanas anteriores siguen disponibles en `evidencias/` (S1 a S8). Al revisar esta rama, **no se encontró una carpeta de capturas S9**; la evidencia escrita específica de Kafka está en `docs/EVIDENCIA-KAFKA-SEMANA9.md`.

## 8. Evidencia audiovisual para la entrega

Si el video forma parte de la entrega, conviene mostrar una demostración **breve y reproducible** que incluya:

1. Estructura de microservicios, configuración Compose y separación del Core legado.
2. Un flujo OAuth2 y una consulta u operación sobre un BFF.
3. Consumo del mismo conjunto de eventos por Auditoría, Notificaciones y Riesgo, con dos instancias por grupo si se vuelve a demostrar el escalamiento.
4. Estado de las bases o contadores de eventos; explicar claramente que los 24 eventos fueron simulados.
5. Resultado de pruebas Batch y resumen `BUILD SUCCESS`.

**No se adjunta ni se afirma haber grabado un video** en esta documentación. Cuando exista, puede añadirse su nombre o enlace y duración en esta sección, sin subir credenciales ni tokens completos.

### ¿Se necesitan capturas adicionales?

En ausencia de un requisito explícito de la pauta, **no se consideran indispensables capturas nuevas** si el video y los resultados escritos permiten verificar las funcionalidades. Es recomendable conservar las capturas históricas y, si hiciera falta una evidencia fija, limitarla a unas pocas imágenes muy relevantes. No crear capturas ficticias ni presentar resultados del video como si fueran archivos ya incorporados.

## 9. Limitaciones pendientes

- Prueba de falla real de una réplica Kafka y recuperación automática: **no acreditada** por los resultados consultados.
- Kafka usa un solo broker de desarrollo: **sin alta disponibilidad**.
- Seguridad del broker Kafka y endurecimiento de despliegue para producción: **pendientes**.
- Pruebas de rendimiento o carga a gran escala: **no realizadas** en las evidencias consultadas.
- Despliegue efectivo en AWS: **no documentado** en estos respaldos.
- Confirmar los requisitos formales de capturas y video: **depende de la pauta de evaluación**.

## 10. Referencias internas

- [`README.md`](README.md)
- [`DIAGRAMA_ARQUITECTURA.md`](DIAGRAMA_ARQUITECTURA.md)
- [`PROPUESTA_TECNICA.md`](PROPUESTA_TECNICA.md)
- [`docs/EVIDENCIA-KAFKA-SEMANA9.md`](docs/EVIDENCIA-KAFKA-SEMANA9.md)
- [`batch-migration/src/test/java/com/bancoxyz/batch/ReintentosBatchIntegrationTest.java`](batch-migration/src/test/java/com/bancoxyz/batch/ReintentosBatchIntegrationTest.java)
- [`batch-migration/src/test/java/com/bancoxyz/batch/ValidacionesBatchTest.java`](batch-migration/src/test/java/com/bancoxyz/batch/ValidacionesBatchTest.java)
- [`docker-compose.yml`](docker-compose.yml)

---

**Conclusión:** las evidencias disponibles respaldan la separación de dominios, el consumo independiente de Kafka y su distribución funcional, el procesamiento de 3.054 registros heredados y la recuperación simulada de fallos transitorios mediante 17 pruebas automatizadas. Este documento no sustituye una demostración actual de ejecución ni atribuye capturas o despliegues no documentados.

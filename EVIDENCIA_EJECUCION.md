# Evidencias de Ejecución
## Banco XYZ - Backend for Frontend (BFF)
### Exp2 - Semana 5

Este documento reúne las evidencias de ejecución de la solución Backend for Frontend desarrollada para Banco XYZ.

La implementación considera un Core API y tres BFF independientes para los canales Web, Mobile y ATM. Las evidencias muestran la ejecución de los servicios, comunicación HTTPS, autenticación y autorización JWT por canal, operaciones bancarias, manejo de errores, observabilidad, compilación y persistencia en MySQL.

---

## Evidencia 1 - Estructura del proyecto BFF

Se observa la organización modular del proyecto mediante un Core API y tres BFF independientes para Web, Mobile y ATM.

![Estructura del proyecto](evidencias/Evidencias%20S5/01_Estructura_Proyecto_BFF.png)

---

## Evidencia 2 - Ejecución del Core API

Se observa el inicio exitoso del servicio central del Banco XYZ en el puerto 8080. El Core API utiliza Spring Data JPA y mantiene la comunicación con la base de datos MySQL.

![Core API](evidencias/Evidencias%20S5/02_Core_API_Puerto_8080.png)

---

## Evidencia 3 - BFF Web mediante HTTPS

Se observa el inicio exitoso del BFF Web en el puerto 8081 utilizando HTTPS y el certificado local configurado con el alias `bancoxyz-local`.

![BFF Web HTTPS](evidencias/Evidencias%20S5/03_BFF_Web_HTTPS_8081.png)

---

## Evidencia 4 - BFF Mobile mediante HTTPS

Se observa el inicio exitoso del BFF Mobile en el puerto 8082 utilizando HTTPS y el certificado local configurado.

![BFF Mobile HTTPS](evidencias/Evidencias%20S5/04_BFF_Mobile_HTTPS_8082.png)

---

## Evidencia 5 - BFF ATM mediante HTTPS

Se observa el inicio exitoso del BFF ATM en el puerto 8083 utilizando HTTPS y el certificado local configurado.

![BFF ATM HTTPS](evidencias/Evidencias%20S5/05_BFF_ATM_HTTPS_8083.png)

---

## Evidencia 6 - Autenticación JWT del BFF Web

Se observa la generación correcta de un token Bearer para el canal Web, con una duración de 1800 segundos y el scope específico `WEB_ACCESS`.

El token se muestra parcialmente para evitar exponer su contenido completo.

![JWT Web](evidencias/Evidencias%20S5/06_JWT_Web_WEB_ACCESS.png)

---

## Evidencia 7 - Endpoint protegido del BFF Web

Se observa el acceso exitoso mediante JWT al endpoint `/api/web/cuentas`.

La respuesta contiene información completa de las cuentas, incluyendo titular, edad, tipo de cuenta, saldo, tasa de interés e interés calculado, demostrando una respuesta adaptada al canal Web.

![Endpoint Web](evidencias/Evidencias%20S5/07_Web_Endpoint_Protegido_Cuentas.png)

---

## Evidencia 8 - Autenticación JWT del BFF Mobile

Se observa la generación correcta de un token Bearer para el canal Mobile, con una duración de 1800 segundos y el scope específico `MOBILE_ACCESS`.

![JWT Mobile](evidencias/Evidencias%20S5/08_JWT_Mobile_MOBILE_ACCESS.png)

---

## Evidencia 9 - Endpoint protegido del BFF Mobile

Se observa el acceso exitoso mediante JWT al endpoint `/api/mobile/cuentas/101`.

La respuesta contiene únicamente información esencial de la cuenta, evidenciando una respuesta más ligera y optimizada para dispositivos móviles.

![Endpoint Mobile](evidencias/Evidencias%20S5/09_Mobile_Endpoint_Protegido_Cuenta.png)

---

## Evidencia 10 - Autenticación JWT del BFF ATM

Se observa la generación correcta de un token Bearer para el canal ATM, con una duración de 1800 segundos y el scope específico `ATM_ACCESS`.

![JWT ATM](evidencias/Evidencias%20S5/10_JWT_ATM_ATM_ACCESS.png)

---

## Evidencia 11 - Consulta de saldo desde BFF ATM

Se observa el acceso exitoso mediante JWT al endpoint protegido `/api/atm/saldo/101`, obteniendo el saldo disponible de la cuenta mediante HTTPS.

![Consulta saldo ATM](evidencias/Evidencias%20S5/11_ATM_Consulta_Saldo_Protegida.png)

---

## Evidencia 12 - Retiro exitoso mediante BFF ATM

Se observa la ejecución exitosa de un retiro de 25 sobre la cuenta 101 mediante el endpoint protegido `/api/atm/retiros`.

La operación finaliza con estado `APROBADO` y actualiza el saldo disponible.

![Retiro ATM](evidencias/Evidencias%20S5/12_ATM_Retiro_Exitoso.png)

---

## Evidencia 13 - Manejo de saldo insuficiente

Se observa que un intento de retiro superior al saldo disponible es rechazado mediante `HTTP 409 Conflict`.

El BFF ATM entrega el código de error:

`SALDO_INSUFICIENTE`

La operación inválida no es procesada.

![Saldo insuficiente](evidencias/Evidencias%20S5/13_ATM_Error_Saldo_Insuficiente_409.png)

---

## Evidencia 14 - Acceso sin token JWT

Se observa que una solicitud al endpoint protegido `/api/atm/saldo/101` sin credenciales es rechazada mediante:

`HTTP 401 Unauthorized`

Esto demuestra que los recursos protegidos requieren autenticación.

![Sin token](evidencias/Evidencias%20S5/14_Seguridad_Sin_Token_401.png)

---

## Evidencia 15 - Token Web rechazado por BFF ATM

Se utiliza un token válido generado por el BFF Web para intentar acceder al BFF ATM.

La solicitud es rechazada mediante `HTTP 401 Unauthorized` e `invalid_token`, demostrando que los tokens generados para Web no son válidos para ATM.

![Token Web rechazado](evidencias/Evidencias%20S5/15_Seguridad_Token_Web_Rechazado_ATM_401.png)

---

## Evidencia 16 - Token Mobile rechazado por BFF ATM

Se utiliza un token válido generado por el BFF Mobile para intentar acceder al BFF ATM.

La solicitud es rechazada mediante `HTTP 401 Unauthorized` e `invalid_token`, confirmando la separación de autenticación y autorización entre los distintos canales.

![Token Mobile rechazado](evidencias/Evidencias%20S5/16_Seguridad_Token_Mobile_Rechazado_ATM_401.png)

---

## Evidencia 17 - Observabilidad mediante Spring Boot Actuator

Se consulta el endpoint `/actuator/health` del BFF Web mediante HTTPS.

El servicio devuelve:

`status: UP`

También se observan componentes asociados a disponibilidad, readiness, liveness y SSL.

![Actuator Health](evidencias/Evidencias%20S5/17_Actuator_Health_BFF_Web.png)

---

## Evidencia 18 - Compilación general del proyecto

Se observa la compilación exitosa de todos los módulos mediante Maven Reactor:

- Banco XYZ - BFF Semana 5
- Banco XYZ - Core API
- Banco XYZ - BFF Web
- Banco XYZ - BFF Mobile
- Banco XYZ - BFF ATM

La ejecución finaliza con:

`BUILD SUCCESS`

![Build Success](evidencias/Evidencias%20S5/18_Compilacion_General_BUILD_SUCCESS.png)

---

## Evidencia 19 - Persistencia en MySQL

Se observa en la base de datos `banco_xyz_core` la persistencia de la operación realizada mediante el BFF ATM.

Para la cuenta 101 se registra:

- Titular: John Doe
- Tipo de cuenta: ahorro
- Saldo actual: 4950.00
- Tipo de movimiento: retiro
- Monto: -25.00
- Descripción: Retiro realizado desde ATM

Esto demuestra el flujo completo:

`BFF ATM -> Core API -> JPA -> MySQL`

![Persistencia MySQL](evidencias/Evidencias%20S5/19_Base_Datos_Persistencia_Retiro_ATM.png)

---

# Resumen de evidencias

Las pruebas realizadas permiten comprobar:

- separación del sistema en tres BFF independientes;
- respuestas adaptadas a Web, Mobile y ATM;
- autenticación JWT específica por canal;
- autorización y aislamiento entre canales;
- comunicación segura mediante HTTPS;
- utilización de certificados;
- operaciones de consulta y retiro en ATM;
- manejo de errores mediante códigos HTTP;
- protección de endpoints sin autenticación;
- observabilidad mediante Spring Boot Actuator;
- compilación exitosa del proyecto completo;
- persistencia de las operaciones en MySQL.

## Conclusión

Las evidencias demuestran el funcionamiento de la arquitectura Backend for Frontend implementada para Banco XYZ.

Los tres canales disponen de backends independientes y adaptados a sus necesidades, manteniendo un Core API como capa central de acceso a los datos. La solución incorpora HTTPS, JWT, autorización diferenciada, manejo de errores, observabilidad y persistencia, cumpliendo los principales requerimientos técnicos definidos para la actividad de Semana 5.
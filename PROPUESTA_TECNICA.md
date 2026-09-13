# Propuesta Técnica
## Banco XYZ - Backend for Frontend (BFF)
### Exp2 - Semana 5

## 1. Introducción

Para la modernización del sistema Banco XYZ se propone implementar el patrón arquitectónico Backend for Frontend (BFF), separando la comunicación con los distintos tipos de clientes en backends especializados.

La solución contempla tres canales:
- Web
- Mobile
- Cajero Automático (ATM)

Cada canal posee necesidades diferentes en cuanto a cantidad de información, operaciones disponibles y seguridad. Por esta razón se implementa un BFF independiente para cada uno.

---

## 2. Estrategia de implementación

La estrategia seleccionada consiste en utilizar un Core API central encargado del acceso a los datos bancarios y tres BFF independientes que consumen dicho servicio.

La arquitectura queda organizada de la siguiente manera:

```text
Clientes
   |
   |-- Web
   |     |
   |     --> BFF Web
   |
   |-- Mobile
   |     |
   |     --> BFF Mobile
   |
   |-- ATM
         |
         --> BFF ATM

              |
              v
          Core API
              |
              v
            MySQL
```

Esta separación permite adaptar las respuestas de acuerdo con las necesidades de cada frontend sin duplicar la lógica principal de acceso a datos.

---

## 3. Core API

El módulo `bank-core-api` actúa como servicio central.

Sus responsabilidades principales son:
- acceso a la base de datos MySQL;
- consulta de cuentas;
- consulta de movimientos;
- registro de depósitos y retiros;
- actualización de saldos;
- validación de operaciones;
- manejo de errores del dominio.

El Core API utiliza Spring Web, Spring Data JPA, Hibernate, MySQL y una organización por capas mediante Repository, Service, DTO, Mapper y manejo global de excepciones.

El servicio se ejecuta en:
```text
http://localhost:8080
```

---

## 4. BFF Web

El BFF Web está orientado a interfaces de navegador que pueden presentar una mayor cantidad de información.

Se ejecuta en:
```text
https://localhost:8081
```

Características principales:
- información completa de cuentas;
- consulta de movimientos;
- generación de dashboard;
- autenticación JWT;
- autorización mediante `WEB_ACCESS`;
- comunicación HTTPS.

El canal Web entrega información como titular, edad, tipo de cuenta, saldo, tasa de interés e interés calculado.

---

## 5. BFF Mobile

El BFF Mobile está diseñado para reducir la cantidad de información transmitida y optimizar el consumo de recursos.

Se ejecuta en:
```text
https://localhost:8082
```

Características principales:
- respuestas ligeras;
- información esencial de cuentas;
- movimientos recientes;
- resumen simplificado;
- autenticación JWT;
- autorización mediante `MOBILE_ACCESS`;
- comunicación HTTPS.

Este enfoque permite reducir el volumen de datos enviados a dispositivos móviles.

---

## 6. BFF ATM

El BFF ATM está orientado exclusivamente a operaciones necesarias para un cajero automático.

Se ejecuta en:
```text
https://localhost:8083
```

Operaciones principales:
- consulta de saldo;
- retiros;
- validación de saldo disponible;
- manejo de errores de operación;
- autenticación JWT;
- autorización mediante `ATM_ACCESS`;
- comunicación HTTPS.

Para operaciones inválidas, como un retiro superior al saldo disponible, el sistema responde con códigos HTTP adecuados, por ejemplo:
```text
HTTP 409 Conflict
SALDO_INSUFICIENTE
```

---

## 7. Autenticación y autorización

Cada BFF implementa autenticación mediante JWT.

Los scopes utilizados son:
```text
WEB_ACCESS
MOBILE_ACCESS
ATM_ACCESS
```

Cada canal utiliza una clave de firma independiente. Por esta razón, un token generado por un BFF no puede utilizarse para acceder a otro.

Ejemplo:
```text
Token Web -> BFF Web     = permitido
Token Web -> BFF ATM     = rechazado
Token Mobile -> BFF ATM  = rechazado
```

Los endpoints protegidos sin token responden con `HTTP 401 Unauthorized`.

---

## 8. Seguridad HTTPS

Los tres BFF utilizan HTTPS.

Para el entorno local se utiliza un certificado PKCS12 generado mediante `keytool`. El certificado se configura mediante variables de entorno y no se almacena en el repositorio Git.

Puertos seguros utilizados:
```text
Web     8081 HTTPS
Mobile  8082 HTTPS
ATM     8083 HTTPS
```

También se utilizan cabeceras de seguridad proporcionadas por Spring Security, como:
- Strict-Transport-Security;
- X-Frame-Options;
- X-Content-Type-Options.

---

## 9. Persistencia

La información bancaria se almacena en MySQL.

Base de datos:
```text
banco_xyz_core
```

Tablas principales:
```text
cuentas
movimientos
```

Las operaciones realizadas desde los BFF son procesadas por el Core API y persistidas mediante Spring Data JPA.

Ejemplo de flujo:
```text
BFF ATM
   |
   v
Core API
   |
   v
Service
   |
   v
Repository
   |
   v
MySQL
```

---

## 10. Manejo de errores

La solución incorpora manejo de excepciones para evitar respuestas genéricas y entregar información adecuada a cada cliente.

Se consideran situaciones como:
- cuenta inexistente;
- datos inválidos;
- saldo insuficiente;
- recursos no encontrados;
- solicitudes no autorizadas.

Ejemplos:
```text
400 Bad Request
401 Unauthorized
404 Not Found
409 Conflict
```

---

## 11. Observabilidad

Se incorpora Spring Boot Actuator para verificar el estado de los servicios.

Ejemplo:
```text
/actuator/health
```

Cuando el servicio se encuentra operativo se obtiene:
```text
status: UP
```

Esto permite comprobar disponibilidad, readiness, liveness y estado general del servicio.

---

## 12. Modularidad y escalabilidad

La solución se encuentra dividida en cuatro módulos principales:
```text
bank-core-api
bff-web
bff-mobile
bff-atm
```

Cada módulo posee responsabilidades propias.

Esta organización permite:
- modificar un canal sin afectar los demás;
- agregar nuevos BFF en el futuro;
- mantener separada la lógica de presentación de la lógica de datos;
- escalar los servicios de manera independiente;
- reducir acoplamiento.

Además, internamente se utilizan capas como:

```text
controller
service
client
repository
dto
mapper
config
exception
```

---

## 13. Tecnologías utilizadas

- Java
- Spring Boot
- Spring Web
- Spring Security
- OAuth2 Resource Server
- JWT
- Spring Data JPA
- Hibernate
- MySQL
- Maven
- HTTPS / TLS
- Spring Boot Actuator
- Git
- GitHub

---

## 14. Conclusión

La propuesta permite implementar el patrón Backend for Frontend de forma independiente para los tres canales solicitados por Banco XYZ.

Web recibe información completa, Mobile utiliza respuestas más livianas y ATM dispone únicamente de operaciones críticas.

La solución incorpora autenticación y autorización específica por canal, HTTPS, certificados, manejo de errores, persistencia en MySQL y observabilidad.

La arquitectura modular permite además extender o escalar cada canal sin afectar directamente a los demás componentes del sistema.
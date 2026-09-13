# Propuesta Técnica
## Banco XYZ - Backend for Frontend (BFF)
### Exp2 - Semana 5

## 1. Introducción

Para la modernización del sistema Banco XYZ se implementa una arquitectura basada en el patrón **Backend for Frontend (BFF)**, separando la atención de los distintos tipos de clientes en backends especializados.

La solución contempla tres canales:

- Web
- Mobile
- Cajero Automático (ATM)

Cada canal posee necesidades diferentes en cuanto a cantidad de información, operaciones disponibles y seguridad. Por esta razón, se implementa un BFF independiente para cada uno.

---

## 2. Estrategia de implementación

La estrategia seleccionada utiliza un **Core API central** encargado de la lógica de acceso a los datos bancarios y tres BFF independientes que consumen dicho servicio.

La arquitectura general es:

```text
Frontend Web
    |
    v
  BFF Web
controller -> service -> client
                         |
                         v
                      Core API
              controller -> service -> repository
                                      |
                                      v
                                    MySQL


Frontend Mobile
    |
    v
 BFF Mobile
controller -> service -> client
                         |
                         v
                      Core API


Frontend ATM
    |
    v
  BFF ATM
controller -> service -> client
                         |
                         v
                      Core API
```

Esta separación permite adaptar las respuestas según las necesidades de cada frontend sin duplicar la lógica principal de persistencia.

---

## 3. Organización interna de los servicios

La solución mantiene responsabilidades claramente separadas.

### Core API

El módulo `bank-core-api` contiene las capas:

```text
controller
service
repository
entity
dto
mapper
exception
```

Responsabilidades:

- `controller`: recibe y gestiona las solicitudes HTTP;
- `service`: concentra reglas de negocio y coordinación de casos de uso;
- `repository`: se comunica con la persistencia mediante Spring Data JPA;
- `entity`: representa las entidades persistentes;
- `dto`: define objetos utilizados para intercambio de datos;
- `mapper`: transforma entidades y DTO;
- `exception`: centraliza el manejo de errores.

### BFF Web, Mobile y ATM

Cada BFF contiene principalmente:

```text
controller
service
client
dto
config
auth
exception
```

Los BFF **no implementan una capa Repository propia**, debido a que no acceden directamente a la base de datos.

La persistencia está encapsulada en `bank-core-api`.

En los BFF:

- `controller`: recibe las solicitudes del canal;
- `service`: adapta, transforma y coordina la información requerida;
- `client`: se comunica con el Core API;
- `dto`: define las respuestas específicas del canal;
- `auth` y `config`: gestionan autenticación, autorización y seguridad;
- `exception`: gestiona errores propios de la integración.

De esta forma, la función del BFF se mantiene enfocada en adaptar y entregar datos según las necesidades del frontend correspondiente.

---

## 4. Core API

El módulo `bank-core-api` funciona como servicio central de datos y reglas principales del dominio bancario.

Sus responsabilidades incluyen:

- consulta de cuentas;
- consulta de movimientos;
- registro de operaciones;
- actualización de saldos;
- validación de operaciones;
- acceso a MySQL;
- manejo de errores del dominio.

Tecnologías utilizadas:

- Spring Web;
- Spring Data JPA;
- Hibernate;
- MySQL;
- DTO y Mapper;
- manejo global de excepciones.

El servicio se ejecuta en:

```text
http://localhost:8080
```

---

## 5. BFF Web

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

El canal Web entrega información como:

- titular;
- edad;
- tipo de cuenta;
- saldo;
- tasa de interés;
- interés calculado.

---

## 6. BFF Mobile

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

Este enfoque reduce el volumen de datos enviados a dispositivos móviles.

---

## 7. BFF ATM

El BFF ATM está orientado a operaciones necesarias para un cajero automático.

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

Para operaciones inválidas, como un retiro superior al saldo disponible, el sistema responde con códigos HTTP adecuados:

```text
HTTP 409 Conflict
SALDO_INSUFICIENTE
```

---

## 8. Autenticación y autorización por canal

Cada BFF implementa autenticación mediante JWT.

Scopes configurados:

```text
WEB_ACCESS
MOBILE_ACCESS
ATM_ACCESS
```

Cada canal utiliza una clave de firma independiente.

Por esta razón, un token generado por un BFF no puede reutilizarse en otro canal.

Ejemplo:

```text
Token Web -> BFF Web     = permitido
Token Web -> BFF ATM     = rechazado
Token Mobile -> BFF ATM  = rechazado
```

Los endpoints protegidos sin token responden:

```text
HTTP 401 Unauthorized
```

Esto permite mantener una autorización diferenciada para cada frontend.

---

## 9. Seguridad HTTPS

Los tres BFF utilizan HTTPS.

Para el entorno local se utiliza un certificado PKCS12 generado mediante `keytool`.

El certificado se configura mediante variables de entorno y el archivo `.p12` se excluye del repositorio Git.

Puertos seguros:

```text
Web     8081 HTTPS
Mobile  8082 HTTPS
ATM     8083 HTTPS
```

También se utilizan cabeceras de seguridad proporcionadas por Spring Security, entre ellas:

- Strict-Transport-Security;
- X-Frame-Options;
- X-Content-Type-Options.

---

## 10. Persistencia

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

La persistencia se encuentra centralizada en `bank-core-api` mediante Spring Data JPA.

Flujo de una operación ATM:

```text
BFF ATM
   |
   v
Controller
   |
   v
Service
   |
   v
Client
   |
   v
Core API
   |
   v
Controller
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

Esta separación evita que los BFF accedan directamente a la base de datos.

---

## 11. Enfoque sin Spring Batch

La solución de Semana 5 se implementa únicamente con **Spring Boot** y no utiliza Spring Batch.

Los datos necesarios ya se encuentran disponibles en la base de datos, por lo que el proyecto se enfoca en:

- exponer servicios;
- aplicar reglas de negocio;
- adaptar respuestas por canal;
- proteger endpoints;
- persistir operaciones.

Esto mantiene la solución centrada en la arquitectura BFF y en la comunicación entre servicios.

---

## 12. Manejo de errores

La solución incorpora manejo de excepciones para entregar respuestas controladas y coherentes.

Se consideran situaciones como:

- cuenta inexistente;
- datos inválidos;
- saldo insuficiente;
- recursos no encontrados;
- solicitudes no autorizadas.

Códigos HTTP utilizados:

```text
400 Bad Request
401 Unauthorized
404 Not Found
409 Conflict
```

---

## 13. Observabilidad

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

## 14. Modularidad y escalabilidad

La solución se divide en cuatro módulos principales:

```text
bank-core-api
bff-web
bff-mobile
bff-atm
```

Esta organización permite:

- modificar un canal sin afectar directamente a los demás;
- mantener separadas las responsabilidades;
- agregar nuevos BFF en el futuro;
- escalar servicios de forma independiente;
- reducir el acoplamiento;
- centralizar la persistencia y las reglas comunes.

Cada BFF conserva su propia lógica de adaptación y seguridad, mientras que el Core API concentra la comunicación con la base de datos.

---

## 15. Tecnologías utilizadas

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

## 16. Conclusión

La propuesta implementa el patrón Backend for Frontend mediante tres backends independientes para Web, Mobile y ATM.

Cada canal dispone de un `controller`, una capa `service` y una capa `client` encargada de comunicarse con el Core API. Los BFF no acceden directamente a la persistencia, por lo que no requieren una capa Repository propia.

El módulo `bank-core-api` concentra las capas `controller`, `service` y `repository`, además de las entidades y el acceso a MySQL mediante Spring Data JPA.

Web recibe información completa, Mobile utiliza respuestas más livianas y ATM dispone de operaciones críticas y controladas.

La solución incorpora JWT específico por canal, HTTPS, manejo de errores, observabilidad y persistencia, manteniendo una arquitectura modular y coherente con la estrategia BFF seleccionada.

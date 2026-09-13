# Banco XYZ - Backend for Frontend (BFF)

## Exp2 - Semana 5

Proyecto desarrollado para la asignatura **Desarrollo Backend III (PBY2203)**, correspondiente a la actividad de implementación del patrón arquitectónico **Backend for Frontend (BFF)** para Banco XYZ.

La solución separa el acceso al sistema en tres canales independientes:
- Web
- Mobile
- Cajero Automático (ATM)

Cada canal posee su propio backend, endpoints especializados, autenticación JWT, autorización específica y comunicación HTTPS.

---

## Objetivo

Implementar una arquitectura BFF que adapte las respuestas y operaciones según las necesidades de cada tipo de cliente, manteniendo un Core API central para el acceso a los datos bancarios.

---

## Arquitectura del proyecto

```text
Exp2_S5_Pablo_Pilar_Rojas
|
|-- bank-core-api
|-- bff-web
|-- bff-mobile
|-- bff-atm
|-- evidencias
|   `-- Evidencias S5
|-- pom.xml
|-- README.md
|-- PROPUESTA_TECNICA.md
|-- EVIDENCIA_EJECUCION.md
|-- mvnw
`-- mvnw.cmd
```

### Servicios

| Servicio | Puerto | Protocolo |
|---|---:|---|
| Core API | 8080 | HTTP |
| BFF Web | 8081 | HTTPS |
| BFF Mobile | 8082 | HTTPS |
| BFF ATM | 8083 | HTTPS |

El Core API centraliza el acceso a los datos y reglas principales del dominio, mientras que cada BFF adapta las respuestas a las necesidades de su canal.

---

## Core API

El módulo `bank-core-api` utiliza Spring Data JPA y MySQL para gestionar:
- cuentas;
- movimientos;
- consultas;
- retiros;
- actualización de saldos;
- validaciones;
- manejo de errores.

Base de datos utilizada:
```text
banco_xyz_core
```

Tablas principales:
```text
cuentas
movimientos
```

---

## BFF Web

El canal Web entrega información más completa, adecuada para interfaces de navegador.

URL base:
```text
https://localhost:8081
```

Endpoints principales:
```text
POST /auth/token
GET  /api/web/cuentas
GET  /api/web/cuentas/{cuentaId}
GET  /api/web/cuentas/{cuentaId}/movimientos
GET  /api/web/dashboard/{cuentaId}
```

Scope requerido:
```text
WEB_ACCESS
```

---

## BFF Mobile

El canal Mobile entrega respuestas más ligeras y con información esencial.

URL base:
```text
https://localhost:8082
```

Endpoints principales:
```text
POST /auth/token
GET  /api/mobile/cuentas/{cuentaId}
GET  /api/mobile/cuentas/{cuentaId}/movimientos-recientes
GET  /api/mobile/resumen/{cuentaId}
```

Scope requerido:
```text
MOBILE_ACCESS
```

---

## BFF ATM

El canal ATM está orientado a operaciones específicas y críticas.

URL base:
```text
https://localhost:8083
```

Endpoints principales:
```text
POST /auth/token
GET  /api/atm/saldo/{cuentaId}
POST /api/atm/retiros
```

Scope requerido:
```text
ATM_ACCESS
```

El BFF ATM también controla errores como:
```text
HTTP 409 Conflict
SALDO_INSUFICIENTE
```

---

## Seguridad

Los tres BFF utilizan:
- Spring Security;
- JWT;
- OAuth2 Resource Server;
- HTTPS;
- certificado local PKCS12;
- autorización específica por canal.

Scopes configurados:
```text
WEB_ACCESS
MOBILE_ACCESS
ATM_ACCESS
```

Cada BFF utiliza una clave de firma diferente, por lo que los tokens no pueden reutilizarse entre canales.

Comportamiento validado:
```text
Sin token                -> 401 Unauthorized
Token correcto           -> acceso permitido
Token de otro canal      -> 401 Unauthorized
```

---

## HTTPS

Para el entorno local se utiliza un certificado PKCS12 generado mediante `keytool`.

El certificado se guarda localmente en:
```text
certs/banco-xyz-local.p12
```

El archivo `.p12` está excluido mediante `.gitignore` y no se almacena en GitHub.

Ejemplo para generar el certificado:
```powershell
New-Item -ItemType Directory -Force ".\certs"

keytool -genkeypair `
    -alias bancoxyz-local `
    -keyalg RSA `
    -keysize 2048 `
    -storetype PKCS12 `
    -keystore ".\certs\banco-xyz-local.p12" `
    -storepass "TU_PASSWORD_SSL" `
    -keypass "TU_PASSWORD_SSL" `
    -validity 3650 `
    -dname "CN=localhost, OU=Desarrollo, O=Banco XYZ, L=Concepcion, ST=Biobio, C=CL" `
    -ext "SAN=dns:localhost,ip:127.0.0.1"
```

---

## Requisitos

- Java 21 o superior
- Maven Wrapper incluido
- MySQL
- PowerShell
- `keytool` disponible desde el JDK

---

## Variables de entorno

### Core API

```powershell
$env:DB_PASSWORD="TU_PASSWORD_MYSQL"
```

### Web

```powershell
$env:WEB_JWT_SECRET="CLAVE_JWT_WEB_DE_AL_MENOS_32_CARACTERES"
$env:WEB_AUTH_USERNAME="webuser"
$env:WEB_AUTH_PASSWORD="web12345"
```

### Mobile

```powershell
$env:MOBILE_JWT_SECRET="CLAVE_JWT_MOBILE_DE_AL_MENOS_32_CARACTERES"
$env:MOBILE_AUTH_USERNAME="mobileuser"
$env:MOBILE_AUTH_PASSWORD="mobile12345"
```

### ATM

```powershell
$env:ATM_JWT_SECRET="CLAVE_JWT_ATM_DE_AL_MENOS_32_CARACTERES"
$env:ATM_AUTH_USERNAME="atmuser"
$env:ATM_AUTH_PASSWORD="atm12345"
```

### HTTPS

```powershell
$env:SSL_KEYSTORE_PASSWORD="TU_PASSWORD_SSL"
$env:SSL_KEYSTORE_PATH="file:C:/ruta/proyecto/certs/banco-xyz-local.p12"
```

---

## Ejecución

Cada servicio debe ejecutarse en una terminal independiente.

### 1. Core API

```powershell
.\mvnw.cmd -pl bank-core-api spring-boot:run
```

Disponible en:
```text
http://localhost:8080
```

### 2. BFF Web

```powershell
.\mvnw.cmd -pl bff-web spring-boot:run
```

Disponible en:
```text
https://localhost:8081
```

### 3. BFF Mobile

```powershell
.\mvnw.cmd -pl bff-mobile spring-boot:run
```

Disponible en:
```text
https://localhost:8082
```

### 4. BFF ATM

```powershell
.\mvnw.cmd -pl bff-atm spring-boot:run
```

Disponible en:
```text
https://localhost:8083
```

---

## Ejemplo de autenticación Web

```powershell
$body = @{
    username = "webuser"
    password = "web12345"
} | ConvertTo-Json -Compress

$body | curl.exe -k -sS `
    -X POST `
    "https://localhost:8081/auth/token" `
    -H "Content-Type: application/json" `
    --data-binary "@-"
```

La respuesta contiene un token JWT con:
```text
scope: WEB_ACCESS
```

---

## Observabilidad

Los BFF exponen endpoints de Spring Boot Actuator.

Ejemplo:
```text
https://localhost:8081/actuator/health
```

Cuando el servicio se encuentra operativo devuelve:
```text
status: UP
```

---

## Compilación general

Para compilar todos los módulos desde la raíz:

```powershell
.\mvnw.cmd compile
```

El resultado esperado es:
```text
Banco XYZ - BFF Semana 5 .... SUCCESS
Banco XYZ - Core API ........ SUCCESS
Banco XYZ - BFF Web ......... SUCCESS
Banco XYZ - BFF Mobile ...... SUCCESS
Banco XYZ - BFF ATM ......... SUCCESS

BUILD SUCCESS
```

---

## Evidencias

Las capturas de ejecución de Semana 5 se encuentran en:
```text
evidencias/Evidencias S5/
```

El detalle completo está documentado en:
```text
EVIDENCIA_EJECUCION.md
```

Las evidencias incluyen:

- estructura modular;
- ejecución de Core API;
- ejecución HTTPS de los tres BFF;
- JWT por canal;
- endpoints protegidos;
- consulta de saldo ATM;
- retiro exitoso;
- manejo de saldo insuficiente;
- acceso sin token;
- rechazo de tokens de otros canales;
- Actuator;
- compilación general;
- persistencia en MySQL.

---

## Documentación adicional

La propuesta de arquitectura y decisiones técnicas se encuentra en:
```text
PROPUESTA_TECNICA.md
```

---

## Tecnologías utilizadas

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

## Conclusión

La solución implementa el patrón Backend for Frontend mediante tres backends independientes para Web, Mobile y ATM.

Cada BFF adapta las respuestas a las necesidades de su canal y utiliza autenticación JWT, autorización específica y HTTPS.

El Core API centraliza el acceso a la información bancaria y persiste las operaciones en MySQL, manteniendo una arquitectura modular, extensible y escalable.
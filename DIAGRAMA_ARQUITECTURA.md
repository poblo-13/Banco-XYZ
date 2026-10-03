# Diagrama de Arquitectura
## Banco XYZ - Semana 8

Este documento presenta la arquitectura general de la solución **Banco XYZ** correspondiente a la **Semana 8** de la asignatura **Desarrollo Backend III (PBY2203)**.

En esta versión se integra una arquitectura basada en microservicios con los siguientes componentes principales:

- **Authorization Server** para la emisión centralizada de tokens OAuth2;
- **BFF Web, BFF Mobile y BFF ATM** como puertas de entrada separadas por canal;
- **Bank Core API** como núcleo de lógica bancaria;
- **MySQL** como base de datos transaccional;
- **Apache Kafka** como sistema de mensajería orientado a eventos;
- **Audit Service** como consumidor asíncrono;
- **Config Server** para configuración centralizada;
- **Discovery Server (Eureka)** para registro y descubrimiento de servicios;
- **Docker Compose** para levantar toda la solución de forma integrada.

---

## 1. Vista general de la arquitectura

```text
                                      +---------------------------+
                                      |   Authorization Server    |
                                      |           :9000           |
                                      | OAuth2 / client_credentials
                                      +-------------+-------------+
                                                    |
                    +-------------------------------+-------------------------------+
                    |                               |                               |
                    v                               v                               v
             +-------------+                 +-------------+                 +-------------+
             |   BFF Web   |                 | BFF Mobile  |                 |   BFF ATM   |
             |    :8081    |                 |    :8082    |                 |    :8083    |
             | WEB_ACCESS  |                 |MOBILE_ACCESS|                 | ATM_ACCESS  |
             +------+------+                 +------+------+                 +------+------+
                    \                               |                               /
                     \                              |                              /
                      \-----------------------------+-----------------------------/
                                                    |
                                              Resilience4j
                                                    |
                                                    v
                                          +-------------------+
                                          |   Bank Core API   |
                                          |       :8080       |
                                          +---------+---------+
                                                    |
                               +--------------------+--------------------+
                               |                                         |
                               v                                         v
                        +--------------+                         +----------------+
                        |    MySQL     |                         |     Kafka      |
                        |     :3306    |                         |     :9092      |
                        | banco_xyz_   |                         | banco.movimientos
                        |    core      |                         |  3 particiones |
                        +--------------+                         +--------+-------+
                                                                          |
                                                      +-------------------+-------------------+
                                                      |                                       |
                                                      v                                       v
                                           +-------------------+                   +-------------------+
                                           |  Audit Service 1  |                   |  Audit Service 2  |
                                           | banco-auditoria   |                   | banco-auditoria   |
                                           +-------------------+                   +-------------------+

                         +-------------------+                     +--------------------+
                         |   Config Server   |                     | Discovery Server   |
                         |       :8888       |                     |       :8761        |
                         +---------+---------+                     +----------+---------+
                                   \                                           /
                                    \------------- soporte común -------------/
```

---

## 2. Diagrama lógico por responsabilidades

### 2.1 Seguridad

```text
Cliente técnico
     |
     v
Authorization Server
     |
     v
Emite token con scope
     |
     +--> WEB_ACCESS
     +--> MOBILE_ACCESS
     `--> ATM_ACCESS
```

Los tres BFF funcionan como **OAuth2 Resource Server**, por lo tanto validan el token antes de permitir el acceso a sus endpoints.

---

### 2.2 Canales BFF

Cada canal mantiene su propia API, adaptada al contexto de uso:

```text
BFF Web
- Lista cuentas
- Consulta información ampliada

BFF Mobile
- Consulta rápida de cuenta
- Respuesta simplificada

BFF ATM
- Consulta saldo
- Ejecuta retiros
```

Esto permite desacoplar cada experiencia de consumo sin exponer directamente el Core a todos los clientes.

---

### 2.3 Lógica central

El servicio **Bank Core API** concentra:

- gestión de cuentas;
- consulta de movimientos;
- depósitos y retiros;
- persistencia en MySQL;
- publicación de eventos en Kafka;
- registro en Eureka;
- lectura de configuración desde Config Server.

---

### 2.4 Persistencia y eventos

```text
Solicitud ATM/Web/Mobile
        |
        v
Bank Core API
        |
        +--> guarda cambios en MySQL
        |
        `--> publica evento en Kafka
```

El tópico utilizado es:

```text
banco.movimientos
```

Los eventos se publican después del commit de la transacción, lo que evita notificar operaciones que finalmente no fueron confirmadas.

---

### 2.5 Consumo asíncrono

Los consumidores del módulo `audit-service` pertenecen al mismo grupo:

```text
banco-auditoria
```

Esto permite distribuir el consumo de particiones entre dos instancias activas:

```text
audit-service-1
audit-service-2
```

De esta forma se demuestra escalabilidad horizontal del consumidor.

---

## 3. Diagrama de flujo de una operación de retiro

A continuación se muestra el flujo que se validó en la prueba funcional del retiro desde ATM.

```text
1. Se solicita token OAuth2 al Authorization Server
2. El cliente obtiene token con scope ATM_ACCESS
3. Se invoca POST /api/atm/retiros en BFF ATM
4. BFF ATM valida el token
5. BFF ATM llama al Bank Core API
6. Bank Core API valida la cuenta y registra el retiro
7. MySQL actualiza el saldo
8. Después del commit, el Core publica RETIRO_REALIZADO en Kafka
9. Una instancia de Audit Service consume el evento
10. El resultado queda trazado de forma asíncrona
```

Representación resumida:

```text
Authorization Server
        |
        v
     Token
        |
        v
     BFF ATM
        |
        v
  Bank Core API
     /      \
    v        v
 MySQL     Kafka
              |
              v
       Audit Service
```

---

## 4. Arquitectura desplegada con Docker Compose

La solución se ejecuta de manera integrada mediante `docker-compose.yml`.

### Contenedores considerados

```text
authorization-server
config-server
discovery-server
bank-core-api
bff-web
bff-mobile
bff-atm
mysql
kafka
audit-service-1
audit-service-2
```

### Puertos publicados

| Servicio | Puerto |
|---|---:|
| Authorization Server | 9000 |
| Config Server | 8888 |
| Discovery Server | 8761 |
| Bank Core API | 8080 |
| BFF Web | 8081 |
| BFF Mobile | 8082 |
| BFF ATM | 8083 |
| Kafka | 9092 |
| MySQL | 3307 (host) / 3306 (contenedor) |

---

## 5. Relación entre componentes

| Componente | Se conecta con | Propósito |
|---|---|---|
| Authorization Server | BFF Web, BFF Mobile, BFF ATM | Emite tokens OAuth2 |
| BFF Web | Authorization Server, Bank Core API | Canal Web |
| BFF Mobile | Authorization Server, Bank Core API | Canal Mobile |
| BFF ATM | Authorization Server, Bank Core API | Canal ATM |
| Bank Core API | Config Server, Discovery Server, MySQL, Kafka | Lógica de negocio central |
| Audit Service | Kafka | Consumo de eventos bancarios |
| Config Server | Bank Core API | Configuración centralizada |
| Discovery Server | Bank Core API | Registro y descubrimiento |
| MySQL | Bank Core API | Persistencia |
| Kafka | Bank Core API, Audit Service | Mensajería asíncrona |

---

## 6. Tecnologías representadas en la arquitectura

- **Spring Boot**
- **Spring Cloud Config**
- **Netflix Eureka**
- **Spring Security OAuth2 Resource Server**
- **Spring Authorization Server**
- **Resilience4j**
- **Spring Data JPA**
- **MySQL**
- **Apache Kafka**
- **Docker**
- **Docker Compose**

---

## 7. Conclusión

La arquitectura de Semana 8 consolida una solución distribuida donde cada componente cumple un rol específico.

Los BFF permiten separar los canales Web, Mobile y ATM. El Authorization Server centraliza la seguridad con OAuth2. El Core concentra la lógica bancaria y se conecta con MySQL para persistencia y con Kafka para la publicación de eventos. Los consumidores `audit-service` permiten procesar dichos eventos de manera asíncrona. Finalmente, Config Server, Eureka y Docker Compose completan la base operativa de la solución.

En conjunto, esta arquitectura permite demostrar seguridad, resiliencia, configuración centralizada, descubrimiento de servicios, mensajería orientada a eventos y despliegue coordinado de todos los servicios del ecosistema Banco XYZ.

# Banco XYZ - Backend for Frontend (BFF)

## Exp2 - Semana 4 - Grupo14

Este proyecto corresponde a la actividad de la Semana 4 de Desarrollo Backend III.
La idea principal fue continuar con el proyecto del Banco XYZ, pero esta vez aplicando el patrón Backend for Frontend (BFF), creando una respuesta diferente dependiendo del tipo de cliente que se conecta al sistema.

## Objetivo

Implementar una solución BFF para tres tipos de clientes:
- Web
- Mobile
- Cajero Automático (ATM)
Cada uno tiene necesidades distintas, por lo tanto no todos reciben la misma cantidad de información.

## Estrategia utilizada

Se decidió separar el funcionamiento por tipo de cliente.
Las rutas principales son:
- `/api/web/**`
- `/api/mobile/**`
- `/api/atm/**`
La idea es que cada canal tenga una respuesta adaptada a lo que realmente necesita.

## Estructura creada

```
bff/
├── config/
│   └── SecurityConfig.java
├── web/
│   └── WebBffController.java
├── mobile/
│   └── MobileBffController.java
└── atm/
    └── AtmBffController.java
```

## BFF Web

Para el canal Web se entrega información más completa, ya que un navegador puede manejar una interfaz con mayor cantidad de datos.

Endpoint:
```
GET /api/web/dashboard
```

La respuesta incluye:
- transacciones
- intereses
- movimientos anuales

Usuario de prueba:
```
web / web123
```

Prueba:
```
curl.exe -u web:web123 http://localhost:8080/api/web/dashboard
```

## BFF Mobile

Para Mobile se decidió entregar menos información, pensando en una respuesta más rápida y liviana.

Endpoint:
```
GET /api/mobile/resumen
```

La respuesta incluye:
- total de transacciones
- total de cuentas
- cantidad de movimientos
- últimas 3 transacciones

Usuario de prueba:
```
mobile / mobile123
```

Prueba:
```
curl.exe -u mobile:mobile123 http://localhost:8080/api/mobile/resumen
```

## BFF ATM

Para el cajero automático se crearon operaciones más específicas.

Endpoints:
```
GET /api/atm/saldo
POST /api/atm/retiro
```

La consulta de saldo entrega información enfocada solamente en una operación de cajero.

Usuario de prueba:
```
atm / atm123
```

Prueba:
```
curl.exe -u atm:atm123 http://localhost:8080/api/atm/saldo
```

## Seguridad

Se agregó Spring Security para separar el acceso según el tipo de canal.

Se utilizan los siguientes roles:
- ROLE_WEB
- ROLE_MOBILE
- ROLE_ATM

Cada usuario puede acceder solamente a las rutas que le corresponden.

Por ejemplo, si el usuario Web intenta entrar a una ruta ATM:
```
curl.exe -i -u web:web123 http://localhost:8080/api/atm/saldo
```

la aplicación responde con:
```
HTTP/1.1 403 Forbidden
```

Esto permite comprobar que la autorización funciona correctamente.

## Base de datos

Se mantiene la base de datos utilizada en las semanas anteriores:
```
bank_batch_db
```

El proyecto utiliza MySQL junto con Spring Data JPA.

La contraseña de MySQL se configura mediante una variable de entorno:
```
$env:DB_PASSWORD="TU_CONTRASEÑA"
```

## Ejecución

Para levantar el proyecto se utiliza:
```
.\mvnw.cmd spring-boot:run
```

La aplicación queda disponible en:
```
http://localhost:8080
```

## Diferencia entre los BFF

La principal diferencia entre los tres canales es la cantidad y tipo de información que reciben.

### Web

Entrega información completa.

### Mobile

Entrega un resumen con menos datos.

### ATM

Entrega información específica para operaciones de cajero.

De esta forma se aplica el patrón BFF, ya que cada frontend recibe una respuesta pensada para sus propias necesidades.

## Evidencias

Las evidencias de esta semana están guardadas en:
```
evidencias/Evidencias S4/
```

Se incluyen capturas de:
1. BFF Web
2. BFF Mobile
3. BFF ATM
4. Validación de seguridad entre canales

## Conclusión

Con esta actividad se logró aplicar el patrón Backend for Frontend sobre el proyecto Banco XYZ.
Se crearon tres accesos distintos para Web, Mobile y ATM, manteniendo una misma fuente de datos pero entregando respuestas diferentes según el tipo de cliente.
Además, se agregó seguridad por roles para evitar que un usuario pueda acceder a un canal que no le corresponde.
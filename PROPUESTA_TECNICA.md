# Propuesta Técnica - Backend for Frontend (BFF)

## Banco XYZ - Exp2 Semana 4 - Grupo14

## 1. Introducción

Para esta actividad se continuó trabajando sobre el proyecto Banco XYZ desarrollado en las semanas anteriores.
En esta oportunidad el objetivo fue aplicar el patrón Backend for Frontend (BFF), considerando que el sistema puede ser utilizado desde distintos tipos de clientes.

Los canales considerados fueron:
- Web
- Mobile
- Cajero Automático (ATM)
Cada uno de estos clientes necesita información diferente, por lo que no es necesario entregar exactamente los mismos datos a todos.

## 2. Problema identificado

Actualmente el Banco XYZ posee información relacionada con cuentas, transacciones, intereses y movimientos.

Si todos los clientes utilizaran una misma respuesta, algunos recibirían información que no necesitan.

Por ejemplo:
- Una aplicación Web puede mostrar una mayor cantidad de información.
- Una aplicación Mobile necesita respuestas más pequeñas y rápidas.
- Un cajero automático solamente necesita información asociada a operaciones específicas.

Por este motivo se decidió utilizar el patrón BFF.

## 3. Estrategia utilizada

La estrategia seleccionada fue separar la lógica según el tipo de cliente.

Dentro del proyecto se crearon controladores independientes para cada canal:
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

Las rutas utilizadas son:
```
/api/web/**
/api/mobile/**
/api/atm/**
```

De esta forma cada canal puede tener su propia respuesta sin modificar el funcionamiento de los demás.

## 4. BFF Web

El BFF Web fue pensado para una interfaz utilizada desde un navegador.
En este caso se entrega una mayor cantidad de información, ya que una aplicación Web puede tener una interfaz más completa.

Se implementó el endpoint:
```
GET /api/web/dashboard
```

La respuesta contiene información como:
- transacciones
- intereses calculados
- movimientos anuales

La finalidad es entregar en una sola respuesta la información que podría utilizar un dashboard del Banco XYZ.

## 5. BFF Mobile

Para el canal Mobile se buscó reducir la cantidad de información enviada.

Se implementó:
```
GET /api/mobile/resumen
```

En vez de entregar todos los datos existentes, la respuesta contiene:
- total de transacciones
- total de cuentas
- cantidad de movimientos
- últimas 3 transacciones

Esto permite tener una respuesta más pequeña y adecuada para un dispositivo móvil.
De esta forma se evita enviar información que probablemente no será mostrada inmediatamente en la aplicación.

## 6. BFF ATM

Para el cajero automático se decidió utilizar operaciones más específicas.

Se implementaron los endpoints:
```
GET /api/atm/saldo
POST /api/atm/retiro
```

La consulta de saldo entrega información específica de una cuenta para representar una operación de cajero automático.
El endpoint de retiro permite recibir una solicitud de retiro y entregar una respuesta asociada a la operación.
En esta versión de la actividad, el retiro se encuentra implementado como una demostración de la operación y no modifica de forma definitiva el saldo almacenado en la base de datos.

## 7. Seguridad

También se agregó seguridad utilizando Spring Security.

Se crearon usuarios de prueba para cada canal:
```
Web:
web / web123

Mobile:
mobile / mobile123

ATM:
atm / atm123
```

Cada usuario posee un rol diferente:
- ROLE_WEB
- ROLE_MOBILE
- ROLE_ATM

Las rutas están protegidas según estos roles.

Por ejemplo, un usuario Web puede utilizar:
```
/api/web/**
```

pero no debería poder acceder a:
```
/api/atm/**
```

Durante las pruebas se utilizó el usuario Web para intentar ingresar al endpoint ATM y el sistema respondió:
```
HTTP 403 Forbidden
```

Esto permite comprobar que la autorización entre canales está funcionando.

## 8. Base de datos

Se mantiene la base de datos utilizada anteriormente en el proyecto Banco XYZ:
```
bank_batch_db
```

El acceso a los datos se realiza utilizando:
- MySQL
- Spring Data JPA
- Repositorios JPA

Los BFF utilizan los datos existentes para construir respuestas diferentes dependiendo del canal solicitado.

## 9. Funcionamiento general

La solución puede representarse de la siguiente forma:
```
              Banco XYZ
                  |
               MySQL
                  |
            Spring Data JPA
                  |
        -----------------------
        |          |          |
      WEB       MOBILE       ATM
       BFF        BFF         BFF
```

Aunque los tres canales se encuentran dentro del mismo proyecto Spring Boot para esta actividad, se organizaron mediante paquetes, controladores, rutas y permisos diferentes.
Esto permite demostrar la separación de responsabilidades que propone el patrón Backend for Frontend.

## 10. Diferencias entre los canales

### Web

Entrega mayor cantidad de información y está pensado para una interfaz más completa.

### Mobile

Entrega una respuesta resumida para disminuir la cantidad de datos enviados.

### ATM

Entrega respuestas específicas para operaciones propias de un cajero automático.

## 11. Ventajas de la solución

La utilización del patrón BFF permite:
- adaptar las respuestas según cada cliente
- evitar información innecesaria
- separar responsabilidades
- tener rutas específicas por canal
- aplicar seguridad diferente para cada cliente
- facilitar futuras modificaciones

Por ejemplo, se podría modificar el BFF Mobile sin cambiar directamente la respuesta utilizada por Web o ATM.

## 12. Evidencias realizadas

Para comprobar el funcionamiento se realizaron pruebas de los tres canales.

Se generaron evidencias para:
1. BFF Web.
2. BFF Mobile.
3. BFF ATM.
4. Seguridad entre canales mediante respuesta 403 Forbidden.

Las capturas se encuentran en:
```
evidencias/Evidencias S4/
```

## 13. Conclusión

La propuesta implementada permite aplicar el patrón Backend for Frontend al proyecto Banco XYZ.
Se crearon respuestas diferentes para Web, Mobile y ATM según las necesidades de cada cliente.
Web recibe una mayor cantidad de información, Mobile utiliza una respuesta más resumida y ATM trabaja con operaciones específicas.
Además, se agregó seguridad utilizando roles para evitar que un usuario pueda acceder a canales que no le corresponden.
Con esto se logra mantener una misma fuente de datos, pero adaptar la información entregada dependiendo del frontend que realiza la solicitud.
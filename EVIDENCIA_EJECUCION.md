# Evidencia de Ejecución - Backend for Frontend (BFF)

## Banco XYZ - Exp2 Semana 4 - Grupo14

Este documento muestra las pruebas realizadas para comprobar el funcionamiento de los BFF implementados para los canales Web, Mobile y ATM.
También se incluye una prueba de seguridad para verificar que cada usuario solamente pueda acceder al canal que le corresponde.

## 1. Inicio de la aplicación

La aplicación se ejecutó utilizando el siguiente comando:
```
.\mvnw.cmd spring-boot:run
```

Durante la ejecución se comprobó que Spring Boot inició correctamente y que la conexión con MySQL se realizó sin problemas.

La aplicación quedó disponible en:
```
http://localhost:8080
```
En consola se pudo observar el inicio correcto del servidor Tomcat en el puerto 8080.

## 2. Prueba BFF Web

Para probar el canal Web se utilizó el siguiente comando:
```
curl.exe -u web:web123 http://localhost:8080/api/web/dashboard
```

La respuesta obtenida fue correcta y mostró información completa para este tipo de cliente.

Ejemplo de respuesta:
```
{
  "canal": "WEB",
  "descripcion": "Respuesta completa optimizada para navegador",
  "transacciones": [...],
  "intereses": [...],
  "movimientosAnuales": [...]
}
```

La respuesta Web incluye mayor cantidad de información, como:
- transacciones
- intereses calculados
- movimientos anuales

Esto permite demostrar que este BFF está pensado para una interfaz que puede manejar una mayor cantidad de datos.

## 3. Prueba BFF Mobile

Para probar el canal Mobile se utilizó:
```
curl.exe -u mobile:mobile123 http://localhost:8080/api/mobile/resumen
```

La respuesta obtenida fue:
```
{
  "canal": "MOBILE",
  "descripcion": "Respuesta ligera con información esencial",
  "totalTransacciones": 8,
  "totalCuentas": 7,
  "movimientosAnuales": 8,
  "transaccionesRecientes": [
    {
      "id": 1,
      "fecha": "2024-01-01",
      "monto": 1000.00,
      "tipo": "debito"
    },
    {
      "id": 2,
      "fecha": "2024-01-02",
      "monto": 1500.00,
      "tipo": "credito"
    },
    {
      "id": 3,
      "fecha": "2024-01-03",
      "monto": 200.00,
      "tipo": "debito"
    }
  ]
}
```

En este caso se puede observar que la respuesta es más pequeña.

No se envía toda la información disponible en la base de datos, sino solamente:
- cantidad de transacciones
- cantidad de cuentas
- cantidad de movimientos
- últimas 3 transacciones

Esto demuestra la adaptación de la información para un dispositivo móvil.

## 4. Prueba BFF ATM

Para probar el canal ATM se ejecutó:
```
curl.exe -u atm:atm123 http://localhost:8080/api/atm/saldo
```

La respuesta obtenida fue:
```
{
  "canal": "ATM",
  "operacion": "CONSULTA_SALDO",
  "descripcion": "Respuesta específica para operación de cajero",
  "cuenta": {
    "cuentaId": 101,
    "nombre": "John Doe",
    "saldoInicial": 5000.00,
    "edad": 30,
    "tipo": "ahorro",
    "tasaInteres": 0.0050,
    "interesCalculado": 25.00,
    "saldoFinal": 5025.00
  }
}
```

En este canal la respuesta se encuentra enfocada directamente en una operación de cajero automático.
En este caso se probó la consulta de saldo.

El BFF ATM también posee el endpoint:
```
POST /api/atm/retiro
```
Este endpoint fue creado para representar una solicitud de retiro.

## 5. Prueba de seguridad

También se realizó una prueba para comprobar que un usuario no pueda acceder a un canal que no le corresponde.
Se intentó acceder al BFF ATM utilizando el usuario Web:
```
curl.exe -i -u web:web123 http://localhost:8080/api/atm/saldo
```

La respuesta del sistema fue:
```
HTTP/1.1 403
```

También se obtuvo:
```
{
  "status": 403,
  "error": "Forbidden",
  "path": "/api/atm/saldo"
}
```

Esto demuestra que Spring Security está aplicando correctamente la autorización por roles.

El usuario Web puede acceder a:
```
/api/web/**
```

pero no puede acceder a:
```
/api/atm/**
```

De la misma forma, los usuarios Mobile y ATM poseen sus propios permisos.

## 6. Usuarios utilizados para las pruebas

Los usuarios configurados para esta actividad fueron:

### Web
```
Usuario: web
Contraseña: web123
Rol: ROLE_WEB
```

### Mobile
```
Usuario: mobile
Contraseña: mobile123
Rol: ROLE_MOBILE
```

### ATM
```
Usuario: atm
Contraseña: atm123
Rol: ROLE_ATM
```

Estos usuarios fueron creados únicamente para realizar las pruebas de la actividad.

## 7. Resultado de las pruebas

Las pruebas realizadas permitieron comprobar lo siguiente:
- La aplicación inicia correctamente.
- La conexión con MySQL funciona correctamente.
- El BFF Web entrega información completa.
- El BFF Mobile entrega una respuesta más ligera.
- El BFF ATM entrega información específica para operaciones de cajero.
- Spring Security limita el acceso según el rol de cada usuario.
- Un usuario de otro canal recibe una respuesta 403 Forbidden.

## 8. Evidencias gráficas

Las capturas realizadas durante las pruebas se encuentran en:
```
evidencias/Evidencias S4/
```

Las evidencias consideradas son:
1. Ejecución del BFF Web.
2. Ejecución del BFF Mobile.
3. Ejecución del BFF ATM.
4. Prueba de seguridad con respuesta 403 Forbidden.

## 9. Conclusión

Las pruebas realizadas muestran que los tres BFF implementados funcionan de forma diferente según el tipo de cliente.
El canal Web recibe mayor cantidad de información, Mobile recibe una respuesta más reducida y ATM recibe información específica para operaciones de cajero.
Además, la prueba de seguridad permitió comprobar que cada usuario puede acceder solamente a las rutas correspondientes a su propio canal.
Con esto se puede comprobar el funcionamiento básico de la solución BFF implementada para Banco XYZ.
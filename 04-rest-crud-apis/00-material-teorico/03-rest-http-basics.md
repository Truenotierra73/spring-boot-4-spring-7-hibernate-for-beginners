# REST sobre HTTP: Fundamentos

## 1. Uso de HTTP/HTTPS sobre REST

REST no exige un protocolo de transporte específico, pero en la práctica **casi todas las APIs REST se implementan sobre HTTP/HTTPS**, ya que este protocolo ya provee de forma nativa gran parte de lo que REST necesita: métodos estandarizados (verbos), códigos de estado, cabeceras (headers), y un modelo de comunicación cliente-servidor sin estado.

### HTTP (Hypertext Transfer Protocol)

- Protocolo de comunicación de la capa de aplicación, utilizado para la transferencia de datos en la web.
- Es un protocolo **sin estado (stateless)**: cada petición es independiente, el servidor no recuerda peticiones anteriores del mismo cliente (salvo que se use algún mecanismo adicional como cookies, tokens, etc.).
- Los datos viajan **en texto plano**, sin cifrado.

### HTTPS (HTTP Secure)

- Es la versión **segura** de HTTP: la misma comunicación HTTP, pero cifrada mediante **TLS/SSL** (Transport Layer Security / Secure Sockets Layer).
- Garantiza:
  - **Confidencialidad**: los datos viajan cifrados, no se pueden leer si son interceptados.
  - **Integridad**: los datos no pueden ser alterados sin ser detectados.
  - **Autenticidad**: el cliente puede verificar que se está comunicando con el servidor real (mediante certificados digitales).
- En producción, **toda API REST debería exponerse únicamente sobre HTTPS**, especialmente si maneja datos sensibles (credenciales, datos personales, información de pago, etc.).

### ¿Por qué HTTP es tan adecuado para REST?

| Necesidad de REST | Solución que aporta HTTP |
|--------------------|---------------------------|
| Identificar recursos | URIs (URLs) |
| Operar sobre recursos | Métodos HTTP (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`) |
| Comunicar el resultado de una operación | Códigos de estado HTTP (`200`, `404`, `500`, etc.) |
| Especificar el formato de los datos | Cabeceras (`Content-Type`, `Accept`) |
| Comunicación sin estado | El propio protocolo HTTP es *stateless* |

---

## 2. Métodos HTTP para operaciones CRUD

Las operaciones **CRUD** (Create, Read, Update, Delete / Crear, Leer, Actualizar, Eliminar) se mapean de forma natural a los verbos (métodos) HTTP:

| Método HTTP | Operación CRUD | Definición | Caso de uso típico | ¿Idempotente? | ¿Seguro? |
|-------------|-----------------|------------|----------------------|:---:|:---:|
| **GET** | Read (Leer) | Solicita la representación de un recurso o colección de recursos, sin modificar el estado del servidor. | Obtener el listado de empleados o un empleado en particular. | Sí | Sí |
| **POST** | Create (Crear) | Envía datos al servidor para crear un nuevo recurso (subordinado a una colección). | Crear un nuevo empleado. | No | No |
| **PUT** | Update (Actualizar completo) | Reemplaza **por completo** un recurso existente con los datos enviados. Si el recurso no existe, algunas APIs lo crean. | Actualizar todos los campos de un empleado existente. | Sí | No |
| **PATCH** | Update (Actualizar parcial) | Modifica **parcialmente** un recurso, enviando solo los campos que cambian. | Actualizar únicamente el puesto de un empleado. | No (en general) | No |
| **DELETE** | Delete (Eliminar) | Elimina el recurso identificado por la URI. | Eliminar un empleado por su id. | Sí | No |
| **QUERY** | Read (Leer, con filtros complejos) | Método HTTP relativamente nuevo (propuesto como estándar) que permite realizar búsquedas/consultas complejas enviando los criterios de filtrado en el **cuerpo (body)** de la solicitud, en lugar de codificarlos en la URL como parámetros de query string. | Buscar empleados aplicando múltiples filtros combinados (ej: rango de fechas, varios departamentos, texto libre) que resultarían en una URL demasiado larga o compleja con `GET`. | Sí | Sí |

### El método QUERY

- `QUERY` surge para cubrir una limitación práctica de `GET`: al no permitir (o no ser recomendable) el envío de un *body*, las búsquedas complejas deben codificarse como parámetros en la URL (`?campo1=valor1&campo2=valor2...`), lo cual tiene límites de longitud y resulta poco práctico para filtros anidados o muy numerosos.
- `QUERY` combina lo mejor de ambos mundos: es **seguro** e **idempotente** como `GET` (no modifica el estado del servidor), pero **permite enviar un cuerpo** en la solicitud (similar a `POST`), típicamente en formato JSON, con los criterios de búsqueda.
- Al momento de escribir este material, `QUERY` es un método en proceso de estandarización (borrador de IETF) y su soporte varía según el servidor, framework y cliente HTTP utilizado; no todos los entornos lo soportan todavía de forma nativa.

```
QUERY  /api/empleados          -> Buscar empleados según criterios enviados en el body

Body de la solicitud:
{
  "departamento": ["Ventas", "Sistemas"],
  "fechaIngresoDesde": "2023-01-01",
  "salarioMinimo": 3000
}
```

### Conceptos clave

- **Idempotente**: una operación es idempotente si, al ejecutarla múltiples veces con los mismos datos, el resultado final es el mismo que ejecutarla una sola vez (no genera efectos secundarios adicionales). Por ejemplo, `DELETE /empleados/5` ejecutado varias veces siempre deja al empleado 5 eliminado (aunque las siguientes veces respondan `404 Not Found`).
- **Seguro (Safe)**: una operación es segura si **no modifica el estado del servidor** (es de solo lectura). `GET`, `HEAD` y `QUERY` son seguros.

### Ejemplo de uso sobre un recurso `empleados`

```
GET    /api/empleados          -> Obtener todos los empleados
GET    /api/empleados/5        -> Obtener el empleado con id 5
POST   /api/empleados          -> Crear un nuevo empleado (body con los datos)
PUT    /api/empleados/5        -> Reemplazar completamente el empleado 5
PATCH  /api/empleados/5        -> Actualizar parcialmente el empleado 5
DELETE /api/empleados/5        -> Eliminar el empleado 5
```

---

## 3. Arquitectura Cliente/Servidor

### Definición

La **arquitectura cliente-servidor** es un modelo de diseño de aplicaciones distribuidas donde las responsabilidades se dividen en dos roles claramente separados:

- **Cliente**: quien **solicita** los recursos o servicios (navegador web, aplicación móvil, otro backend, etc.). Se encarga generalmente de la interfaz de usuario y la experiencia del usuario.
- **Servidor**: quien **atiende las solicitudes**, procesa la lógica de negocio, accede a los datos (bases de datos) y devuelve una respuesta.

Esta separación permite que cliente y servidor **evolucionen de forma independiente**, siempre que se respete el contrato (la interfaz/API) entre ambos. Es uno de los principios fundamentales de REST.

### Diagrama

```
   ┌────────────┐        HTTP Request        ┌────────────┐
   │            │  ───────────────────────►  │            │
   │  CLIENTE   │                             │  SERVIDOR  │
   │ (Browser,  │                             │ (Spring    │
   │  App, etc) │  ◄───────────────────────   │  Boot API) │
   │            │        HTTP Response        │            │
   └────────────┘                             └────────────┘
```

### Solicitudes y respuestas

La comunicación siempre sigue el mismo patrón de **request-response** (solicitud-respuesta):

1. El **cliente** envía una **solicitud (request)** al servidor, indicando qué recurso quiere y qué operación desea realizar.
2. El **servidor** procesa la solicitud y devuelve una **respuesta (response)**, indicando el resultado de la operación.

### Anatomía de un HTTP Request Message (Mensaje de solicitud)

Un mensaje de solicitud HTTP se compone de las siguientes partes:

```
POST /api/empleados HTTP/1.1                    ← Línea de solicitud (Request Line)
Host: localhost:8080                             ← Cabeceras (Headers)
Content-Type: application/json
Accept: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

{                                                 ← Cuerpo (Body)
  "nombre": "Juan Pérez",
  "puesto": "Desarrollador"
}
```

1. **Línea de solicitud (Request Line)**: contiene tres elementos:
   - **Método HTTP**: `POST` (verbo de la operación a realizar).
   - **Ruta/URI del recurso**: `/api/empleados`.
   - **Versión del protocolo HTTP**: `HTTP/1.1` (u otra versión, como `HTTP/2`).
2. **Cabeceras (Headers)**: pares clave-valor con metadatos sobre la solicitud. Algunas comunes:
   - `Host`: dominio/servidor al que se dirige la solicitud.
   - `Content-Type`: formato del cuerpo enviado (ej: `application/json`).
   - `Accept`: formato(s) de respuesta que el cliente está dispuesto a aceptar.
   - `Authorization`: credenciales de autenticación (ej: token Bearer).
3. **Línea en blanco**: separa las cabeceras del cuerpo.
4. **Cuerpo (Body)**: contiene los datos enviados (opcional; por ejemplo, `GET` normalmente no lleva body, mientras que `POST`, `PUT` y `PATCH` sí).

### Anatomía de un HTTP Response Message (Mensaje de respuesta)

```
HTTP/1.1 201 Created                             ← Línea de estado (Status Line)
Content-Type: application/json                   ← Cabeceras (Headers)
Location: /api/empleados/6

{                                                 ← Cuerpo (Body)
  "id": 6,
  "nombre": "Juan Pérez",
  "puesto": "Desarrollador"
}
```

1. **Línea de estado (Status Line)**: contiene:
   - **Versión del protocolo**: `HTTP/1.1`.
   - **Código de estado**: `201`.
   - **Frase de estado (Reason Phrase)**: `Created`.
2. **Cabeceras (Headers)**: metadatos de la respuesta, por ejemplo:
   - `Content-Type`: formato del cuerpo devuelto.
   - `Location`: URI del recurso recién creado (típico en respuestas `201 Created`).
3. **Línea en blanco**: separa las cabeceras del cuerpo.
4. **Cuerpo (Body)**: contiene los datos de la respuesta (puede estar vacío, por ejemplo en un `204 No Content`).

---

## 4. Tipos de códigos HTTP (clasificaciones)

Los **códigos de estado HTTP** son números de 3 dígitos que el servidor incluye en la respuesta para indicar el resultado de la solicitud. Se agrupan en **5 categorías**, según su primer dígito:

| Rango | Categoría | Significado | Ejemplos |
|-------|-----------|--------------|----------|
| **1xx** | Informational (Informativos) | La solicitud fue recibida y se está procesando; son respuestas provisionales. | `100 Continue`, `101 Switching Protocols` |
| **2xx** | Success (Éxito) | La solicitud fue recibida, entendida y procesada correctamente. | `200 OK`, `201 Created`, `204 No Content` |
| **3xx** | Redirection (Redirección) | Se requiere una acción adicional (normalmente, seguir otra URI) para completar la solicitud. | `301 Moved Permanently`, `304 Not Modified` |
| **4xx** | Client Error (Error del cliente) | La solicitud contiene un error atribuible al cliente (datos inválidos, falta de autenticación, recurso inexistente, etc.). | `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found` |
| **5xx** | Server Error (Error del servidor) | El servidor falló al procesar una solicitud válida (error interno, no implementado, etc.). | `500 Internal Server Error`, `503 Service Unavailable` |

### Códigos más utilizados en APIs REST

| Código | Nombre | Uso típico |
|--------|--------|-------------|
| `200 OK` | OK | Operación exitosa (ej: `GET`, `PUT`, `PATCH` exitosos). |
| `201 Created` | Creado | Recurso creado exitosamente (típico en `POST`). |
| `204 No Content` | Sin contenido | Operación exitosa sin cuerpo de respuesta (típico en `DELETE`). |
| `400 Bad Request` | Solicitud incorrecta | Los datos enviados por el cliente son inválidos o malformados. |
| `401 Unauthorized` | No autorizado | El cliente no está autenticado (falta o es inválido el token/credenciales). |
| `403 Forbidden` | Prohibido | El cliente está autenticado, pero no tiene permisos para esa operación. |
| `404 Not Found` | No encontrado | El recurso solicitado no existe. |
| `409 Conflict` | Conflicto | La solicitud entra en conflicto con el estado actual del recurso (ej: datos duplicados). |
| `500 Internal Server Error` | Error interno del servidor | Error inesperado en el servidor al procesar la solicitud. |

---

## 5. ¿Qué es MIME y para qué sirve?

**MIME** (Multipurpose Internet Mail Extensions) es un estándar que define el **formato/tipo de un contenido** que se transmite, originalmente creado para especificar el tipo de los archivos adjuntos en correos electrónicos, y luego adoptado por HTTP para indicar el tipo de contenido de las solicitudes y respuestas.

### Media Type / MIME Type / Content-Type

En el contexto de HTTP, un **MIME Type** (también llamado **Media Type**) se especifica en la cabecera `Content-Type` (para indicar qué tipo de dato se está enviando) y en la cabecera `Accept` (para indicar qué tipo de dato se espera recibir).

Un MIME Type tiene el formato: `tipo/subtipo`.

### ¿Para qué sirve?

- Permite que el **receptor** (cliente o servidor) sepa **cómo interpretar** el contenido del cuerpo del mensaje, sin necesidad de "adivinar" el formato.
- Es fundamental en REST, ya que un mismo recurso podría representarse en distintos formatos (JSON, XML, texto plano, imagen, etc.), y el MIME Type indica exactamente cuál se está utilizando.

### Ejemplos comunes de MIME Types en APIs REST

| MIME Type | Descripción |
|-----------|--------------|
| `application/json` | Contenido en formato JSON (el más usado en APIs REST modernas). |
| `application/xml` | Contenido en formato XML. |
| `text/plain` | Texto plano, sin formato especial. |
| `text/html` | Contenido HTML. |
| `application/x-www-form-urlencoded` | Datos de formularios codificados como pares clave-valor. |
| `multipart/form-data` | Datos de formularios que incluyen archivos (uploads). |
| `application/octet-stream` | Datos binarios genéricos (ej: descarga de un archivo). |
| `image/png`, `image/jpeg` | Imágenes en formato PNG o JPEG. |

### Ejemplo de uso en una solicitud HTTP

```
POST /api/empleados HTTP/1.1
Content-Type: application/json      ← "Te estoy enviando datos en formato JSON"
Accept: application/json             ← "Espero recibir la respuesta en formato JSON"

{
  "nombre": "Juan Pérez",
  "puesto": "Desarrollador"
}
```

En Spring Boot, cuando se utiliza `@RestController` junto con Jackson, el `Content-Type: application/json` es el valor por defecto tanto para las solicitudes que recibe como para las respuestas que produce, lo cual es coherente con el enfoque de **APIs REST basadas en JSON** que se utilizará en este módulo (`04-rest-crud-apis`).

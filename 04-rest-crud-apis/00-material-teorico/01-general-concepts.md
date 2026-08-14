# Conceptos Generales: API, REST y Servicios Web

## 1. ¿Qué es una API?

**API** (Application Programming Interface / Interfaz de Programación de Aplicaciones) es un conjunto de reglas, protocolos y definiciones que permite que dos aplicaciones de software se comuniquen entre sí.

Una API define:
- **Qué operaciones** se pueden solicitar (endpoints, métodos, funciones).
- **Qué datos** se deben enviar (parámetros, cuerpo de la petición).
- **Qué respuesta** se puede esperar (formato, estructura, códigos de estado).

### Analogía

Piensa en una API como el **menú de un restaurante**: el cliente (aplicación consumidora) no necesita saber cómo se cocina el plato (implementación interna), solo necesita saber qué puede pedir (endpoints disponibles) y qué recibirá a cambio (respuesta).

### Tipos de API según su alcance

| Tipo | Descripción |
|------|-------------|
| **API de librería/framework** | Conjunto de clases y métodos expuestos por una librería (ej: la API de Java Collections). |
| **API de sistema operativo** | Permite a las aplicaciones interactuar con el SO (ej: API de Windows). |
| **API Web** | Se expone a través de HTTP/HTTPS para que otras aplicaciones (locales o remotas) consuman sus funcionalidades. |

En el contexto de desarrollo web y este curso, cuando hablamos de "API" normalmente nos referimos a una **API Web**.

---

## 2. ¿Qué es REST?

**REST** (Representational State Transfer / Transferencia de Estado Representacional) es un **estilo arquitectónico** (no un protocolo ni un estándar) definido por Roy Fielding en el año 2000, pensado para el diseño de servicios en red, especialmente sobre el protocolo HTTP.

REST no es una tecnología en sí misma, sino un **conjunto de principios y restricciones** que, si se siguen, permiten construir sistemas distribuidos escalables, simples y desacoplados.

### ¿Para qué se utiliza?

Se utiliza para diseñar **APIs web** que permiten a distintos sistemas (frontend, apps móviles, otros backends) comunicarse entre sí de forma estandarizada, utilizando principalmente el protocolo **HTTP**.

### ¿Cómo funciona?

REST se basa en el concepto de **recursos**. Cada recurso (por ejemplo, un usuario, un producto, un pedido) se identifica mediante una **URI** (Uniform Resource Identifier), y se manipula utilizando los **métodos HTTP estándar**:

| Método HTTP | Operación CRUD | Descripción |
|-------------|----------------|-------------|
| `GET`       | Read (Leer)    | Obtiene un recurso o una colección de recursos. |
| `POST`      | Create (Crear) | Crea un nuevo recurso. |
| `PUT`       | Update (Actualizar) | Actualiza un recurso completo. |
| `PATCH`     | Update (Actualizar parcial) | Actualiza parcialmente un recurso. |
| `DELETE`    | Delete (Eliminar) | Elimina un recurso. |

Ejemplo de URIs orientadas a recursos:

```
GET    /api/empleados          -> Obtener todos los empleados
GET    /api/empleados/5        -> Obtener el empleado con id 5
POST   /api/empleados          -> Crear un nuevo empleado
PUT    /api/empleados/5        -> Actualizar el empleado con id 5
DELETE /api/empleados/5        -> Eliminar el empleado con id 5
```

### Los 6 principios (restricciones) de REST

1. **Cliente-Servidor**: separación de responsabilidades entre el cliente (UI/consumo) y el servidor (datos/lógica).
2. **Sin estado (Stateless)**: cada petición del cliente debe contener toda la información necesaria; el servidor no guarda estado de sesión entre peticiones.
3. **Cacheable**: las respuestas deben indicar si pueden ser cacheadas o no, para mejorar el rendimiento.
4. **Interfaz uniforme**: forma consistente de interactuar con los recursos (URIs, métodos HTTP, representaciones estándar).
5. **Sistema en capas (Layered System)**: el cliente no necesita saber si se comunica directamente con el servidor final o con intermediarios (proxies, balanceadores de carga, gateways).
6. **Código bajo demanda (opcional)**: el servidor puede enviar código ejecutable al cliente (ej: JavaScript), aunque es la única restricción opcional.

### Formatos utilizados en REST

REST no impone un formato de datos específico, pero los más comunes son:

- **JSON** (JavaScript Object Notation): el más utilizado actualmente por su simplicidad y ligereza.
- **XML** (eXtensible Markup Language): usado históricamente, más verboso.
- Otros menos comunes: **YAML**, **texto plano**, **HTML**.

Ejemplo de representación en JSON de un recurso:

```json
{
  "id": 5,
  "nombre": "Juan Pérez",
  "puesto": "Desarrollador"
}
```

---

## 3. API REST vs API RESTful

Este es un punto de confusión muy común. La diferencia es principalmente de **grado de cumplimiento** de los principios REST.

### API REST

- Es una API que se **inspira** en el estilo arquitectónico REST y utiliza HTTP con sus verbos y URIs orientadas a recursos.
- **No necesariamente cumple con todas las restricciones** de REST (por ejemplo, puede no ser completamente *stateless*, o no implementar HATEOAS).
- En la práctica, el término se usa de forma coloquial para referirse a cualquier API que use HTTP + JSON con una estructura de recursos, sin ser estrictamente purista.

### API RESTful

- Es una API que **cumple fielmente con todos los principios REST** definidos por Fielding, incluyendo restricciones más estrictas como:
  - **Stateless** de forma completa.
  - **HATEOAS** (Hypermedia As The Engine Of Application State): las respuestas incluyen enlaces (hipervínculos) que indican las acciones/recursos relacionados disponibles.

Ejemplo de respuesta con HATEOAS (característica de una API verdaderamente RESTful):

```json
{
  "id": 5,
  "nombre": "Juan Pérez",
  "puesto": "Desarrollador",
  "_links": {
    "self": { "href": "/api/empleados/5" },
    "empleados": { "href": "/api/empleados" },
    "departamento": { "href": "/api/departamentos/2" }
  }
}
```

### Principales diferencias

| Característica | API REST | API RESTful |
|-----------------|----------|-------------|
| Cumplimiento de principios | Parcial / flexible | Total / estricto |
| HATEOAS | No suele implementarse | Se implementa |
| Uso del término | Coloquial / genérico | Técnico / preciso |
| Rigurosidad arquitectónica | Baja-Media | Alta |
| Frecuencia en el mundo real | Muy común | Poco común (la mayoría de las "APIs REST" no son 100% RESTful) |

### Casos de uso

- **API REST**: la mayoría de las APIs modernas (Twitter/X, GitHub, Spotify, etc.) se autodenominan "REST" pero no implementan HATEOAS. Es adecuada para la mayoría de aplicaciones web y móviles donde se prioriza la simplicidad y velocidad de desarrollo. **Este será el enfoque que utilizaremos en este curso.**
- **API RESTful**: útil en sistemas donde se requiere **auto-descubribilidad** (el cliente puede navegar la API dinámicamente sin conocer previamente todas las rutas), como en ciertos ecosistemas empresariales grandes o APIs públicas muy documentadas mediante hipermedia.

---

## 4. Servicios REST vs Servicios RESTful vs Servicios Web REST vs Servicios Web RESTful

Estos términos suelen usarse indistintamente en la práctica, pero técnicamente tienen matices.

### Servicio REST

Es un **servicio** (backend) que expone su funcionalidad siguiendo (parcial o totalmente) el estilo arquitectónico REST. El término "servicio" es más genérico y no implica obligatoriamente que se use el protocolo HTTP (aunque en la práctica casi siempre lo es).

### Servicio RESTful

Es un servicio que **cumple totalmente** con las restricciones de REST (incluyendo HATEOAS, stateless estricto, etc.). Es la contraparte "pura" del servicio REST.

### Servicio Web REST

Es un **Servicio Web** (Web Service), es decir, una aplicación que se expone y es consumida a través de la **red usando protocolos web (HTTP/HTTPS)**, diseñado según el estilo REST. Aquí se enfatiza explícitamente el uso de la **web** como medio de transporte.

> Nota: existen "Servicios Web" que no son REST, como los servicios **SOAP** (Simple Object Access Protocol), que usan XML y un protocolo mucho más rígido y formal (con WSDL, contratos estrictos, etc.).

### Servicio Web RESTful

Es un **Servicio Web** que además **cumple totalmente** con los principios REST (incluyendo HATEOAS). Es el término más específico y estricto de los cuatro.

### Tabla comparativa

| Término | ¿Implica uso de HTTP/Web? | ¿Cumple 100% con REST? | Precisión del término |
|---------|:---------------------------:|:------------------------:|------------------------|
| Servicio REST | Generalmente sí (en la práctica) | No necesariamente | Media |
| Servicio RESTful | Generalmente sí (en la práctica) | Sí | Alta |
| Servicio Web REST | Sí (explícito) | No necesariamente | Alta |
| Servicio Web RESTful | Sí (explícito) | Sí | Muy alta |

### Diferencias clave

1. **"REST" vs "RESTful"**: el primero es una aproximación/inspiración al estilo; el segundo es el cumplimiento estricto de todas sus restricciones.
2. **"Servicio" vs "Servicio Web"**: "Servicio" es un término más amplio (podría, en teoría, no depender de la web); "Servicio Web" deja explícito que la comunicación se realiza sobre protocolos web (HTTP/HTTPS), diferenciándolo de otros tipos de servicios (RPC binarios, colas de mensajes, etc.).

### Casos de uso

- **Servicio REST / Servicio Web REST**: es el enfoque más común en el desarrollo moderno con Spring Boot. Se construyen controladores (`@RestController`) que exponen endpoints HTTP con JSON, sin necesidad de implementar HATEOAS. **Es el enfoque que se utilizará en los ejercicios prácticos de este módulo (`04-rest-crud-apis`).**
- **Servicio RESTful / Servicio Web RESTful**: se utiliza cuando se requiere una API totalmente autodescriptiva y navegable, típicamente en sistemas con Spring HATEOAS, donde el cliente descubre las acciones disponibles a través de los enlaces devueltos en cada respuesta, reduciendo el acoplamiento entre cliente y servidor.

---

## 5. Resumen visual

```
API  ─────────────────────────────────────────────► Interfaz de comunicación entre software
  │
  └── API Web ────────────────────────────────────► Expuesta vía HTTP/HTTPS
        │
        ├── SOAP ──────────────────────────────────► Protocolo rígido basado en XML
        │
        └── REST (estilo arquitectónico) ──────────► Basado en recursos + verbos HTTP
              │
              ├── API / Servicio REST ──────────────► Cumplimiento parcial de REST (lo más común)
              │
              └── API / Servicio RESTful ───────────► Cumplimiento total de REST (incluye HATEOAS)
```

## 6. Conclusión práctica para el curso

A lo largo de este módulo (`04-rest-crud-apis`) construiremos **APIs / Servicios Web REST** utilizando Spring Boot:

- Expondremos recursos mediante `@RestController`.
- Utilizaremos los verbos HTTP (`GET`, `POST`, `PUT`, `DELETE`) para las operaciones CRUD.
- Usaremos **JSON** como formato de intercambio de datos.
- No implementaremos HATEOAS, por lo que, en rigor, estaremos construyendo APIs **REST**, no **RESTful** — tal como ocurre en la gran mayoría de las aplicaciones reales del mercado.

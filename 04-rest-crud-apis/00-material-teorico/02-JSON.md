# JSON (JavaScript Object Notation)

## 1. ¿Qué es JSON?

**JSON** (JavaScript Object Notation / Notación de Objetos de JavaScript) es un **formato ligero de intercambio de datos**, basado en texto plano, fácil de leer y escribir para los humanos, y fácil de generar e interpretar (parsear) para las máquinas.

A pesar de su nombre (derivado de la sintaxis de objetos de JavaScript), **JSON es independiente del lenguaje de programación**: es un formato estándar (definido en [RFC 8259](https://datatracker.ietf.org/doc/html/rfc8259) y en el estándar ECMA-404) que puede ser generado y consumido por prácticamente cualquier lenguaje moderno (Java, Python, JavaScript, C#, PHP, etc.).

Ejemplo básico de un objeto JSON:

```json
{
  "id": 1,
  "nombre": "Juan Pérez",
  "activo": true,
  "puesto": "Desarrollador"
}
```

### ¿Por qué se usa tanto en APIs REST?

- Es **liviano** (menos verboso que XML).
- Es **legible** por humanos.
- Se **mapea de forma natural** a estructuras de datos comunes en la mayoría de los lenguajes (objetos, mapas, listas, arreglos).
- Tiene **soporte nativo** en JavaScript y librerías maduras en casi todos los lenguajes (en Java, por ejemplo, Jackson, Gson, etc.).

---

## 2. ¿Quién puede utilizarlo?

Al ser un formato de texto estándar y no depender de ningún lenguaje o plataforma en particular, **JSON puede ser utilizado por cualquier sistema, lenguaje o tecnología** capaz de leer y escribir texto, entre ellos:

- **Lenguajes de programación**: Java, JavaScript/TypeScript, Python, C#, PHP, Ruby, Go, Kotlin, etc.
- **Frameworks web y APIs**: Spring Boot, Express (Node.js), Django, Laravel, .NET, entre otros.
- **Bases de datos**: muchas bases de datos relacionales (MariaDB, PostgreSQL, MySQL) y NoSQL (MongoDB) soportan tipos de datos JSON de forma nativa.
- **Archivos de configuración**: `package.json`, `composer.json`, configuraciones de herramientas, etc.
- **Aplicaciones cliente**: navegadores web, aplicaciones móviles (Android/iOS), aplicaciones de escritorio.

En resumen, cualquier sistema que necesite **intercambiar información de forma estructurada** a través de una red (o incluso almacenarla localmente) puede utilizar JSON, sin importar en qué tecnología esté implementado el emisor o el receptor.

En el contexto de Spring Boot, JSON es el formato por defecto que utiliza Jackson (la librería incluida en `spring-boot-starter-web`) para **serializar** (convertir objetos Java a JSON) y **deserializar** (convertir JSON a objetos Java) los datos que viajan entre el cliente y el servidor.

---

## 3. Definiciones dentro del JSON

Un documento JSON está formado, principalmente, por dos estructuras:

### 3.1. Objeto

Un **objeto** es una colección desordenada de pares **propiedad: valor** (también llamados **clave: valor**), delimitada por llaves `{ }`. Cada par está separado por comas.

```json
{
  "propiedad1": "valor1",
  "propiedad2": "valor2"
}
```

### 3.2. Propiedad (o clave / *key*)

Es el **nombre o identificador** de un dato dentro de un objeto. En JSON, las propiedades **siempre deben ser cadenas de texto (strings)**, escritas entre comillas dobles `"..."`.

```json
"nombre": "Juan Pérez"
```

Aquí, `"nombre"` es la **propiedad** (clave).

### 3.3. Valor

Es el **dato asociado** a una propiedad. A diferencia de la propiedad (que siempre es un string), el valor puede ser de **distintos tipos** (ver sección siguiente).

```json
"nombre": "Juan Pérez"
```

Aquí, `"Juan Pérez"` es el **valor** asociado a la propiedad `"nombre"`.

### 3.4. Array (arreglo)

Un **array** es una colección **ordenada** de valores, delimitada por corchetes `[ ]`, separados por comas. Los valores de un array pueden ser de cualquier tipo (incluso mezclados, aunque no es una buena práctica).

```json
{
  "empleados": ["Juan", "María", "Pedro"]
}
```

---

## 4. Tipos de valores que admite JSON

JSON admite **seis tipos de datos** para los valores:

| Tipo | Descripción | Ejemplo |
|------|-------------|---------|
| **String** (cadena de texto) | Texto entre comillas dobles. | `"nombre": "Juan Pérez"` |
| **Number** (número) | Números enteros o decimales, positivos o negativos (no distingue entre `int`, `long`, `double`, etc. como Java). | `"edad": 30`, `"precio": 19.99` |
| **Boolean** (booleano) | Solo puede ser `true` o `false`. | `"activo": true` |
| **Object** (objeto) | Un objeto JSON anidado (par propiedad-valor entre llaves). | `"direccion": { "ciudad": "Córdoba", "cp": "5000" }` |
| **Array** (arreglo) | Una colección ordenada de valores entre corchetes. | `"habilidades": ["Java", "Spring", "SQL"]` |
| **null** | Representa la ausencia de valor. | `"telefono": null` |

### Ejemplo combinando todos los tipos

```json
{
  "id": 1,
  "nombre": "Juan Pérez",
  "salario": 4500.50,
  "activo": true,
  "telefono": null,
  "habilidades": ["Java", "Spring Boot", "SQL"],
  "direccion": {
    "ciudad": "Córdoba",
    "codigoPostal": "5000"
  }
}
```

En este ejemplo:
- `"id"` → valor de tipo **Number**.
- `"nombre"` → valor de tipo **String**.
- `"salario"` → valor de tipo **Number** (decimal).
- `"activo"` → valor de tipo **Boolean**.
- `"telefono"` → valor de tipo **null**.
- `"habilidades"` → valor de tipo **Array** (de strings).
- `"direccion"` → valor de tipo **Object** (anidado).

### Importante: JSON NO admite

- Comentarios (`// ...` o `/* ... */`).
- Comillas simples (`'...'`) para strings; siempre deben ser comillas dobles (`"..."`).
- Comas finales (*trailing commas*) después del último elemento de un objeto o array.
- Claves sin comillas (a diferencia de un objeto literal de JavaScript).

```json
// Esto es INVÁLIDO en JSON:
{
  'nombre': 'Juan', // comentario no permitido
  edad: 30,          // clave sin comillas
  "activo": true,    // coma final no permitida
}
```

---

## 5. Relación con Java y Spring Boot

Cuando trabajemos con **Spring Boot** en este módulo (`04-rest-crud-apis`), Jackson se encargará automáticamente de la conversión entre objetos Java y JSON:

| JSON | Java (tipo equivalente aproximado) |
|------|-------------------------------------|
| String | `String` |
| Number (entero) | `int`, `Integer`, `long`, `Long` |
| Number (decimal) | `double`, `Double`, `BigDecimal` |
| Boolean | `boolean`, `Boolean` |
| Object | Clase Java (POJO / DTO / Entidad) |
| Array | `List`, `Set`, arreglo (`[]`) |
| null | `null` |

Por ejemplo, un objeto Java como el siguiente:

```java
public class Empleado {
    private int id;
    private String nombre;
    private boolean activo;
}
```

Se serializa automáticamente (gracias a Jackson) a este JSON:

```json
{
  "id": 1,
  "nombre": "Juan Pérez",
  "activo": true
}
```

Este mecanismo de **serialización/deserialización automática** es una de las razones principales por las que JSON es el formato estándar en las APIs REST desarrolladas con Spring Boot.

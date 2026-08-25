# JSON Data Binding en Java con Jackson

## 1. ¿Qué es Java JSON Data Binding?

**JSON Data Binding** es el proceso de **convertir automáticamente** datos entre el formato **JSON** (texto) y **objetos Java** (POJOs), y viceversa. En lugar de parsear manualmente el JSON campo por campo, una librería se encarga de "mapear" (bind) los atributos del JSON con los atributos de una clase Java, y viceversa.

### Otros nombres con los que se identifica

Este mismo concepto recibe distintos nombres según el contexto o la tecnología:

| Término | Significado |
|---|---|
| **Data Binding** | Nombre genérico: "enlazar" datos entre dos representaciones (JSON <-> Java). |
| **Marshalling** | Convertir un objeto Java a un formato de intercambio (JSON, XML, etc.). |
| **Unmarshalling** | Convertir un formato de intercambio (JSON, XML, etc.) a un objeto Java. |
| **Serialización** | Convertir un objeto Java a JSON (o a otro formato como bytes). |
| **Deserialización** | Convertir JSON (u otro formato) a un objeto Java. |

### ¿Cuándo es Serialización y cuándo es Deserialización?

- **Serialización (Marshalling)**: cuando se parte de un **objeto Java (POJO)** y se lo convierte a **JSON**.
  - Ejemplo: el backend responde a un cliente REST devolviendo un objeto `Student` convertido a JSON.
- **Deserialización (Unmarshalling)**: cuando se parte de **JSON** y se lo convierte a un **objeto Java (POJO)**.
  - Ejemplo: el backend recibe un JSON en el cuerpo (body) de una petición HTTP `POST` y lo convierte en un objeto `Student`.

```
Java Object (POJO)  --- Serialización / Marshalling --->  JSON
JSON                 --- Deserialización / Unmarshalling --->  Java Object (POJO)
```

## 2. ¿Qué es una clase POJO en Java?

**POJO** significa **Plain Old Java Object** ("simple objeto Java antiguo"). Es una clase Java **sin restricciones especiales**, es decir:

- No extiende ninguna clase específica del framework.
- No implementa ninguna interfaz especial obligatoria.
- No depende de ninguna anotación obligatoria de un framework.
- Generalmente contiene:
  - Atributos privados (`private`).
  - Constructor vacío (sin argumentos).
  - Métodos `getter` y `setter` para cada atributo.
  - Opcionalmente, `equals()`, `hashCode()` y `toString()`.

### Ejemplo de un POJO

```java
public class Student {

    private String firstName;
    private String lastName;

    public Student() {
    }

    public Student(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
```

Este objeto simple es precisamente el que Jackson necesita para poder convertirlo a/desde JSON: solo necesita sus getters y setters (o su constructor y atributos, según la configuración).

## 3. ¿Qué es Jackson Data Binding?

**Jackson** es la librería de referencia en el ecosistema Java (y utilizada internamente por Spring Boot) para realizar **JSON Data Binding**. Permite convertir automáticamente entre JSON y objetos Java (POJOs) sin que el desarrollador tenga que escribir manualmente el código de parseo.

### ¿Para qué se utiliza?

- Convertir el cuerpo JSON de una petición HTTP en un objeto Java (deserialización).
- Convertir un objeto Java en JSON para enviarlo como respuesta HTTP (serialización).
- Leer/escribir archivos JSON, configuraciones, mensajes de colas, etc.

### ¿Cuándo se utiliza?

- Al desarrollar **REST APIs**, donde los datos se intercambian en formato JSON entre cliente y servidor.
- Al necesitar persistir o transportar información estructurada en JSON de forma sencilla, tipada y mantenible.

### ¿Cómo se utiliza?

Se puede usar Jackson de forma **manual** (standalone), mediante la clase `ObjectMapper`, o de forma **automática** dentro de Spring Boot (se explica en la sección 4).

#### Uso manual con `ObjectMapper`

```java
ObjectMapper objectMapper = new ObjectMapper();
```

## 4. ¿Cómo funciona Jackson "detrás de escenas"? (JSON <-> POJO)

Jackson utiliza principalmente **reflexión** (`java.lang.reflect`) sobre la clase Java y, por convención, sobre los nombres de los **getters/setters**, para saber cómo mapear cada propiedad JSON con cada atributo del POJO (aunque también soporta acceso directo a campos y anotaciones como `@JsonProperty` para personalizar el nombre).

### 4.1. Conversión de JSON a POJO (Deserialización)

Dado el siguiente JSON:

```json
{
  "firstName": "Mario",
  "lastName": "Rossi"
}
```

Y la clase POJO `Student` (ver ejemplo anterior), Jackson realiza el siguiente proceso internamente:

1. Lee el JSON y lo interpreta como un conjunto de pares clave-valor.
2. Para cada clave (por ejemplo, `firstName`), busca en la clase `Student` un método `setFirstName(String)` (siguiendo la convención `set` + Nombre de la propiedad con la primera letra en mayúscula).
3. Invoca ese método `setter` pasándole el valor correspondiente del JSON.
4. Repite el proceso para cada propiedad del JSON.
5. Devuelve la instancia de `Student` ya completamente poblada.

Ejemplo de código usando `ObjectMapper`:

```java
String json = "{\"firstName\":\"Mario\",\"lastName\":\"Rossi\"}";

ObjectMapper objectMapper = new ObjectMapper();
Student student = objectMapper.readValue(json, Student.class);

System.out.println(student.getFirstName()); // Mario
System.out.println(student.getLastName());  // Rossi
```

### 4.2. Conversión de POJO a JSON (Serialización)

Dado el siguiente objeto Java:

```java
Student student = new Student("Mario", "Rossi");
```

Jackson realiza el siguiente proceso internamente:

1. Analiza la clase `Student` mediante reflexión, buscando todos los métodos `getter` públicos (`getFirstName()`, `getLastName()`, etc.).
2. Por cada `getter` encontrado, deduce el nombre de la propiedad JSON quitando el prefijo `get` y convirtiendo la primera letra a minúscula (`getFirstName` -> `firstName`).
3. Invoca cada `getter` para obtener el valor del atributo.
4. Construye el JSON resultante como un conjunto de pares clave-valor con esos nombres y valores.

Ejemplo de código usando `ObjectMapper`:

```java
Student student = new Student("Mario", "Rossi");

ObjectMapper objectMapper = new ObjectMapper();
String json = objectMapper.writeValueAsString(student);

System.out.println(json);
// {"firstName":"Mario","lastName":"Rossi"}
```

## 5. ¿Cómo funciona Jackson en un proyecto Spring Boot con `@RestController`?

Cuando se utiliza `spring-boot-starter-web`, Spring Boot incluye **Jackson** automáticamente como dependencia, y lo configura de forma predeterminada como el conversor de mensajes HTTP para JSON (`MappingJackson2HttpMessageConverter`).

Gracias a esto, en un `@RestController`, la conversión JSON <-> POJO ocurre de manera **transparente**, sin que el desarrollador tenga que invocar manualmente `ObjectMapper`.

### 5.1. Deserialización automática: `@RequestBody`

Cuando un método de un `@RestController` recibe un parámetro anotado con `@RequestBody`, Spring Boot:

1. Intercepta la petición HTTP entrante.
2. Detecta que el `Content-Type` es `application/json`.
3. Delega en Jackson (`ObjectMapper` interno) la conversión del cuerpo (body) JSON al tipo del parámetro Java indicado.
4. Inyecta el objeto Java ya construido como argumento del método.

```java
@RestController
@RequestMapping("/api")
public class StudentRestController {

    @PostMapping("/students")
    public Student addStudent(@RequestBody Student student) {
        // "student" ya es un objeto Java, Jackson lo convirtió desde el JSON del body
        return student;
    }
}
```

### 5.2. Serialización automática: valor de retorno

Cuando el método anotado con `@GetMapping`, `@PostMapping`, etc. de un `@RestController` **retorna un objeto** (o una lista, o un `Map`), Spring Boot:

1. Toma el objeto devuelto por el método.
2. Detecta, según el header `Accept` de la petición (normalmente `application/json`), que debe responder en JSON.
3. Delega en Jackson la conversión del objeto Java a JSON.
4. Escribe ese JSON como cuerpo de la respuesta HTTP, con `Content-Type: application/json`.

```java
@RestController
@RequestMapping("/api")
public class StudentRestController {

    @GetMapping("/students/{id}")
    public Student getStudent(@PathVariable int id) {
        Student student = new Student("Mario", "Rossi");
        // Jackson convierte automáticamente este POJO a JSON en la respuesta
        return student;
    }
}
```

### 5.3. Resumen del flujo completo

```
Cliente (JSON) --> @RequestBody --> Jackson (Deserialización) --> POJO
POJO --> return del método --> Jackson (Serialización) --> JSON --> Cliente
```

Todo este proceso es posible gracias a que `@RestController` combina `@Controller` + `@ResponseBody`, indicándole a Spring que el valor de retorno del método debe escribirse directamente en el cuerpo de la respuesta HTTP (en lugar de resolverse como una vista), y es justamente en ese paso donde interviene Jackson para hacer la conversión a JSON.

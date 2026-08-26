# Path Variables (Rutas Variables) en Spring Boot

## 1. ¿Qué son las Path Variables?

Las **Path Variables** (rutas variables) son **segmentos dinámicos** dentro de la URL de un endpoint REST, cuyo valor real se recibe como parte de la ruta de la petición HTTP, en lugar de recibirse en el cuerpo (body) o como parámetro de consulta (`query param`).

Se definen en el `mapping` del endpoint usando llaves `{ }`, y luego se capturan en el método del controlador mediante la anotación `@PathVariable`.

```
GET /api/students/5
              ^
        path variable (id = 5)
```

En este ejemplo, `5` no es un parámetro fijo del endpoint, sino un **valor variable** que identifica a un recurso específico (el estudiante con `id = 5`).

## 2. ¿Cómo se utilizan?

### 2.1. Definición en el `@RequestMapping` / `@GetMapping`

El segmento variable se declara entre llaves `{ }` dentro de la ruta:

```java
@RestController
@RequestMapping("/api")
public class StudentRestController {

    @GetMapping("/students/{studentId}")
    public Student getStudent(@PathVariable int studentId) {
        // studentId contiene el valor recibido en la URL
        return new Student(studentId, "Mario", "Rossi");
    }
}
```

### 2.2. Captura del valor con `@PathVariable`

- Spring Boot toma el valor del segmento de la URL (`{studentId}`) y lo inyecta automáticamente en el parámetro del método anotado con `@PathVariable`.
- Por defecto, el **nombre del parámetro** del método debe coincidir con el **nombre del placeholder** de la ruta (`studentId` <-> `studentId`).
- Spring Boot realiza automáticamente la conversión de tipo (`String` de la URL -> `int`, `Long`, `String`, etc., según el tipo declarado en el parámetro).

## 3. ¿Cuándo utilizarlas?

Las path variables se utilizan cuando el valor forma parte de la **identidad del recurso** dentro de la URL, siguiendo las convenciones REST. Casos típicos:

- **Obtener un recurso específico por su identificador**: `GET /api/students/{id}`
- **Actualizar un recurso específico**: `PUT /api/students/{id}`
- **Eliminar un recurso específico**: `DELETE /api/students/{id}`
- **Navegar jerarquías de recursos**: `GET /api/courses/{courseId}/students/{studentId}`

Se diferencian de los `query params` (`?nombre=valor`), que se usan típicamente para **filtros, búsquedas u opciones opcionales**, y no para identificar de forma directa un recurso dentro de la ruta.

## 4. Ejemplos

### 4.1. Un solo path variable

```java
@GetMapping("/students/{studentId}")
public Student getStudent(@PathVariable int studentId) {
    return studentService.findById(studentId);
}
```

Petición: `GET /api/students/3`

### 4.2. Múltiples path variables

```java
@GetMapping("/courses/{courseId}/students/{studentId}")
public Student getStudentFromCourse(
        @PathVariable int courseId,
        @PathVariable int studentId) {
    return studentService.findByCourseAndId(courseId, studentId);
}
```

Petición: `GET /api/courses/10/students/3`

### 4.3. Path variable de tipo `String`

```java
@GetMapping("/students/{lastName}")
public List<Student> getStudentsByLastName(@PathVariable String lastName) {
    return studentService.findByLastName(lastName);
}
```

Petición: `GET /api/students/Rossi`

## 5. ¿Existen diferentes formas de configurar los Path Variables?

Sí, existen varias formas de configurarlos según la necesidad:

### 5.1. Nombre del parámetro igual al del placeholder (forma implícita)

Si el nombre del parámetro del método coincide exactamente con el nombre del placeholder de la ruta, no es necesario indicar nada más:

```java
@GetMapping("/students/{studentId}")
public Student getStudent(@PathVariable int studentId) {
    // "studentId" coincide con "{studentId}"
    ...
}
```

### 5.2. Nombre del parámetro distinto al del placeholder (forma explícita)

Si se desea (o necesita) que el nombre del parámetro Java sea distinto al del placeholder de la URL, se debe indicar explícitamente la relación usando el atributo `value` (o `name`) de `@PathVariable`:

```java
@GetMapping("/students/{studentId}")
public Student getStudent(@PathVariable("studentId") int id) {
    // "id" es el nombre del parámetro Java
    // "studentId" es el nombre del placeholder en la URL
    ...
}
```

### 5.3. Múltiples path variables en el mismo mapping

Se pueden declarar varios placeholders en la misma ruta, cada uno capturado con su propio `@PathVariable`:

```java
@GetMapping("/courses/{courseId}/students/{studentId}")
public Student getStudent(
        @PathVariable int courseId,
        @PathVariable int studentId) {
    ...
}
```

### 5.4. Path variable opcional

Desde Spring, `@PathVariable` admite el atributo `required`, permitiendo declarar el parámetro como opcional (aunque, en la práctica, es poco común y suele resolverse mejor con dos `mappings` distintos o con `query params`):

```java
@GetMapping({"/students", "/students/{studentId}"})
public Object getStudent(@PathVariable(required = false) Integer studentId) {
    if (studentId == null) {
        return studentService.findAll();
    }
    return studentService.findById(studentId);
}
```

### 5.5. Capturar todos los path variables en un `Map`

Cuando existen múltiples placeholders y no se desea declarar un parámetro por cada uno, se pueden capturar todos juntos en un `Map<String, String>`:

```java
@GetMapping("/courses/{courseId}/students/{studentId}")
public String getStudent(@PathVariable Map<String, String> pathVarsMap) {
    String courseId = pathVarsMap.get("courseId");
    String studentId = pathVarsMap.get("studentId");
    ...
}
```

## 6. Resumen

```
URL:      /api/students/{studentId}
                              ^
                       placeholder (path variable)

Método:   getStudent(@PathVariable int studentId)
                                          ^
                                   parámetro capturado
```

- Las **path variables** permiten construir URLs semánticas y alineadas con las convenciones REST, identificando recursos directamente en la ruta.
- Se configuran con `{ }` en el `mapping` (`@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, etc.) y se capturan con `@PathVariable` en el parámetro del método.
- Se pueden usar de forma implícita (mismo nombre), explícita (`@PathVariable("nombre")`), múltiples en la misma ruta, opcionales, o agrupadas en un `Map`.

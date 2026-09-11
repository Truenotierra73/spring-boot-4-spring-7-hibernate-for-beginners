# Exception Handling (Manejo de Excepciones) en Spring REST

## 1. ¿Qué es el Exception Handling?

El **Exception Handling** (manejo de excepciones) es el mecanismo que ofrece Spring REST para **capturar, procesar y transformar las excepciones** que ocurren durante la ejecución de un endpoint en una **respuesta HTTP controlada y consistente**, en lugar de dejar que el error se propague sin control hacia el cliente.

Cuando un cliente REST realiza una petición, y en el servidor ocurre un error (por ejemplo, un recurso que no existe, o un dato inválido), Spring REST necesita **devolver una respuesta clara** con:

- Un **código de estado HTTP** apropiado (`404`, `400`, `500`, etc.).
- Un **mensaje de error** entendible, generalmente en formato JSON.
- Información adicional útil para el consumidor de la API (timestamp, detalle del error, etc.).

Sin un manejo de excepciones adecuado, Spring Boot devuelve por defecto una respuesta de error genérica (poco informativa y con detalles internos del servidor), lo cual no es una buena práctica en una API REST.

## 2. ¿Para qué se utiliza?

El manejo de excepciones en Spring REST se utiliza para:

- **Centralizar la lógica de manejo de errores**, evitando repetir bloques `try/catch` en cada método del controlador.
- **Estandarizar el formato de las respuestas de error** en toda la API (mismo formato JSON para todos los errores).
- **Ocultar detalles internos** de la aplicación (stack traces, nombres de clases internas, etc.) que no deberían exponerse al cliente.
- **Mapear excepciones de negocio a códigos de estado HTTP correctos** (por ejemplo, una excepción `StudentNotFoundException` debe traducirse a un `404 Not Found`).
- **Mejorar la experiencia del consumidor de la API**, entregando mensajes de error claros y consistentes.

## 3. ¿Cuándo se utiliza? (Casos de uso)

El manejo de excepciones se aplica típicamente en los siguientes casos:

- **Recurso no encontrado**: se solicita un recurso por `id` (path variable) que no existe en la base de datos. Ejemplo: `GET /api/students/999` cuando el estudiante `999` no existe → `404 Not Found`.
- **Datos de entrada inválidos**: el cliente envía un `path variable`, `query param` o `body` con un formato incorrecto (por ejemplo, un `id` no numérico). Ejemplo: `GET /api/students/abc` → `400 Bad Request`.
- **Errores de validación**: el `body` de una petición `POST`/`PUT` no cumple las reglas de validación definidas (campos obligatorios, formatos, rangos, etc.).
- **Errores inesperados del servidor**: cualquier excepción no controlada explícitamente (`NullPointerException`, errores de conexión a base de datos, etc.), que debe capturarse para no exponer información sensible → `500 Internal Server Error`.
- **Reglas de negocio incumplidas**: por ejemplo, intentar eliminar un recurso que está siendo utilizado por otro, o exceder un límite permitido.

## 4. Pasos a seguir para implementar Exception Handling

Para implementar un manejo de excepciones adecuado en Spring REST, se siguen generalmente estos pasos:

### 4.1. Crear una clase personalizada de respuesta de error

Se define un **POJO** que representa la estructura de la respuesta de error que se enviará al cliente (generalmente serializada a JSON):

```java
public class StudentErrorResponse {

    private int status;
    private String message;
    private long timeStamp;

    public StudentErrorResponse() {
    }

    public StudentErrorResponse(int status, String message, long timeStamp) {
        this.status = status;
        this.message = message;
        this.timeStamp = timeStamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
}
```

### 4.2. Crear una clase de excepción personalizada

Se crea una excepción propia (generalmente extendiendo `RuntimeException`) que represente el error de negocio específico:

```java
public class StudentNotFoundException extends RuntimeException {

    public StudentNotFoundException(String message) {
        super(message);
    }
}
```

> **Buena práctica**: extender de `RuntimeException` (excepción *unchecked*) evita la necesidad de declarar `throws` en cada método del controlador o del servicio.

### 4.3. Lanzar la excepción personalizada

Dentro del controlador (o de la capa de servicio), se lanza la excepción cuando se detecta la condición de error:

```java
@RestController
@RequestMapping("/api")
public class StudentRestController {

    private List<Student> students = List.of(
            new Student(1, "Mario", "Rossi"),
            new Student(2, "Luigi", "Verdi")
    );

    @GetMapping("/students/{studentId}")
    public Student getStudent(@PathVariable int studentId) {

        if (studentId < 0 || studentId >= students.size()) {
            throw new StudentNotFoundException("El estudiante con id: " + studentId + " no fue encontrado");
        }

        return students.get(studentId);
    }
}
```

### 4.4. Agregar un método manejador de excepciones con `@ExceptionHandler`

Se agrega un método anotado con `@ExceptionHandler` que captura la excepción lanzada y construye la respuesta HTTP apropiada, utilizando `ResponseEntity`:

```java
@RestController
@RequestMapping("/api")
public class StudentRestController {

    // ... métodos del controlador ...

    @ExceptionHandler
    public ResponseEntity<StudentErrorResponse> handleException(StudentNotFoundException exc) {

        StudentErrorResponse error = new StudentErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                exc.getMessage(),
                System.currentTimeMillis()
        );

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<StudentErrorResponse> handleException(Exception exc) {

        StudentErrorResponse error = new StudentErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                exc.getMessage(),
                System.currentTimeMillis()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
```

- El primer manejador captura específicamente `StudentNotFoundException` y responde con `404 Not Found`.
- El segundo manejador captura cualquier otra excepción genérica (`Exception`) como red de seguridad, respondiendo con `400 Bad Request` (o `500 Internal Server Error`, según el criterio de diseño).
- Spring REST invoca automáticamente el método manejador cuyo tipo de excepción coincida (o sea superclase) con la excepción lanzada, priorizando siempre la coincidencia más específica.

## 5. Ejemplo completo (buenas prácticas)

Para mantener el código limpio y organizado, es una **buena práctica** ubicar la lógica de manejo de excepciones en un lugar centralizado, en lugar de duplicarla en cada controlador. Existen dos enfoques principales:

### 5.1. Manejo de excepciones local al controlador

Los métodos `@ExceptionHandler` se definen dentro del mismo controlador, aplicándose únicamente a ese controlador:

```java
@RestController
@RequestMapping("/api")
public class StudentRestController {

    @GetMapping("/students/{studentId}")
    public Student getStudent(@PathVariable int studentId) {
        if (studentId < 0 || studentId >= students.size()) {
            throw new StudentNotFoundException("El estudiante con id: " + studentId + " no fue encontrado");
        }
        return students.get(studentId);
    }

    @ExceptionHandler
    public ResponseEntity<StudentErrorResponse> handleException(StudentNotFoundException exc) {
        StudentErrorResponse error = new StudentErrorResponse(
                HttpStatus.NOT_FOUND.value(), exc.getMessage(), System.currentTimeMillis());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
}
```

### 5.2. Manejo de excepciones global con `@ControllerAdvice`

Cuando se necesita **reutilizar el manejo de excepciones en múltiples controladores**, se recomienda extraer los métodos `@ExceptionHandler` a una clase separada anotada con `@ControllerAdvice` (o `@RestControllerAdvice`), la cual actúa de forma centralizada para toda la aplicación:

```java
@RestControllerAdvice
public class StudentRestExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<StudentErrorResponse> handleException(StudentNotFoundException exc) {

        StudentErrorResponse error = new StudentErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                exc.getMessage(),
                System.currentTimeMillis()
        );

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<StudentErrorResponse> handleException(Exception exc) {

        StudentErrorResponse error = new StudentErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                exc.getMessage(),
                System.currentTimeMillis()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
```

> `@RestControllerAdvice` es la combinación de `@ControllerAdvice` + `@ResponseBody`, y permite aplicar el manejo de excepciones a **todos los controladores** de la aplicación de forma centralizada, evitando duplicar código.

## 6. Resumen

```
Cliente                Controller                     ExceptionHandler
   |                        |                                 |
   |--- GET /students/99 -->|                                 |
   |                        |-- throw StudentNotFoundException|
   |                        |-------------------------------->|
   |                        |                                 |-- construye StudentErrorResponse
   |<----------------- 404 Not Found + JSON -------------------|
```

- El **Exception Handling** permite capturar excepciones y transformarlas en respuestas HTTP consistentes y controladas.
- Se utiliza para **centralizar y estandarizar** el manejo de errores, **ocultar detalles internos** y **mapear excepciones de negocio a códigos de estado HTTP** correctos.
- Los pasos clave son: crear una **clase de respuesta de error**, crear una **excepción personalizada**, **lanzar** dicha excepción cuando corresponda, y **capturarla** con un método anotado con `@ExceptionHandler`.
- Como buena práctica, se recomienda centralizar el manejo de excepciones en una clase anotada con `@RestControllerAdvice`, reutilizable por todos los controladores de la aplicación.

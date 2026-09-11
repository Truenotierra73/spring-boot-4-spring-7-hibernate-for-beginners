# Global Exception Handling (Manejo Global de Excepciones) en Spring REST

## 1. ¿Qué es el Global Exception Handling?

El **Global Exception Handling** (Manejo Global de Excepciones) es un patrón arquitectónico y mecanismo de Spring REST que permite interceptar, capturar y procesar de forma **centralizada** las excepciones lanzadas por cualquier controlador (`@RestController`) o capa subyacente (servicios, repositorios) de la aplicación, transformándolas en respuestas HTTP estructuradas y consistentes para los clientes.

En lugar de definir bloques `try-catch` repetitivos o métodos `@ExceptionHandler` dispersos en cada controlador, Spring MVC provee el mecanismo de **Cross-Cutting Concerns** (aspectos transversales) a través de la anotación `@ControllerAdvice` (y su especialización REST `@RestControllerAdvice`).

### ¿Cómo funciona internamente?
Spring utiliza el concepto de **AOP (Programación Orientada a Aspectos)** y filtros de interceptación:
1. El cliente realiza una solicitud HTTP a un endpoint expuesto en un `@RestController`.
2. Durante el procesamiento, ocurre un error y se lanza una excepción (por ejemplo, `StudentNotFoundException` o `MethodArgumentNotValidException`).
3. La excepción se propaga hacia arriba en el hilo de ejecución.
4. El despachador central de Spring MVC (`DispatcherServlet`) detecta la excepción no capturada en el controlador y delega la resolución a los beans registrados como manejadores de excepciones (`HandlerExceptionResolverComposite`).
5. El componente anotado con `@RestControllerAdvice` intercepta la excepción, busca el método `@ExceptionHandler` con la coincidencia más específica, construye la respuesta HTTP (`ResponseEntity` o `ProblemDetail`) y la envía serializada (JSON) al cliente.

```
+---------------------------------------------------------------------------------------+
|                                    Spring MVC Context                                  |
|                                                                                       |
|   +----------+        +--------------------+        +-----------------------------+   |
|   |  Client  | ---->  | DispatcherServlet  | ---->  |   StudentRestController     |   |
|   |          |        +--------------------+        |  (Lanza StudentNotFoundExc) |   |
|   |          |                  |                   +-----------------------------+   |
|   |          |                  |                                 |                   |
|   |          |                  v (Delegación tras error)         v (Propagación)     |
|   |          |        +-------------------------------------------------------+       |
|   |          |        |        GlobalExceptionHandler (@RestControllerAdvice) |       |
|   |          |        |    - @ExceptionHandler(StudentNotFoundException.class)|       |
|   |          |        |    - @ExceptionHandler(Exception.class)               |       |
|   |          |        +-------------------------------------------------------+       |
|   |          |                  |                                                     |
|   | HTTP 404 | <----------------+ (Serializa JSON de Error)                           |
+---------------------------------------------------------------------------------------+
```

---

## 2. ¿Cuándo y para qué se utiliza?

### ¿Para qué se utiliza?
- **Principio DRY (Don't Repeat Yourself)**: Elimina la duplicación de código de manejo de errores en cada controlador.
- **Desacoplamiento y Responsabilidad Única (SRP)**: Los controladores se enfocan exclusivamente en recibir peticiones y coordinar la respuesta exitosa (el "camino feliz"), mientras que el aspecto de error se delega a una clase especializada.
- **Estandarización de Contratos**: Asegura que cualquier error en la API (sea de validación, seguridad, negocio o del sistema) retorne un esquema JSON uniforme, predecible y documentable (p. ej., según RFC 9457).
- **Seguridad**: Previene fugas de información sensible (como trazas de la base de datos o stack traces del servidor) interceptando errores inesperados con un manejador genérico seguro (`500 Internal Server Error`).

### ¿Cuándo se utiliza?
Se debe utilizar en **prácticamente todas las aplicaciones y microservicios REST** de producción:
- Cuando la aplicación cuenta con más de un controlador y se desea mantener una respuesta de error homogénea.
- Cuando se integran validaciones de entrada (`@Valid`, Bean Validation) que lanzan excepciones automáticas de Spring.
- Cuando se desea mapear excepciones técnicas de infraestructura (ej. `DataIntegrityViolationException`, `HttpMessageNotReadableException`) a respuestas HTTP legibles para el cliente.

---

## 3. Diferencias: Manejo Local vs. Manejo Global

| Característica | Manejo Local (`@ExceptionHandler` en Controller) | Manejo Global (`@RestControllerAdvice`) |
| :--- | :--- | :--- |
| **Ubicación** | Dentro de la clase `@RestController` individual. | En una clase separada anotada con `@RestControllerAdvice` (o `@ControllerAdvice`). |
| **Alcance (Scope)** | **Local**: Solo captura excepciones lanzadas por métodos de ese controlador específico. | **Global**: Captura excepciones de **todos** los controladores de la aplicación (o de un paquete configurado). |
| **Reutilización de código** | Nula. Se debe copiar y pegar el manejador en cada controlador que lance la misma excepción. | Alta. Un único método gestiona la excepción para toda la API. |
| **Separación de responsabilidades** | Baja. Mezcla lógica de ruteo/negocio con lógica de transformación de errores. | Alta. Separa nítidamente el manejo de errores del flujo del controlador. |
| **Prioridad de resolución** | **Mayor prioridad**. Si un controlador tiene un manejador local, Spring lo ejecuta primero. | **Menor prioridad** que el local. Se ejecuta si el controlador local no capturó la excepción. |
| **Casos de uso recomendados** | Casos excepcionales donde un controlador específico requiere un formato o tratamiento de error totalmente distinto al resto de la API. | Estándar por defecto para el 99% de las APIs REST modernas. |

---

## 4. ¿Se crea un manejador global por cada controlador o es único y genérico?

### La regla general: **Único y Genérico**
En la gran mayoría de los proyectos y microservicios, se implementa **un único manejador global** (comúnmente llamado `GlobalExceptionHandler` o `RestExceptionHandler`) para toda la aplicación.

**¿Por qué es único?**
- Porque el contrato de error de la API suele ser universal en todo el servicio.
- Porque excepciones como `ResourceNotFoundException`, `MethodArgumentNotValidException` o `BadCredentialsException` tienen el mismo significado conceptual sin importar qué recurso se esté consultando.

### ¿Cuándo se pueden tener múltiples manejadores de excepciones (`@ControllerAdvice`)?
Spring permite segmentar el alcance de los `@ControllerAdvice` mediante selectores. Esto es útil en arquitecturas más complejas:

1. **Segmentación por tipo de cliente (Web MVC vs. REST API)**:
   - Una clase `@ControllerAdvice` para controladores tradicionales que devuelven vistas HTML (Thymeleaf, JSP) renderizando páginas de error (`error.html`).
   - Una clase `@RestControllerAdvice` para la API REST que devuelve respuestas JSON.

2. **Segmentación por paquetes o módulos (`basePackages`)**:
   ```java
   // Aplica solo a controladores del módulo de administración
   @RestControllerAdvice(basePackages = "com.agustinbollati.app.admin.rest")
   public class AdminRestExceptionHandler { ... }

   // Aplica solo a controladores de la API pública
   @RestControllerAdvice(basePackages = "com.agustinbollati.app.publicapi.rest")
   public class PublicApiRestExceptionHandler { ... }
   ```

3. **Segmentación por anotaciones personalizadas (`annotations`)**:
   ```java
   // Aplica únicamente a controladores marcados con una anotación específica
   @RestControllerAdvice(annotations = PremiumApi.class)
   public class PremiumApiExceptionHandler { ... }
   ```

4. **Orden de precedencia (`@Order`)**:
   Si coexisten múltiples `@ControllerAdvice`, se puede utilizar la anotación `@Order` de Spring para definir cuál se evalúa primero:
   ```java
   @RestControllerAdvice
   @Order(Ordered.HIGHEST_PRECEDENCE)
   public class DomainExceptionHandler { ... }
   ```

---

## 5. Ejemplos prácticos: De lo simple a lo complejo (Buenas Prácticas)

A continuación se presentan tres niveles de implementación en proyectos reales, desde un enfoque inicial hasta una solución robusta de nivel empresarial.

---

### 5.1. Nivel 1: Manejador Global Básico (POJO `ErrorResponse`)

Ideal para proyectos pequeños, pruebas de concepto o microservicios con necesidades sencillas de reporte de errores.

#### A. Modelo de respuesta de error (DTO / Record)
> En Java moderno (Java 17 / 21 / 25), los `record` son ideales para modelar DTOs inmutables:

```java
package com.agustinbollati.springboot.demo.rest.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp
) {
    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, LocalDateTime.now());
    }
}
```

#### B. Excepción personalizada de negocio
```java
package com.agustinbollati.springboot.demo.exception;

public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(String message) {
        super(message);
    }
}
```

#### C. Manejador Global con `@RestControllerAdvice`
```java
package com.agustinbollati.springboot.demo.rest.exception;

import com.agustinbollati.springboot.demo.exception.StudentNotFoundException;
import com.agustinbollati.springboot.demo.rest.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Manejo de excepción específica de negocio (404)
    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStudentNotFound(StudentNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // Red de seguridad: Captura cualquier otra excepción no controlada (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Ocurrió un error inesperado en el servidor. Por favor, intente más tarde."
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

---

### 5.2. Nivel 2: Estándar Moderno con RFC 9457 (`ProblemDetail`) y Errores de Validación

En Spring Framework 6+ / 7 y Spring Boot 3+ / 4 se incorporó soporte nativo para el estándar **RFC 7807 / RFC 9457 ("Problem Details for HTTP APIs")** a través de la clase `org.springframework.http.ProblemDetail`. Además, se añade soporte para capturar errores de validación (`@Valid`).

#### A. Manejo Global con `ProblemDetail` y Validación de Campos
```java
package com.agustinbollati.springboot.demo.rest.exception;

import com.agustinbollati.springboot.demo.exception.StudentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class StandardProblemDetailsExceptionHandler {

    // 1. Recurso no encontrado (404) usando ProblemDetail
    @ExceptionHandler(StudentNotFoundException.class)
    public ProblemDetail handleStudentNotFound(StudentNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("Recurso no encontrado");
        problemDetail.setType(URI.create("https://api.agustinbollati.com/errors/not-found"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    // 2. Errores de validación de Bean Validation (@Valid en DTOs) (400)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "La solicitud contiene campos inválidos."
        );
        problemDetail.setTitle("Error de Validación");
        problemDetail.setType(URI.create("https://api.agustinbollati.com/errors/validation-error"));

        // Extraer los errores campo por campo
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        problemDetail.setProperty("invalidFields", fieldErrors);
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }
}
```

#### Ejemplo de respuesta JSON generada (`400 Bad Request`):
```json
{
  "type": "https://api.agustinbollati.com/errors/validation-error",
  "title": "Error de Validación",
  "status": 400,
  "detail": "La solicitud contiene campos inválidos.",
  "instance": "/api/students",
  "invalidFields": {
    "email": "El formato del correo electrónico no es válido",
    "firstName": "El nombre es obligatorio"
  },
  "timestamp": "2026-09-11T16:00:00Z"
}
```

---

### 5.3. Nivel 3: Nivel Empresarial / Producción

En sistemas de misión crítica, arquitecturas de microservicios o APIs corporativas, el manejador global debe:
1. **Extender `ResponseEntityExceptionHandler`**: Para aprovechar todos los manejadores predefinidos de Spring MVC (manejo automático de `HttpRequestMethodNotSupportedException`, `HttpMessageNotReadableException`, etc.).
2. **Jerarquía de Excepciones de Negocio**: Excepciones tipadas que encapsulan su propio `HttpStatus` y código de error interno.
3. **Trazabilidad y Observabilidad**: Inclusión de `traceId` / `correlationId` para correlación de logs en herramientas como OpenTelemetry, Grafana Loki o Splunk.
4. **Logging Estructurado**: Registrar con `WARN` los errores de cliente (`4xx`) y con `ERROR` + stack trace los errores de servidor (`5xx`).

#### A. Jerarquía de Excepciones de Dominio / Negocio
```java
package com.agustinbollati.springboot.demo.exception;

import org.springframework.http.HttpStatus;

public abstract class BusinessException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    protected BusinessException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
```

```java
package com.agustinbollati.springboot.demo.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resourceName, Object id) {
        super(
                String.format("%s con identificador [%s] no fue encontrado.", resourceName, id),
                "ERR_RESOURCE_NOT_FOUND",
                HttpStatus.NOT_FOUND
        );
    }
}
```

#### B. DTO de Error Empresarial con Trazabilidad
```java
package com.agustinbollati.springboot.demo.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        String traceId,
        String errorCode,
        int status,
        String message,
        OffsetDateTime timestamp,
        Map<String, String> details
) {
    public static ApiErrorResponse of(String traceId, String errorCode, int status, String message) {
        return new ApiErrorResponse(traceId, errorCode, status, message, OffsetDateTime.now(), null);
    }

    public static ApiErrorResponse of(String traceId, String errorCode, int status, String message, Map<String, String> details) {
        return new ApiErrorResponse(traceId, errorCode, status, message, OffsetDateTime.now(), details);
    }
}
```

#### C. Manejador Global Empresarial extendiendo `ResponseEntityExceptionHandler`
```java
package com.agustinbollati.springboot.demo.rest.exception;

import com.agustinbollati.springboot.demo.exception.BusinessException;
import com.agustinbollati.springboot.demo.rest.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class EnterpriseGlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(EnterpriseGlobalExceptionHandler.class);
    private static final String TRACE_ID_HEADER = "X-Correlation-Id";

    // 1. Manejo de todas las excepciones de negocio del dominio
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        String traceId = getOrGenerateTraceId();

        log.warn("Excepción de negocio capturada [TraceId: {} | Code: {}]: {}", 
                traceId, ex.getErrorCode(), ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(
                traceId,
                ex.getErrorCode(),
                ex.getHttpStatus().value(),
                ex.getMessage()
        );

        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    // 2. Sobrescritura de métodos estándar de Spring (Bean Validation)
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String traceId = getOrGenerateTraceId();
        Map<String, String> validationErrors = new HashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.warn("Error de validación en la petición [TraceId: {}]: {}", traceId, validationErrors);

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                traceId,
                "ERR_VALIDATION_FAILED",
                status.value(),
                "Los datos de la petición no superaron las validaciones requeridas.",
                validationErrors
        );

        return ResponseEntity.status(status).body(errorResponse);
    }

    // 3. Sobrescritura de JSON mal formado o cuerpo ilegible
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String traceId = getOrGenerateTraceId();
        log.warn("Cuerpo de petición ilegible o JSON mal formado [TraceId: {}]", traceId);

        ApiErrorResponse errorResponse = ApiErrorResponse.of(
                traceId,
                "ERR_MALFORMED_JSON",
                status.value(),
                "El cuerpo de la petición (JSON) es inválido o no se puede procesar."
        );

        return ResponseEntity.status(status).body(errorResponse);
    }

    // 4. Captura general para fallos no previstos (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUncaughtException(Exception ex, HttpServletRequest request) {
        String traceId = getOrGenerateTraceId();

        // Registrar stack trace completo con severidad ERROR para auditoría y observabilidad
        log.error("Fallo crítico no controlado en la aplicación [TraceId: {}]", traceId, ex);

        ApiErrorResponse response = ApiErrorResponse.of(
                traceId,
                "ERR_INTERNAL_SERVER",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Ha ocurrido un error inesperado. Por favor, contacte con soporte indicando el Trace ID."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Obtiene el identificador de correlación desde el MDC (logging) o genera un UUID único.
     */
    private String getOrGenerateTraceId() {
        String traceId = MDC.get("traceId");
        return (traceId != null && !traceId.isBlank()) ? traceId : UUID.randomUUID().toString();
    }
}
```

---

## 6. ¿Cuándo conviene que sea simple vs. cuándo conviene complejizarlo?

```
+-------------------------------------------------------------------------------------------------+
|                               CRITERIOS DE SELECCIÓN DE DISEÑO                                  |
|                                                                                                 |
|   MANTENER SIMPLE (Nivel 1 / 2)                       COMPLEJIZAR (Nivel 3 - Empresarial)       |
|   -----------------------------                       -----------------------------------       |
|   • APIs internas de consumo acotado                  • APIs públicas o con múltiples clientes  |
|   • Microservicios simples / MVPs / POCs              • Arquitecturas de microservicios distrib.|
|   • Equipos de desarrollo pequeños                    • Múltiples equipos en paralelo           |
|   • Sin herramientas complejas de observabilidad      • Integración con OpenTelemetry / APMs    |
|   • Pocos tipos de excepciones de negocio             • Dominios complejos con muchas reglas    |
|   • Cumplimiento con RFC 9457 suficiente              • Necesidad de códigos de error y i18n    |
+-------------------------------------------------------------------------------------------------+
```

### Cuándo mantenerlo simple:
1. **Etapa temprana / Prototipos / MVPs**: Implementar un DTO sencillo (`ErrorResponse`) o usar directamente `ProblemDetail` de Spring Boot 3+/4 permite avanzar rápido sin sobreingeniería.
2. **Microservicios internos con dominio acotado**: Si el servicio solo expone 2 o 3 operaciones CRUD básicas y se comunica únicamente con otros servicios internos de confianza.
3. **Baja complejidad de validaciones**: Cuando los errores se reducen casi exclusivamente a "No encontrado" (`404`) o "Datos inválidos" (`400`).

### Cuándo conviene complejizarlo:
1. **APIs públicas o B2B**: Requieren contratos estrictos, documentación precisa (OpenAPI/Swagger) y códigos de error estandarizados para que los desarrolladores externos integren fácilmente.
2. **Sistemas distribuidos y Microservicios a gran escala**: La inclusión de `traceId` / `correlationId` es indispensable para rastrear una falla a través de múltiples saltos entre microservicios.
3. **Requerimientos de Seguridad y Cumplimiento**: En industrias reguladas (Fintech, Salud), es mandatorio no exponer ningún dato interno en los errores y contar con auditoría detallada en logs estructurados.
4. **Soporte Multilenguaje (i18n)**: Si la API atiende usuarios internacionales y los mensajes de error deben traducirse dinámicamente mediante `MessageSource` y la cabecera `Accept-Language`.
5. **Jerarquías de dominio ricas**: Cuando existen decenas de errores de negocio que comparten comportamientos o metadatos comunes (políticas de reintento, límites de cuota, etc.).

---

## 7. ¿Excepciones por clase de dominio o excepciones genéricas?

Una de las decisiones de diseño más importantes al construir la jerarquía de excepciones de negocio es determinar el **grano de especificidad**: ¿se debe crear una excepción propia por cada entidad de dominio (`StudentNotFoundException`, `CourseNotFoundException`, `InstructorNotFoundException`...) o conviene reutilizar excepciones **genéricas y parametrizables** (`ResourceNotFoundException`, `BadRequestException`, `ConflictException`)?

En la práctica **no existe una única respuesta correcta**: la mejor práctica es un **enfoque híbrido**, y a continuación se explica cómo decidir en cada caso.

### 7.1. Enfoque A: Una excepción por cada clase de dominio

```java
package com.agustinbollati.springboot.demo.exception;

public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(Long studentId) {
        super("Estudiante con id [" + studentId + "] no fue encontrado.");
    }
}
```

```java
package com.agustinbollati.springboot.demo.exception;

public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException(Long courseId) {
        super("Curso con id [" + courseId + "] no fue encontrado.");
    }
}
```

#### Ventajas
- **Legibilidad y semántica explícita**: El nombre de la excepción comunica de inmediato qué entidad falló, sin necesidad de leer el mensaje o el stack trace.
- **Manejo diferenciado por tipo**: Permite capturar (`@ExceptionHandler`) y tratar de forma distinta cada caso particular si, por ejemplo, `CourseNotFoundException` debiera disparar una lógica adicional (auditoría, métricas específicas, eventos de dominio) que `StudentNotFoundException` no requiere.
- **Tipado fuerte en tiempo de compilación**: Facilita detectar errores de uso (lanzar la excepción equivocada) durante la revisión de código o mediante análisis estático.
- **Trazabilidad en logs y monitoreo**: Herramientas de observabilidad (Sentry, Datadog, Grafana) agrupan y alertan más fácilmente por tipo de excepción cuando cada una es distinta.

#### Desventajas
- **Explosión de clases (Class Explosion)**: En un dominio con decenas de entidades, se termina con decenas de clases casi idénticas (`XxxNotFoundException`, `XxxAlreadyExistsException`, `XxxInvalidStateException`...), lo que incrementa el mantenimiento y el "ruido" del código.
- **Duplicación de lógica**: Si cada excepción encapsula su propio `HttpStatus` o `errorCode`, ese comportamiento se repite en cada clase, salvo que se herede de una base común.
- **Manejador global más extenso**: El `@RestControllerAdvice` necesita un método `@ExceptionHandler` por cada tipo (o agruparlos con `@ExceptionHandler({A.class, B.class, ...})`), lo que puede volverse difícil de mantener a medida que crece el dominio.

### 7.2. Enfoque B: Excepciones genéricas y parametrizables

```java
package com.agustinbollati.springboot.demo.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName, Object id) {
        super(String.format("%s con identificador [%s] no fue encontrado.", resourceName, id));
    }
}
```

```java
package com.agustinbollati.springboot.demo.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
```

```java
// Uso en el servicio de negocio
public Student findById(Long id) {
    return studentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Student", id));
}

public Course findCourseById(Long id) {
    return courseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course", id));
}
```

#### Ventajas
- **Reducción drástica de clases**: Un puñado de excepciones (`ResourceNotFoundException`, `BadRequestException`, `ConflictException`, `UnauthorizedException`) cubre la mayoría de los casos de error HTTP (404, 400, 409, 401) de toda la aplicación.
- **Manejador global compacto**: Un solo `@ExceptionHandler` por cada excepción genérica resuelve todos los recursos del dominio, sin importar cuántas entidades existan.
- **Consistencia garantizada**: Como todas las entidades comparten la misma excepción, es imposible que un desarrollador olvide asignarle el `HttpStatus` correcto a una nueva entidad.
- **Escalabilidad del dominio**: Agregar una nueva entidad (`Enrollment`, `Instructor`) no requiere crear nuevas clases de excepción; solo se reutiliza la existente con los parámetros adecuados.

#### Desventajas
- **Pérdida de semántica en el código**: Al leer `throw new ResourceNotFoundException("Course", id)`, el tipo de la excepción no distingue el caso por sí mismo; hay que inspeccionar los argumentos o el mensaje.
- **Dificulta el manejo diferenciado**: Si en el futuro se necesita un comportamiento especial solo para un recurso puntual (por ejemplo, enviar una notificación cuando no se encuentra un `Payment`), no es posible capturarlo con un `@ExceptionHandler` específico sin agregar lógica condicional dentro del manejador genérico (`if (resourceName.equals("Payment")) {...}`), lo cual es una mala práctica (rompe el principio Open/Closed).
- **Menor tipado**: Los errores de uso (pasar el nombre de recurso incorrecto, o confundir el orden de los parámetros) solo se detectan en tiempo de ejecución, no de compilación.

### 7.3. La mejor práctica: Enfoque híbrido basado en códigos de error de negocio

La solución adoptada por la mayoría de los proyectos reales y de nivel empresarial combina ambos enfoques, apoyándose en la jerarquía `BusinessException` presentada en la sección 5.3:

1. **Excepciones genéricas por categoría HTTP** (`ResourceNotFoundException`, `BadRequestException`, `ConflictException`) para los casos estándar de CRUD, parametrizadas con el nombre del recurso y su identificador.
2. **Excepciones específicas de dominio únicamente cuando aportan comportamiento o reglas de negocio propias** — no simplemente para diferenciar el nombre de la entidad. Por ejemplo:
   - `InsufficientFundsException` (en un dominio bancario) sí se justifica como clase propia, porque encapsula datos y lógica particular (saldo disponible, monto solicitado) que no tiene sentido en un `BadRequestException` genérico.
   - `CourseEnrollmentLimitExceededException` se justifica si dispara reglas de negocio adicionales (notificar al administrador, registrar en una tabla de auditoría específica).
   - En cambio, `StudentNotFoundException` y `CourseNotFoundException` **no se justifican como clases separadas** si su único propósito es indicar un 404 sin comportamiento adicional: en ese caso, `ResourceNotFoundException("Student", id)` es preferible.
3. **Códigos de error de negocio (`errorCode`) como discriminador semántico**, en lugar de multiplicar clases. Esto permite mantener pocas clases de excepción, pero conservar una semántica rica y consultable (`ERR_STUDENT_NOT_FOUND`, `ERR_COURSE_NOT_FOUND`) en el campo `errorCode` del cuerpo de la respuesta, sin necesidad de una clase Java por cada una:

```java
public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resourceName, Object id) {
        super(
                String.format("%s con identificador [%s] no fue encontrado.", resourceName, id),
                "ERR_" + resourceName.toUpperCase() + "_NOT_FOUND",
                HttpStatus.NOT_FOUND
        );
    }
}
```

#### Criterio de decisión resumido

| Situación | Recomendación |
| :--- | :--- |
| Error estándar de CRUD (recurso no encontrado, dato inválido, conflicto de unicidad) | Excepción **genérica** parametrizada (`ResourceNotFoundException`, `BadRequestException`, `ConflictException`). |
| Regla de negocio con datos o comportamiento propio (saldo insuficiente, cupo excedido, estado inválido de una máquina de estados) | Excepción **específica** de dominio, heredando de una base común (`BusinessException`). |
| Necesidad de identificar el error unívocamente en logs, métricas o documentación de API sin crear una clase nueva | Usar el campo `errorCode` dentro de una excepción genérica en lugar de crear una subclase. |
| Proyecto pequeño / MVP con pocas entidades | Puede optarse por excepciones específicas simples (Nivel 1 de la sección 5.1), dado que la "explosión de clases" aún no es un problema real. |
| Dominio con decenas o cientos de entidades (proyectos grandes / microservicios amplios) | Priorizar excepciones genéricas parametrizadas para evitar el mantenimiento de una clase por entidad. |

> **Regla mnemotécnica**: *"Una excepción nueva se justifica por el comportamiento que agrega, no por el nombre de la entidad que representa."* Si la única diferencia entre dos excepciones sería el nombre de la clase, ambas deberían ser la misma excepción genérica parametrizada.

---

## 8. ¿Cuál es la diferencia entre `@ControllerAdvice` y `@RestControllerAdvice`?

Ambas anotaciones habilitan el mecanismo de **Cross-Cutting Concerns** para el manejo global de excepciones (y también para `@InitBinder` y `@ModelAttribute` globales), pero difieren en el **tipo de respuesta** que producen sus métodos, de forma análoga a la relación entre `@Controller` y `@RestController`.

### 8.1. `@ControllerAdvice`: anotación base, orientada a vistas (MVC tradicional)

`@ControllerAdvice` es la anotación **genérica y original** introducida en Spring 3.2. Por defecto, sus métodos `@ExceptionHandler` se comportan igual que un método de un `@Controller` clásico:
- Si el método retorna un `String`, Spring lo interpreta como el **nombre de una vista** (por ejemplo, una plantilla Thymeleaf `error.html`) a renderizar.
- Si el método retorna un objeto (`ErrorResponse`, `Map`, etc.) y se desea que ese objeto se serialice en el cuerpo de la respuesta (JSON/XML), es **obligatorio** anotar el método (o la clase) explícitamente con `@ResponseBody`.
- Es la opción adecuada para aplicaciones **MVC tradicionales basadas en vistas** (Thymeleaf, JSP), donde los errores deben renderizar una página HTML de error para el usuario final.

```java
package com.agustinbollati.springboot.demo.web.exception;

import com.agustinbollati.springboot.demo.exception.StudentNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

// Manejador global para controladores @Controller que renderizan vistas HTML
@ControllerAdvice
public class WebGlobalExceptionHandler {

    // Retorna el NOMBRE DE LA VISTA (no un JSON), ya que no lleva @ResponseBody
    @ExceptionHandler(StudentNotFoundException.class)
    public String handleStudentNotFound(StudentNotFoundException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/student-not-found"; // Resuelve a error/student-not-found.html
    }
}
```

### 8.2. `@RestControllerAdvice`: especialización para APIs REST

`@RestControllerAdvice` fue introducida en Spring 4.3 como una **anotación compuesta (meta-anotación)** que combina `@ControllerAdvice` y `@ResponseBody`:

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ControllerAdvice
@ResponseBody
public @interface RestControllerAdvice { ... }
```

Esto significa que **todo lo retornado por los métodos `@ExceptionHandler`** de una clase anotada con `@RestControllerAdvice` se serializa **automáticamente** al cuerpo de la respuesta HTTP (JSON o XML, según el `HttpMessageConverter` negociado), sin necesidad de agregar `@ResponseBody` en cada método. Es exactamente la relación que existe entre `@Controller` + `@ResponseBody` = `@RestController`.

```java
// Equivalencia conceptual:
@RestControllerAdvice  ==  @ControllerAdvice + @ResponseBody
@RestController         ==  @Controller       + @ResponseBody
```

Por eso, en todos los ejemplos de las secciones 5.1 a 5.3 de este documento se utilizó `@RestControllerAdvice`: los métodos retornan directamente `ResponseEntity<ErrorResponse>`, `ProblemDetail` o `ApiErrorResponse`, y Spring los serializa a JSON sin pasos adicionales.

### 8.3. Comparativa directa

| Característica | `@ControllerAdvice` | `@RestControllerAdvice` |
| :--- | :--- | :--- |
| **Naturaleza** | Anotación base / genérica. | Anotación compuesta: `@ControllerAdvice` + `@ResponseBody`. |
| **Serialización automática** | No. Requiere `@ResponseBody` explícito en cada método (o en la clase) para devolver JSON/XML. | Sí. Todos los métodos serializan su valor de retorno al cuerpo de la respuesta automáticamente. |
| **Valor de retorno típico** | `String` (nombre de vista), `ModelAndView`, o un objeto si se agrega `@ResponseBody`. | `ResponseEntity<T>`, `ProblemDetail`, DTOs (`record`, POJOs) serializados directamente. |
| **Caso de uso típico** | Aplicaciones **MVC con vistas** (Thymeleaf, JSP) que renderizan páginas de error HTML. | **APIs REST** que devuelven respuestas de error en JSON/XML. |
| **Introducida en** | Spring 3.2. | Spring 4.3 (como especialización de la anterior). |
| **Relación análoga** | Equivalente a `@Controller`. | Equivalente a `@RestController` (`@Controller` + `@ResponseBody`). |

### 8.4. ¿Cuándo usar cada una?

- **Usar `@RestControllerAdvice`** en el 99% de los casos dentro de un proyecto de **API REST** (como los ejemplos de este curso): es la opción correcta y estándar, ya que evita repetir `@ResponseBody` y deja explícito que la clase existe para producir respuestas de datos, no vistas.
- **Usar `@ControllerAdvice`** únicamente cuando la aplicación es **MVC tradicional basada en vistas** (o híbrida, con controladores `@Controller` que renderizan HTML), y se necesita que el manejador de excepciones retorne nombres de vista o un `ModelAndView` para mostrarle al usuario una página de error amigable, en lugar de un JSON.
- En una aplicación **híbrida** (parte MVC con vistas + parte API REST), es una buena práctica **segmentar** ambos manejadores, tal como se explicó en la sección 4.1: un `@ControllerAdvice` para los controladores de vistas y un `@RestControllerAdvice` con `basePackages` o `annotations` para los controladores REST, evitando que un mismo manejador global mezcle ambas responsabilidades.

---

## 9. Resumen y Buenas Prácticas

- **Utilizar `@RestControllerAdvice` como estándar**: Centraliza el manejo de excepciones y desacopla los controladores de la lógica de error.
- **Evitar `try-catch` en los métodos del `@RestController`**: Dejar que las excepciones fluyan libremente hacia el manejador global.
- **Herencia de `RuntimeException`**: Definir excepciones de negocio como *unchecked exceptions* para no ensuciar las firmas de métodos con `throws`.
- **Extender de `ResponseEntityExceptionHandler`**: Garantiza que los errores de protocolo HTTP propios de Spring MVC se manejen correctamente en formato JSON.
- **Nunca exponer stack traces al cliente**: Los detalles técnicos deben registrarse en los logs del servidor (`log.error(...)`), nunca en el JSON de respuesta.
- **Aprovechar los estándares modernos**: Usar `ProblemDetail` (RFC 9457) cuando se busque alineación con los estándares web actuales.
- **Preferir excepciones genéricas parametrizadas sobre una excepción por entidad**: Crear una clase de excepción específica solo cuando aporta comportamiento o datos propios de negocio, no únicamente para diferenciar el nombre de la entidad.
- **Elegir `@RestControllerAdvice` sobre `@ControllerAdvice` en APIs REST**: Evita el `@ResponseBody` repetitivo y refleja explícitamente que el manejador produce datos serializados, no vistas HTML.

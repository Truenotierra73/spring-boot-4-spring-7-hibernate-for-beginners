# Separación de Capas y Clases en un Proyecto Spring Boot

## 1. La duda: ¿tantas clases parecidas son necesarias?

A lo largo del curso aparecen varias clases que, a simple vista, parecen "lo mismo" (todas representan a un `Student`, por ejemplo):

- Una clase **POJO** simple (`Student`) para transportar datos en JSON con Jackson.
- Una clase **Entity** (`Student` con `@Entity`, `@Table`, `@Id`, etc.) que mapea con una tabla de la base de datos.
- Una **interfaz DAO** (`StudentDAO`) y su **implementación** (`StudentDAOImpl`).
- Opcionalmente, una **interfaz Repository de Spring Data JPA** (`StudentRepository`).
- Una **interfaz de servicio** (`StudentService`) y su implementación (`StudentServiceImpl`).
- Un **`@RestController`** que expone endpoints HTTP.

**Sí, en un proyecto profesional vas a tener múltiples clases para cada responsabilidad particular, aunque se "vean" similares.** Esto no es redundancia: es **separación de responsabilidades (Separation of Concerns)**, uno de los principios más importantes del diseño de software. Cada clase existe para resolver un problema distinto, en una capa distinta, y eso trae beneficios: mantenibilidad, testeabilidad, bajo acoplamiento y flexibilidad para cambiar una capa sin romper las demás.

## 2. Separación de Capas y Clases

La arquitectura típica de una aplicación Spring Boot con JPA se organiza en capas. Cada capa tiene un tipo de clase con un nombre típico y una responsabilidad concreta:

| Capa | Tipo de Clase | Nombre Típico | Responsabilidad |
|---|---|---|---|
| **Presentación (Web)** | Controlador REST | `StudentRestController` | Exponer endpoints HTTP (`@GetMapping`, `@PostMapping`, etc.), recibir/devolver JSON usando Jackson, delegar la lógica al Service. **No** contiene lógica de negocio ni de acceso a datos. |
| **Transferencia de datos (opcional)** | DTO / POJO | `StudentDTO`, `Student` (POJO) | Representar los datos que entran/salen por la API (JSON <-> Java). Puede coincidir o no con la Entity, según el diseño. |
| **Negocio (Service)** | Interfaz + Implementación | `StudentService` / `StudentServiceImpl` | Contener la **lógica de negocio** (validaciones, reglas, orquestación de varias operaciones). Actúa como intermediario entre el Controller y el DAO/Repository. Suele estar anotada con `@Service`. |
| **Acceso a datos (DAO/Repository)** | Interfaz + Implementación, o Interfaz Spring Data | `StudentDAO` / `StudentDAOImpl`, o `StudentRepository extends JpaRepository` | Encapsular el acceso a la base de datos: consultas, `EntityManager`, o los métodos heredados de `JpaRepository`. Anotada con `@Repository` (en la implementación manual) o directamente detectada por Spring Data JPA. |
| **Persistencia (Modelo de datos)** | Entity | `Student` (con `@Entity`) | Mapear una fila de una tabla de la base de datos a un objeto Java, usando anotaciones JPA (`@Entity`, `@Table`, `@Id`, `@Column`, etc.). Es el objeto que realmente "conoce" la base de datos. |
| **Configuración/Infraestructura** | Bean / Componente | Clases anotadas con `@Component`, `@Configuration`, `@Bean` | Definir la infraestructura de la aplicación (beans, configuración de `DataSource`, `EntityManagerFactory`, etc.), independientemente del negocio. |

### Aclaración sobre `@Component`, DAO y JPA Repository

- `@Component` (y sus especializaciones `@Service`, `@Repository`, `@Controller`/`@RestController`) es la forma en que Spring **detecta y gestiona** una clase como Bean dentro del contenedor de IoC. Todas las clases de las capas anteriores (Service, DAO, Controller) terminan siendo Beans de Spring, cada una con la anotación apropiada a su rol.
- El **patrón DAO manual** (interfaz `StudentDAO` + implementación `StudentDAOImpl` usando `EntityManager`) y **Spring Data JPA Repository** (`StudentRepository extends JpaRepository<Student, Integer>`) son **dos formas distintas de resolver la misma responsabilidad**: el acceso a datos. Spring Data JPA es, en esencia, un DAO que Spring genera automáticamente en tiempo de ejecución (mediante un *proxy*), evitando que escribas la implementación manualmente.

## 3. Mapeo entre Capas (diagramas)

### 3.1. Diagrama de capas (flujo de una petición HTTP)

```
┌───────────────────────────┐
│      Cliente (HTTP)       │   Envía/recibe JSON
└─────────────┬─────────────┘
              │  JSON <-> Java (Jackson)
┌─────────────▼─────────────┐
│   @RestController          │   StudentRestController
│   (Capa de Presentación)   │   - Recibe @RequestBody
└─────────────┬─────────────┘   - Devuelve POJO/DTO (se serializa a JSON)
              │  Llama a métodos del Service
┌─────────────▼─────────────┐
│   @Service                 │   StudentService / StudentServiceImpl
│   (Capa de Negocio)        │   - Lógica de negocio / validaciones
└─────────────┬─────────────┘
              │  Llama a métodos del DAO/Repository
┌─────────────▼─────────────┐
│  @Repository / DAO         │   StudentDAO / StudentDAOImpl
│  (Capa de Acceso a Datos)  │   o StudentRepository (Spring Data JPA)
└─────────────┬─────────────┘
              │  Usa EntityManager / JPA
┌─────────────▼─────────────┐
│      Entity (@Entity)      │   Student (mapea con la tabla `student`)
│   (Capa de Persistencia)   │
└─────────────┬─────────────┘
              │  SQL (generado por Hibernate/JPA)
┌─────────────▼─────────────┐
│      Base de Datos         │   Tabla `student`
└─────────────────────────────┘
```

### 3.2. Diagrama de conversión de datos entre capas

Cada flecha representa una **conversión o mapeo** entre representaciones distintas de "lo mismo" (un estudiante):

```
JSON (petición HTTP)
   │  Jackson deserializa (@RequestBody)
   ▼
POJO / DTO Java  (ej: Student con getters/setters simples)
   │  el Service puede usar el mismo objeto o mapearlo a una Entity
   ▼
Entity JPA  (Student con @Entity, @Table, @Id, @Column)
   │  el DAO/Repository usa el EntityManager/JpaRepository
   ▼
Fila de la tabla `student` en la base de datos
```

Y en sentido inverso (para responder una consulta):

```
Fila de la tabla `student`
   │  Hibernate/JPA reconstruye el objeto
   ▼
Entity JPA (Student)
   │  el Service la devuelve (o la mapea a un DTO)
   ▼
POJO / DTO Java
   │  Jackson serializa (valor de retorno del @RestController)
   ▼
JSON (respuesta HTTP)
```

### 3.3. ¿Siempre son clases distintas?

No es obligatorio. Hay dos enfoques válidos:

1. **Una sola clase `Student` con anotaciones JPA (`@Entity`)** que se usa tanto para persistencia como para la respuesta/petición JSON. Es el enfoque más simple y el que suele usarse en proyectos pequeños o educativos (como este curso). Jackson serializa/deserializa directamente la Entity.
2. **Clases separadas: Entity + DTO** (`Student` Entity y `StudentDTO` POJO). Es el enfoque recomendado en proyectos grandes/profesionales, porque desacopla el modelo de base de datos del modelo expuesto por la API (permite ocultar campos sensibles, versionar la API sin tocar la base de datos, evitar problemas de serialización con relaciones JPA `@OneToMany`/`@ManyToOne`, etc.).

En ambos casos, la separación de capas (Controller / Service / DAO-Repository / Entity) se mantiene igual; lo que cambia es si existe o no una clase DTO adicional entre el Controller y el resto de las capas.

### 3.4. `StudentRequest` y `StudentResponse` como mappers de serialización/deserialización

Dentro del enfoque "Entity + DTO", es muy común **dividir el DTO en dos clases distintas** según la dirección del flujo de datos, en lugar de usar un único DTO genérico:

- **`StudentRequest`**: DTO que representa el JSON que **entra** en una petición (`@RequestBody`). Contiene solo los campos que el cliente puede enviar (por ejemplo, no incluye el `id` en una creación, porque lo genera la base de datos).
- **`StudentResponse`**: DTO que representa el JSON que **sale** en una respuesta. Contiene solo los campos que se quieren exponer al cliente (por ejemplo, puede excluir una contraseña o incluir campos calculados que no existen en la Entity).

Esta separación es útil porque **la forma de los datos de entrada no siempre coincide con la de salida**: en un `POST` de creación, el request no trae `id`, pero el response sí lo devuelve (recién generado); un campo puede ser obligatorio al crear pero no modificable al actualizar; se puede querer ocultar datos sensibles solo en la respuesta; etc. Usar una sola clase DTO para ambos casos obliga a "forzar" campos opcionales o nulos que no siempre aplican.

```java
@RestController
@RequestMapping("/api")
public class StudentRestController {

    @PostMapping("/students")
    public StudentResponse addStudent(@RequestBody StudentRequest request) {
        // Jackson deserializa el JSON de entrada a StudentRequest
        Student entity = mapToEntity(request);      // Request -> Entity
        Student saved = studentService.save(entity); // persistencia (JPA)
        return mapToResponse(saved);                  // Entity -> Response (Jackson lo serializa a JSON)
    }
}
```

En este esquema, `StudentRequest` y `StudentResponse` cumplen el rol de **mapper** en cada dirección: Jackson los usa directamente para la conversión JSON <-> Java, y el código de la aplicación (a mano, o con una librería como MapStruct) se encarga de mapear entre estos DTOs y la Entity.

```
JSON (petición)  --Jackson-->  StudentRequest  --mapeo manual/MapStruct-->  Entity (Student)
Entity (Student) --mapeo manual/MapStruct-->  StudentResponse  --Jackson-->  JSON (respuesta)
```

#### Uso de `record` para `StudentRequest` y `StudentResponse`

En versiones modernas de Java (desde Java 14+, y de uso común desde Java 17/21 en adelante), es habitual implementar estos DTOs como **`record`** en lugar de clases tradicionales. Un `record` es un tipo inmutable que genera automáticamente el constructor, los `getters` (con el mismo nombre del atributo, sin prefijo `get`), `equals()`, `hashCode()` y `toString()`, reduciendo el código repetitivo (*boilerplate*) de un POJO clásico.

```java
public record StudentRequest(String firstName, String lastName) {
}

public record StudentResponse(Integer id, String firstName, String lastName) {
}
```

Jackson es compatible con `record` de forma nativa: para deserializar, utiliza el **constructor canónico** del `record` (en lugar de setters); para serializar, utiliza los métodos de acceso generados automáticamente (`firstName()`, `lastName()`, en lugar de `getFirstName()`, `getLastName()`).

```java
String json = "{\"firstName\":\"Mario\",\"lastName\":\"Rossi\"}";

ObjectMapper objectMapper = new ObjectMapper();
StudentRequest request = objectMapper.readValue(json, StudentRequest.class);

System.out.println(request.firstName()); // Mario
```

## 4. Resumen

| Pregunta | Respuesta corta |
|---|---|
| ¿Voy a tener múltiples clases similares? | Sí, una por cada capa/responsabilidad, aunque representen "lo mismo" conceptualmente. |
| ¿Por qué no usar una sola clase para todo? | Porque mezclaría responsabilidades (JSON, persistencia, negocio) y dificultaría el mantenimiento, las pruebas y los cambios futuros. |
| ¿Qué anota cada clase? | Controller: `@RestController`. Service: `@Service`. DAO/Repository: `@Repository` (o nada, si es interfaz de Spring Data JPA). Entity: `@Entity` + `@Table`. |
| ¿Jackson y JPA compiten entre sí? | No. Jackson trabaja en la capa de presentación (JSON <-> Java). JPA/Hibernate trabaja en la capa de persistencia (Java <-> Base de Datos). Pueden operar sobre el mismo objeto Java o sobre objetos distintos (DTO vs Entity). |

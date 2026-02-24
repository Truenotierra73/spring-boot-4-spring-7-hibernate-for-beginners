
# JPA EntityManager — CRUD y Consultas JPQL

## ¿Qué es CRUD?

CRUD es el acrónimo de las cuatro operaciones básicas de persistencia de datos:

| Letra | Operación | SQL equivalente | HTTP (REST) |
|-------|-----------|-----------------|-------------|
| **C** | Create    | `INSERT`        | `POST`      |
| **R** | Read      | `SELECT`        | `GET`       |
| **U** | Update    | `UPDATE`        | `PUT/PATCH` |
| **D** | Delete    | `DELETE`        | `DELETE`    |

### ¿Para qué se utiliza?

CRUD representa el **ciclo de vida completo** de cualquier entidad persistente. Es la base de cualquier sistema que necesite almacenar, recuperar, modificar o eliminar datos: desde una simple agenda de contactos hasta un sistema bancario empresarial.

### ¿Cuándo se utiliza?

- Cuando la aplicación necesita **persistir estado** entre sesiones o reinicios.
- En sistemas con **gestión de entidades**: usuarios, productos, pedidos, facturas, etc.
- En el desarrollo de **APIs REST**, donde cada verbo HTTP mapea a una operación CRUD.
- En cualquier capa de **acceso a datos (DAO/Repository)** de una aplicación.

---

## Entidad de ejemplo

Todos los ejemplos de esta sección utilizan la siguiente entidad:

```java
// Entidad que representa un estudiante en el sistema
@Entity
@Table(name = "estudiante")
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "apellido")
    private String apellido;

    @Column(name = "email")
    private String email;

    // Constructor vacío requerido por JPA
    public Estudiante() {}

    public Estudiante(String nombre, String apellido, String email) {
        this.nombre  = nombre;
        this.apellido = apellido;
        this.email   = email;
    }

    // Getters y setters...
}
```

---

## CREATE — Crear un registro

Se utiliza para **persistir una nueva entidad** en la base de datos.  
El método clave es `entityManager.persist(entidad)`.

### Flujo

```
Objeto Java  →  entityManager.persist()  →  INSERT INTO estudiante ...
```

### DAO

```java
// Clase DAO para operaciones de creación de estudiantes
@Repository
public class EstudianteDAOImpl implements EstudianteDAO {

    private final EntityManager entityManager;

    // Inyección del EntityManager mediante constructor
    public EstudianteDAOImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void guardar(Estudiante estudiante) {
        // persist() inserta el objeto como nuevo registro en la BD
        entityManager.persist(estudiante);
    }
}
```

### Uso en el servicio o runner

```java
// Crear y persistir un nuevo estudiante
Estudiante nuevo = new Estudiante("Juan", "Pérez", "juan@email.com");
estudianteDAO.guardar(nuevo);

// Hibernate ejecuta: INSERT INTO estudiante (nombre, apellido, email) VALUES (?, ?, ?)
System.out.println("Estudiante guardado con ID: " + nuevo.getId());
```

> ✅ Después de `persist()`, Hibernate asigna automáticamente el ID generado al objeto Java.

---

## READ — Leer registros

Se utiliza para **consultar datos** de la base de datos.  
Los métodos clave son `entityManager.find()` para búsqueda por ID y JPQL para consultas más complejas.

### Buscar por ID

```java
// Buscar un estudiante por su clave primaria
@Override
public Estudiante buscarPorId(Long id) {
    // find() retorna null si no existe el registro
    return entityManager.find(Estudiante.class, id);
}
```

```java
// Uso: buscar el estudiante con ID 1
Estudiante encontrado = estudianteDAO.buscarPorId(1L);

// Hibernate ejecuta: SELECT * FROM estudiante WHERE id = ?
if (encontrado != null) {
    System.out.println("Encontrado: " + encontrado.getNombre());
}
```

### Listar todos

```java
// Recuperar todos los estudiantes con una consulta JPQL
@Override
public List<Estudiante> listarTodos() {
    // JPQL usa el nombre de la clase Java, no el nombre de la tabla
    return entityManager
        .createQuery("SELECT e FROM Estudiante e", Estudiante.class)
        .getResultList();
}
```

> ⚠️ `@Transactional` **no es necesario** en operaciones de solo lectura, pero puede agregarse con `readOnly = true` para optimizar el rendimiento.

---

## UPDATE — Actualizar un registro

Se utiliza para **modificar los datos** de una entidad ya existente en la base de datos.  
El método clave es `entityManager.merge(entidad)`.

### Flujo

```
Buscar entidad  →  Modificar atributos  →  entityManager.merge()  →  UPDATE ...
```

### DAO

```java
// Actualizar un estudiante existente en la base de datos
@Override
@Transactional
public Estudiante actualizar(Estudiante estudiante) {
    // merge() sincroniza el estado del objeto con la base de datos
    // Si el objeto tiene ID, realiza un UPDATE; si no, un INSERT
    return entityManager.merge(estudiante);
}
```

### Uso

```java
// Buscar, modificar y actualizar un estudiante
Estudiante estudiante = estudianteDAO.buscarPorId(1L);
estudiante.setEmail("nuevo.email@gmail.com");

Estudiante actualizado = estudianteDAO.actualizar(estudiante);

// Hibernate ejecuta: UPDATE estudiante SET email = ? WHERE id = ?
System.out.println("Email actualizado: " + actualizado.getEmail());
```

### Actualización masiva con JPQL

```java
// Actualizar el dominio de email de todos los estudiantes de forma masiva
@Override
@Transactional
public int actualizarDominioEmail(String domainViejo, String dominioNuevo) {
    return entityManager.createQuery(
        "UPDATE Estudiante e SET e.email = " +
        "CONCAT(SUBSTRING(e.email, 1, LOCATE('@', e.email)), :nuevo) " +
        "WHERE e.email LIKE :patron")
        .setParameter("nuevo", dominioNuevo)
        .setParameter("patron", "%" + domainViejo)
        .executeUpdate();
}
```

---

## DELETE — Eliminar un registro

Se utiliza para **remover una entidad** de la base de datos de forma permanente.  
El método clave es `entityManager.remove(entidad)`.

### Flujo

```
Buscar entidad  →  entityManager.remove()  →  DELETE FROM estudiante WHERE id = ?
```

### DAO

```java
// Eliminar un estudiante por su ID
@Override
@Transactional
public void eliminarPorId(Long id) {
    // Primero se debe buscar la entidad para luego eliminarla
    Estudiante estudiante = entityManager.find(Estudiante.class, id);

    if (estudiante != null) {
        // remove() elimina el objeto del contexto de persistencia y de la BD
        entityManager.remove(estudiante);
    }
}
```

### Uso

```java
// Eliminar el estudiante con ID 2
estudianteDAO.eliminarPorId(2L);

// Hibernate ejecuta: DELETE FROM estudiante WHERE id = ?
System.out.println("Estudiante eliminado.");
```

### Eliminación masiva con JPQL

```java
// Eliminar todos los estudiantes cuyo email pertenece a un dominio específico
@Override
@Transactional
public int eliminarPorDominioEmail(String dominio) {
    return entityManager.createQuery(
        "DELETE FROM Estudiante e WHERE e.email LIKE :patron")
        .setParameter("patron", "%" + dominio)
        .executeUpdate();
}
```

---

## JPQL — Jakarta Persistence Query Language

### ¿Qué es JPQL?

JPQL es el **lenguaje de consulta estándar** definido por la especificación Jakarta Persistence (JPA).  
A diferencia de SQL, JPQL **opera sobre entidades Java y sus atributos**, no sobre tablas y columnas directamente. Esto lo hace independiente de la base de datos subyacente.

```
SQL:   SELECT * FROM estudiante WHERE apellido = 'Pérez'
JPQL:  SELECT e FROM Estudiante e WHERE e.apellido = 'Pérez'
         ↑                  ↑                   ↑
    clase Java         nombre clase         atributo Java
```

### ¿Cuándo utilizar JPQL?

| Situación | ¿Usar JPQL? |
|-----------|-------------|
| Consulta simple por ID | ❌ Usar `find()` |
| Listar todos los registros | ✅ |
| Filtrar con condiciones | ✅ |
| Ordenar resultados | ✅ |
| Consultas con `JOIN` entre entidades | ✅ |
| Funciones nativas de la BD | ❌ Usar SQL nativo |
| Operaciones masivas (UPDATE/DELETE) | ✅ |

### Consideraciones importantes

> ⚠️ **JPQL trabaja con nombres Java, no con nombres SQL.**

1. **Nombres de entidades y atributos:** Se usa el nombre de la **clase Java** (no la tabla) y los nombres de los **atributos del campo** (no las columnas).
2. **Case-sensitive:** Los nombres de entidades y atributos distinguen mayúsculas y minúsculas.
3. **Parámetros nombrados:** Usar siempre `:nombreParam` en lugar de `?1` para mayor legibilidad.
4. **Caché de primer nivel:** Las operaciones `UPDATE` y `DELETE` masivos con JPQL **no actualizan el caché** del contexto de persistencia actual.
5. **No reemplaza SQL nativo:** Para funciones específicas de la BD (MariaDB, PostgreSQL, etc.), usar `@Query(nativeQuery = true)` o `createNativeQuery()`.

---

### Ejemplos de consultas JPQL

#### Listar todos los registros

```java
// Recuperar todos los estudiantes sin ningún filtro
public List<Estudiante> listarTodos() {
    return entityManager
        .createQuery("SELECT e FROM Estudiante e", Estudiante.class)
        .getResultList();
}
```

#### Buscar con parámetro exacto

```java
// Filtrar estudiantes por apellido exacto
public List<Estudiante> buscarPorApellido(String apellido) {
    return entityManager
        .createQuery(
            "SELECT e FROM Estudiante e WHERE e.apellido = :apellido",
            Estudiante.class)
        .setParameter("apellido", apellido)
        .getResultList();
}
```

#### Buscar con LIKE (búsqueda parcial)

```java
// Buscar estudiantes cuyo email contenga una cadena específica
public List<Estudiante> buscarPorEmail(String fragmento) {
    return entityManager
        .createQuery(
            "SELECT e FROM Estudiante e WHERE e.email LIKE :patron",
            Estudiante.class)
        // % antes: coincide cualquier prefijo; % después: cualquier sufijo
        .setParameter("patron", "%" + fragmento + "%")
        .getResultList();
}
```

#### Ordenar resultados

```java
// Listar todos los estudiantes ordenados por apellido de forma ascendente
public List<Estudiante> listarOrdenadosPorApellido() {
    return entityManager
        .createQuery(
            "SELECT e FROM Estudiante e ORDER BY e.apellido ASC, e.nombre ASC",
            Estudiante.class)
        .getResultList();
}
```

#### Múltiples condiciones

```java
// Filtrar por apellido y dominio de email simultáneamente
public List<Estudiante> buscarPorApellidoYDominio(String apellido, String dominio) {
    return entityManager
        .createQuery(
            "SELECT e FROM Estudiante e " +
            "WHERE e.apellido = :apellido AND e.email LIKE :dominio " +
            "ORDER BY e.nombre",
            Estudiante.class)
        .setParameter("apellido", apellido)
        .setParameter("dominio", "%" + dominio)
        .getResultList();
}
```

#### Contar registros

```java
// Contar la cantidad total de estudiantes registrados
public long contarEstudiantes() {
    return entityManager
        .createQuery("SELECT COUNT(e) FROM Estudiante e", Long.class)
        .getSingleResult();
}
```

---

## HQL — Hibernate Query Language

### ¿Qué es HQL?

HQL es el **lenguaje de consulta propio de Hibernate**, predecesor de JPQL.  
La especificación JPA tomó a HQL como base, por lo que ambos son prácticamente idénticos en sintaxis básica, pero HQL ofrece extensiones adicionales no cubiertas por el estándar JPA.

### Diferencias entre JPQL estándar y HQL

| Característica | JPQL (Estándar JPA) | HQL (Hibernate) |
|----------------|---------------------|-----------------|
| **Especificación** | Jakarta EE (portable) | Hibernate (propietario) |
| **Portabilidad** | ✅ Cualquier implementación JPA | ❌ Solo con Hibernate |
| **`SELECT` obligatorio** | ✅ Requerido (modo estricto) | ❌ Opcional (modo relajado) |
| **Funciones nativas de BD** | ❌ No soportadas directamente | ✅ `function('nombre', args)` |
| **`FETCH JOIN`** | ✅ Soportado | ✅ Soportado (con más opciones) |
| **Tipos de resultado extra** | Limitados al estándar | Tuplas, mapas, etc. |

### JPQL modo estricto vs. HQL modo relajado

Hibernate acepta ambos estilos al procesar una consulta:

**Modo relajado (HQL) — `SELECT` implícito:**
```java
// HQL: el SELECT está implícito, Hibernate lo infiere automáticamente
String hql = "FROM Estudiante e WHERE e.apellido = :apellido";

List<Estudiante> resultado = entityManager
    .createQuery(hql, Estudiante.class)
    .setParameter("apellido", "Pérez")
    .getResultList();
```

**Modo estricto (JPQL puro) — `SELECT` explícito:**
```java
// JPQL estricto: el SELECT es obligatorio según la especificación JPA
String jpql = "SELECT e FROM Estudiante e WHERE e.apellido = :apellido";

List<Estudiante> resultado = entityManager
    .createQuery(jpql, Estudiante.class)
    .setParameter("apellido", "Pérez")
    .getResultList();
```

> ✅ **Buena práctica:** Usar siempre el formato **estricto** con `SELECT e` explícito para garantizar la **portabilidad** entre distintas implementaciones JPA (Hibernate, EclipseLink, etc.) y mayor claridad en el código.

### Funciones nativas de MariaDB con HQL

Cuando se necesita una función específica de MariaDB que JPQL no soporta directamente, HQL permite invocarla mediante `function('nombre_funcion', argumentos)`:

```java
// Usar la función nativa DATE_FORMAT de MariaDB desde HQL
// para buscar estudiantes nacidos en un año específico
public List<Estudiante> buscarPorAnioNacimiento(int anio) {
    return entityManager
        .createQuery(
            "SELECT e FROM Estudiante e " +
            "WHERE function('YEAR', e.fechaNacimiento) = :anio",
            Estudiante.class)
        .setParameter("anio", anio)
        .getResultList();
}
```

---

## Resumen comparativo

```
┌─────────────────────────────────────────────────────────┐
│                    Opciones de consulta                  │
├──────────────────┬──────────────────┬───────────────────┤
│   entityManager  │      JPQL        │    SQL Nativo     │
│     .find()      │  (recomendado)   │  (último recurso) │
├──────────────────┼──────────────────┼───────────────────┤
│ Buscar por ID    │ Consultas custom │ Funciones nativas │
│ Operación simple │ Filtros, ORDER   │ Stored procedures │
│ Un solo objeto   │ Portable entre   │ Rendimiento ultra │
│                  │ bases de datos   │ optimizado        │
└──────────────────┴──────────────────┴───────────────────┘
```

---

## Referencias

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/)
- [Hibernate 6 ORM User Guide — HQL](https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#hql)
- [Spring Data JPA — @Query](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)


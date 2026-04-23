
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
Objeto Java  →  entityManager.persist()  →  INSERT INTO estudiante (nombre, apellido, email) VALUES (?, ?, ?)
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

### Flujo

```
Objeto Java  →  entityManager.find() o JPQL  →  SELECT * FROM estudiante WHERE ...
```

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
Buscar entidad  →  Modificar atributos  →  entityManager.merge()  →  UPDATE estudiante SET ... WHERE id = ?
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

## Creación de tablas con JPA/Hibernate

### ¿Cómo genera Hibernate las tablas automáticamente?

Hibernate puede **inspeccionar las entidades Java anotadas** y, a partir de ellas, generar automáticamente el esquema SQL de la base de datos (tablas, columnas, constraints, índices, etc.).  
Este proceso se conoce como **DDL (Data Definition Language) auto-generation** y se controla mediante la propiedad `spring.jpa.hibernate.ddl-auto`.

### Anotaciones JPA para mapear el esquema

#### Anotaciones de entidad y tabla

| Anotación | Nivel | Descripción |
|-----------|-------|-------------|
| `@Entity` | Clase | Marca la clase como entidad JPA gestionada por Hibernate |
| `@Table` | Clase | Especifica el nombre de la tabla y sus atributos (`name`, `schema`, `uniqueConstraints`, `indexes`) |
| `@Id` | Campo | Declara la clave primaria |
| `@GeneratedValue` | Campo | Estrategia de generación del ID (`IDENTITY`, `SEQUENCE`, `TABLE`, `AUTO`, `UUID`) |
| `@Column` | Campo | Mapeo detallado de columna: `name`, `nullable`, `unique`, `length`, `precision`, `scale` |
| `@Lob` | Campo | Columna de tipo `TEXT`/`BLOB` para datos grandes |
| `@Temporal` | Campo | Tipo de dato temporal (`DATE`, `TIME`, `TIMESTAMP`) — obsoleto en JPA 3.x con Java `LocalDate` |
| `@Enumerated` | Campo | Persiste un enum como `ORDINAL` (número) o `STRING` (nombre) |
| `@Transient` | Campo | Excluye el campo del mapeo (no se persiste) |

#### Anotaciones de constraints y rendimiento

```java
// Ejemplo completo de entidad con anotaciones de esquema
@Entity
@Table(
    name = "estudiante",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_estudiante_email",
        columnNames = "email"
    ),
    indexes = @Index(
        name = "idx_estudiante_apellido",
        columnList = "apellido"
    )
)
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // Columna obligatoria, máximo 100 caracteres
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    // Columna obligatoria, máximo 100 caracteres
    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    // Columna única, obligatoria, máximo 255 caracteres
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    // Campo calculado: no se persiste en la BD
    @Transient
    private String nombreCompleto;
}
```

### Flujo: código Java → JPA/Hibernate → SQL → Base de datos

```
┌─────────────────────────────────────────────────────────────────────┐
│                        FLUJO DE CREACIÓN                            │
│                                                                      │
│  1. Clase Java anotada con @Entity, @Table, @Column, etc.           │
│            │                                                         │
│            ▼                                                         │
│  2. Hibernate lee los metadatos de las anotaciones al arrancar       │
│            │                                                         │
│            ▼                                                         │
│  3. Hibernate genera el DDL según la estrategia configurada          │
│     (create, update, validate, none, create-drop)                    │
│            │                                                         │
│            ▼                                                         │
│  4. Se ejecutan los comandos SQL sobre la base de datos              │
│     CREATE TABLE, ALTER TABLE, DROP TABLE, etc.                      │
│            │                                                         │
│            ▼                                                         │
│  5. La base de datos confirma la operación                           │
│     → Tablas listas para persistir entidades                         │
└─────────────────────────────────────────────────────────────────────┘

Ejemplo del DDL generado para la entidad Estudiante:

  CREATE TABLE estudiante (
      id       BIGINT       NOT NULL AUTO_INCREMENT,
      nombre   VARCHAR(100) NOT NULL,
      apellido VARCHAR(100) NOT NULL,
      email    VARCHAR(255) NOT NULL UNIQUE,
      PRIMARY KEY (id)
  );

  CREATE INDEX idx_estudiante_apellido ON estudiante (apellido);
```

---

### Configuración: `spring.jpa.hibernate.ddl-auto`

Esta propiedad controla **qué acción realiza Hibernate sobre el esquema** al iniciar la aplicación.

#### Valores disponibles

| Valor | Comportamiento | Destruye datos |
|-------|---------------|----------------|
| `none` | No realiza ninguna acción sobre el esquema | No |
| `validate` | Valida que el esquema existente coincida con las entidades; lanza error si no coincide | No |
| `update` | Compara entidades vs. esquema existente y aplica los cambios necesarios | No* |
| `create` | Elimina el esquema existente al iniciar y lo vuelve a crear | **Sí** |
| `create-drop` | Crea el esquema al iniciar y lo elimina al apagar la aplicación | **Sí** |

> ⚠️ (*) `update` no elimina columnas ni tablas que hayan dejado de existir en las entidades.

#### Descripción detallada de cada valor

**`none`**
```properties
spring.jpa.hibernate.ddl-auto=none
```
Hibernate no toca el esquema de ninguna manera. La aplicación asume que las tablas ya existen y son correctas.  
Se utiliza en combinación con herramientas externas de migración (Flyway, Liquibase).

---

**`validate`**
```properties
spring.jpa.hibernate.ddl-auto=validate
```
Al arrancar, Hibernate compara el esquema de la base de datos con las entidades mapeadas.  
Si hay discrepancias (una columna faltante, un tipo de dato diferente, etc.), lanza `SchemaManagementException` e impide que la aplicación inicie.  
No modifica nada en la base de datos.

---

**`update`**
```properties
spring.jpa.hibernate.ddl-auto=update
```
Hibernate compara las entidades con el esquema actual y **agrega** lo que falta: nuevas tablas, nuevas columnas, nuevos índices. Sin embargo, **nunca elimina** columnas o tablas obsoletas.

---

**`create`**
```properties
spring.jpa.hibernate.ddl-auto=create
```
Al iniciar, Hibernate **elimina todas las tablas** y las vuelve a crear desde cero según las entidades actuales.  
Todos los datos existentes se pierden en cada reinicio.

---

**`create-drop`**
```properties
spring.jpa.hibernate.ddl-auto=create-drop
```
Similar a `create`, pero además **elimina todas las tablas al cerrar** el contexto de Spring.  
Diseñado para pruebas unitarias de integración donde cada ejecución requiere un estado limpio.

---

#### Cuándo utilizar cada uno — por ambiente

| Ambiente | Valor recomendado | Justificación |
|----------|------------------|---------------|
| **Local / desarrollo inicial** | `create` o `update` | Agiliza la iteración rápida sin gestionar migraciones manualmente |
| **Desarrollo (equipo)** | `update` o `validate` | Permite evolucionar el esquema pero ya empieza a exigir coherencia |
| **Testing / CI** | `create-drop` | Cada ejecución de tests aislada, base de datos siempre limpia |
| **QA / staging** | `validate` + Flyway/Liquibase | Valida que las migraciones aplicadas sean coherentes con el código |
| **Producción** | `none` o `validate` | **Nunca** modificar el esquema automáticamente en producción |

#### Ventajas y desventajas

| Valor | ✅ Ventajas | ❌ Desventajas |
|-------|------------|--------------|
| `none` | Control total; predecible | Requiere gestión externa del esquema |
| `validate` | Detecta inconsistencias al arrancar | No corrige nada; solo reporta |
| `update` | Automático; no borra datos | No elimina columnas huérfanas; riesgo de esquema sucio |
| `create` | Siempre esquema limpio | Pierde todos los datos en cada reinicio |
| `create-drop` | Ideal para tests aislados | Pierde datos al cerrar; inutilizable en producción |

#### Propiedad complementaria: mostrar el SQL generado

```properties
# Muestra el SQL generado por Hibernate en la consola (útil para depuración)
spring.jpa.show-sql=true

# Formatea el SQL para facilitar su lectura
spring.jpa.properties.hibernate.format_sql=true
```

---

## Flyway

### ¿Qué es Flyway?

[Flyway](https://flywaydb.org/) es una herramienta de **migración de bases de datos basada en versiones**.  
Gestiona la evolución del esquema SQL mediante **archivos de migración numerados** que se aplican de forma ordenada y garantizan que el estado de la BD sea siempre reproducible y auditado.

### ¿Para qué sirve?

- Controlar el **historial de cambios** del esquema de la base de datos.
- Garantizar que todos los entornos (local, staging, prod) tengan **exactamente el mismo esquema**.
- Integrar la evolución del esquema al **ciclo de CI/CD**.
- Auditar cuándo y qué cambios se aplicaron en producción.

### ¿Cómo funciona?

Flyway lee los archivos de migración ubicados en `src/main/resources/db/migration/` y los aplica en orden, registrando cada ejecución en la tabla `flyway_schema_history`.

```
src/main/resources/db/migration/
├── V1__crear_tabla_estudiante.sql
├── V2__agregar_columna_telefono.sql
├── V3__crear_tabla_curso.sql
└── V4__datos_iniciales.sql
```

Convención de nombres: `V{versión}__{descripcion}.sql`

```sql
-- V1__crear_tabla_estudiante.sql
CREATE TABLE estudiante (
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    nombre   VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    email    VARCHAR(255) NOT NULL UNIQUE,
    PRIMARY KEY (id)
);
```

### Integración con Spring Boot

```xml
<!-- Dependencia en pom.xml -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<!-- Para MariaDB/MySQL se requiere el módulo adicional -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

```properties
# Al usar Flyway, desactivar la generación automática de Hibernate
spring.jpa.hibernate.ddl-auto=validate

# Flyway se configura automáticamente usando el DataSource de Spring
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

### Ventajas y desventajas de Flyway

| ✅ Ventajas | ❌ Desventajas |
|------------|--------------|
| Migraciones en SQL puro; fácil de auditar | Solo SQL (no abstrae el dialecto de la BD) |
| Historial versionado e inmutable | Las migraciones aplicadas no se pueden modificar |
| Integración nativa con Spring Boot | Migrar entre distintos motores requiere reescribir scripts |
| Sintaxis simple: solo archivos `.sql` | Menos flexible para cambios de datos complejos |
| Ampliamente adoptado en producción | La versión Community tiene funciones limitadas |
| Fácil de integrar en CI/CD | Rollback manual (no automático en Community) |

---

## Liquibase

### ¿Qué es Liquibase?

[Liquibase](https://www.liquibase.org/) es una herramienta de **migración de bases de datos basada en changesets**.  
A diferencia de Flyway, permite describir los cambios del esquema en **XML, YAML, JSON o SQL**, y su motor genera el SQL apropiado para cada base de datos destino.

### ¿Para qué sirve?

- Gestionar la evolución del esquema de forma **agnóstica al motor** de base de datos.
- Facilitar **rollbacks automáticos** de cambios (en formatos declarativos).
- Mantener un historial detallado de cada conjunto de cambios aplicados.
- Generar el SQL diferencial entre dos estados del esquema.

### ¿Cómo funciona?

Liquibase lee un archivo maestro `db.changelog-master.yaml` (o XML/JSON) que referencia los *changelogs* individuales:

```
src/main/resources/db/changelog/
├── db.changelog-master.yaml
├── v1.0/
│   ├── 001-crear-tabla-estudiante.yaml
│   └── 002-agregar-columna-telefono.yaml
└── v2.0/
    └── 001-crear-tabla-curso.yaml
```

```yaml
# db.changelog-master.yaml
databaseChangeLog:
  - include:
      file: db/changelog/v1.0/001-crear-tabla-estudiante.yaml
  - include:
      file: db/changelog/v1.0/002-agregar-columna-telefono.yaml
```

```yaml
# v1.0/001-crear-tabla-estudiante.yaml
databaseChangeLog:
  - changeSet:
      id: 001-crear-tabla-estudiante
      author: equipo-dev
      changes:
        - createTable:
            tableName: estudiante
            columns:
              - column:
                  name: id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: nombre
                  type: VARCHAR(100)
                  constraints:
                    nullable: false
              - column:
                  name: email
                  type: VARCHAR(255)
                  constraints:
                    nullable: false
                    unique: true
```

### Integración con Spring Boot

```xml
<!-- Dependencia en pom.xml -->
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>
```

```properties
# Al usar Liquibase, desactivar la generación automática de Hibernate
spring.jpa.hibernate.ddl-auto=validate

# Liquibase se configura automáticamente con Spring Boot
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml
```

### Ventajas y desventajas de Liquibase

| ✅ Ventajas | ❌ Desventajas |
|------------|--------------|
| Compatible con múltiples formatos (XML, YAML, JSON, SQL) | Mayor curva de aprendizaje que Flyway |
| Rollback automático en formatos declarativos | YAML/XML más verboso que SQL plano |
| Independiente del motor de BD (genera el DDL apropiado) | Más complejo de depurar en proyectos grandes |
| Mejor soporte para cambios de datos complejos | Puede resultar excesivo para proyectos simples |
| Control granular por `context` y `label` | La generación de SQL implícita puede ser difícil de auditar |
| Historial detallado en `DATABASECHANGELOG` | Requiere más configuración inicial |

---

## Flyway vs Liquibase

| Característica | Flyway | Liquibase |
|----------------|--------|-----------|
| **Formato de migraciones** | Solo SQL (o Java) | XML, YAML, JSON, SQL |
| **Portabilidad entre BD** | ❌ SQL específico por BD | ✅ Abstrae el dialecto de la BD |
| **Rollback automático** | ❌ Solo versión Pro | ✅ En formatos declarativos |
| **Curva de aprendizaje** | Baja | Media-Alta |
| **Verbosidad** | Baja (archivos `.sql`) | Media-Alta (YAML/XML) |
| **Integración Spring Boot** | ✅ Nativa | ✅ Nativa |
| **Auditoría** | `flyway_schema_history` | `DATABASECHANGELOG` |
| **Idóneo para** | Equipos que dominan SQL | Proyectos multi-BD o con rollbacks complejos |
| **Comunidad / adopción** | Muy alta | Alta |

### Recomendación de uso por contexto

| Contexto | Herramienta recomendada | Justificación |
|----------|------------------------|---------------|
| Proyecto con un solo motor de BD | **Flyway** | Simple, directo, SQL puro y fácil de auditar |
| Proyecto multi-motor o con rollbacks frecuentes | **Liquibase** | Portabilidad y rollback automático incorporado |
| Equipo sin experiencia en herramientas de migración | **Flyway** | La curva de aprendizaje es mínima |
| Proyecto empresarial con governance estricto | **Liquibase** | Mejor control de contextos, etiquetas y auditoría granular |
| Solo exploración / prototipado | `ddl-auto=create` o `create-drop` | Sin overhead de gestión de migraciones |

> 🏆 **Regla de oro:** En **producción, nunca usar** `ddl-auto=create` ni `ddl-auto=update`. Siempre gestionar el esquema con **Flyway o Liquibase** y establecer `ddl-auto=validate` o `ddl-auto=none`.

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


# CommandLineRunner en Spring Boot

## ¿Qué es CommandLineRunner?

`CommandLineRunner` es una interfaz funcional proporcionada por Spring Boot que permite ejecutar código específico justo después de que la aplicación Spring haya iniciado completamente. Es útil para ejecutar tareas de inicialización, cargar datos de prueba, o realizar cualquier acción que deba ejecutarse una vez que el contexto de Spring esté listo.

> **Nota:** Esta funcionalidad está disponible desde las primeras versiones de Spring Boot y se mantiene sin cambios en Spring Boot 4 con Java 25.

## ¿Para qué sirve?

- Ejecutar lógica de inicialización al arrancar la aplicación.
- Cargar datos iniciales en la base de datos.
- Realizar comprobaciones o configuraciones automáticas.
- Ejecutar scripts o procesos automáticos al inicio.

## ¿Cómo se utiliza?

Para utilizar `CommandLineRunner`, simplemente se debe crear una clase que implemente esta interfaz y anotar la clase como un componente de Spring (`@Component`). El método `run` se ejecutará automáticamente al iniciar la aplicación.

### Ejemplo de uso

```java
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Ejemplo de implementación de CommandLineRunner.
 * Este componente se ejecutará al iniciar la aplicación.
 */
@Component
public class MiRunner implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        // Lógica a ejecutar al iniciar la aplicación
        System.out.println("La aplicación ha iniciado correctamente.");
    }
}
```

### Ejemplo con acceso a repositorios JPA

```java
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class CargarDatosIniciales implements CommandLineRunner {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public void run(String... args) throws Exception {
        // Crear y guardar un usuario de ejemplo
        Usuario usuario = new Usuario("Juan", "Pérez");
        usuarioRepository.save(usuario);
        System.out.println("Usuario de ejemplo guardado en la base de datos.");
    }
}
```

### Ejemplo definiendo un CommandLineRunner como Bean

Otra forma común de utilizar `CommandLineRunner` es definiendo un bean en la clase principal de la aplicación o en una clase de configuración. Esto permite escribir la lógica de inicialización directamente usando una expresión lambda.

```java
package com.agustinbollati.cruddemo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CruddemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CruddemoApplication.class, args);
    }

    /**
     * Ejemplo de CommandLineRunner definido como Bean en Spring Boot 4.
     * Este bean se ejecutará automáticamente al iniciar la aplicación.
     */
    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            System.out.println("Hello World desde Spring Boot 4 y Java 25");
        };
    }
}
```

> **Nota:** Puedes tener múltiples implementaciones de `CommandLineRunner` en tu proyecto. Todas se ejecutarán al iniciar la aplicación.

---

**En resumen:** `CommandLineRunner` es una herramienta muy útil para ejecutar código automáticamente al inicio de una aplicación Spring Boot, facilitando tareas de inicialización y configuración.

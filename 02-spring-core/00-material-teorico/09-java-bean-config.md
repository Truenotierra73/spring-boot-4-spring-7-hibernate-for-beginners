# Configuración de Beans con código Java en Spring

## ¿Qué es la configuración de un Bean con código Java?

La configuración de un Bean con código Java es una forma de definir y registrar beans en el contenedor de Spring utilizando clases Java y anotaciones, en lugar de archivos XML. Este enfoque se conoce como **Java-based Configuration** y permite aprovechar el tipado fuerte, la refactorización y las capacidades del IDE.

## ¿Para qué sirve?

Sirve para:
- Registrar beans personalizados en el contexto de Spring.
- Configurar dependencias, propiedades y ciclos de vida de los beans.
- Centralizar la configuración de la aplicación de manera programática y mantenible.

## Casos de uso (¿Cuándo utilizarlo?)
- Cuando se requiere una configuración más flexible y controlada que la que ofrecen las anotaciones como `@Component`.
- Cuando se necesita crear beans de librerías externas o de clases que no se pueden anotar.
- Para definir beans condicionales, personalizados o con lógica de inicialización compleja.

## Ejemplo básico

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration // Indica que esta clase contiene definiciones de beans
public class AppConfig {
    @Bean // Indica que el método retorna un bean gestionado por Spring
    public MiServicio miServicio() {
        return new MiServicioImpl();
    }
}
```

En este ejemplo:
- `@Configuration` marca la clase como fuente de definiciones de beans.
- `@Bean` indica que el método produce un bean que será gestionado por el contenedor de Spring.

## Cómo hacer uso del bean mediante inyección de dependencias

Una vez que el bean está definido en una clase de configuración, puedes inyectarlo en otras clases utilizando la anotación `@Autowired` o mediante inyección por constructor (recomendada en Spring Boot 4 y Spring 7).

**Ejemplo de inyección por constructor:**

```java
import org.springframework.stereotype.Component;

@Component
public class ClienteServicio {
    private final MiServicio miServicio;

    // Inyección de dependencias por constructor
    public ClienteServicio(MiServicio miServicio) {
        this.miServicio = miServicio;
    }

    // Métodos que usan miServicio...
}
```

Spring detecta automáticamente el bean `MiServicio` y lo inyecta en el constructor de `ClienteServicio`.

## Nombre del bean por defecto y alternativas

Por defecto, el nombre del bean es el nombre del método anotado con `@Bean`. Por ejemplo, en el siguiente método:

```java
@Bean
public MiServicio miServicio() {
    return new MiServicioImpl();
}
```

El nombre del bean será `miServicio`.

### Alternativas para definir el nombre del bean
Puedes especificar un nombre personalizado usando el atributo `name` o pasando un valor al método `@Bean`:

```java
@Bean("servicioPrincipal")
public MiServicio miServicio() {
    return new MiServicioImpl();
}
```

En este caso, el bean se registrará con el nombre `servicioPrincipal`.

También puedes definir varios nombres (alias):

```java
@Bean({"servicioPrincipal", "servicioSecundario"})
public MiServicio miServicio() {
    return new MiServicioImpl();
}
```

Así, el bean será accesible por cualquiera de esos nombres.

## Explicación de las anotaciones

### @Configuration
- Marca una clase como clase de configuración.
- Spring procesa esta clase para registrar los beans definidos en sus métodos anotados con `@Bean`.
- Es equivalente a `<beans>` en XML.

### @Bean
- Marca un método como productor de un bean.
- El valor retornado por el método será registrado como bean en el contexto de Spring.
- Permite configurar dependencias, inicialización y destrucción del bean.

## Referencias de la documentación
- [Spring Framework - Java-based Container Configuration](https://docs.spring.io/spring-framework/reference/core/beans/java.html)
- [Spring Framework - @Configuration](https://docs.spring.io/spring-framework/reference/core/beans/java/configuration-annotation.html)
- [Spring Framework - @Bean](https://docs.spring.io/spring-framework/reference/core/beans/java/bean-annotation.html)

---

> **Nota:** Utilizar configuración Java es una buena práctica recomendada en proyectos modernos con Spring Boot 4 y Spring Framework 7, ya que mejora la mantenibilidad y claridad del código.

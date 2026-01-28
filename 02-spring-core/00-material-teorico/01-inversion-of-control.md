# Inversión de Control (IoC) e Inversión de Dependencia (DI)

La **Inversión de Control** (IoC) es un principio fundamental en el desarrollo de software que consiste en invertir la
responsabilidad de la creación y gestión de las dependencias de los objetos. En lugar de que una clase cree o gestione
sus propias dependencias, estas le son proporcionadas desde el exterior, generalmente por un contenedor o framework.

La **Inversión de Dependencia** (DI) es una forma concreta de aplicar IoC, donde las dependencias de una clase se
inyectan (por constructor, método o atributo) en lugar de ser instanciadas directamente dentro de la clase.

---

## Diferencia entre IoC y DI: No son lo mismo

Es importante entender que **IoC y DI no son conceptos idénticos**. La relación entre ellos es:

- **Toda DI es IoC** - Cuando aplicamos inyección de dependencias, estamos aplicando inversión de control
- **No toda IoC es DI** - Existen otras formas de aplicar IoC que no involucran inyección de dependencias

### Ejemplos donde toda DI es IoC:

```java
// Inyección por Constructor (DI que es IoC)
@Service
public class PedidoService {
    private final ClienteService clienteService;
    
    // El control de crear ClienteService está invertido al contenedor Spring
    public PedidoService(ClienteService clienteService) {
        this.clienteService = clienteService;
    }
}

// Inyección por Campo (DI que es IoC)
@RestController
public class PedidoController {
    @Autowired
    private PedidoService pedidoService; // Spring controla la creación
}
```

### Ejemplos donde IoC NO es DI:

#### 1. Patrón Template Method (IoC sin DI)
```java
public abstract class ProcesamientoDatos {
    // El método principal define el flujo (control invertido)
    public final void procesar() {
        cargarDatos();
        validarDatos(); // Control invertido a las subclases
        guardarDatos();
    }
    
    protected abstract void validarDatos(); // Las subclases controlan esta lógica
    
    private void cargarDatos() { /* implementación base */ }
    private void guardarDatos() { /* implementación base */ }
}

public class ProcesamientoClientes extends ProcesamientoDatos {
    @Override
    protected void validarDatos() {
        // La subclase controla esta parte del algoritmo
        System.out.println("Validando datos de clientes...");
    }
}
```

#### 2. Patrón Observer (IoC sin DI)
```java
public class EventoNotificador {
    private List<EventoListener> listeners = new ArrayList<>();
    
    public void suscribir(EventoListener listener) {
        listeners.add(listener);
    }
    
    public void publicarEvento(Evento evento) {
        // El control se invierte a los listeners
        for (EventoListener listener : listeners) {
            listener.manejarEvento(evento); // Control invertido
        }
    }
}

@Component
public class EmailListener implements EventoListener {
    @Override
    public void manejarEvento(Evento evento) {
        // El listener controla cómo manejar el evento
        System.out.println("Enviando email por evento: " + evento.getTipo());
    }
}
```

#### 3. Callbacks y Programación Reactiva (IoC sin DI)
```java
public class ServicioAsincrono {
    public void procesarAsync(String datos, Consumer<String> callback) {
        CompletableFuture.supplyAsync(() -> {
            // Simular procesamiento
            return "Procesado: " + datos;
        }).thenAccept(callback); // Control invertido al callback
    }
}

// Uso del servicio
@Service
public class MiServicio {
    public void ejecutar() {
        ServicioAsincrono servicio = new ServicioAsincrono();
        
        // El control de qué hacer con el resultado se invierte al callback
        servicio.procesarAsync("datos", resultado -> {
            System.out.println("Resultado recibido: " + resultado);
        });
    }
}
```

#### 4. Framework Web Callbacks (IoC sin DI)
```java
@RestController
public class MiController {
    
    // Spring invierte el control: él llama a este método cuando llega una petición
    @GetMapping("/usuarios")
    public ResponseEntity<List<Usuario>> obtenerUsuarios() {
        // No necesariamente hay DI aquí, pero sí IoC
        // Spring controla cuándo ejecutar este método
        return ResponseEntity.ok(Arrays.asList(new Usuario("Juan")));
    }
}
```

### Resumen de la Diferencia

| Aspecto | IoC | DI |
|---------|-----|-----|
| **Definición** | Principio general de inversión del control de flujo | Técnica específica de inyección de dependencias |
| **Alcance** | Amplio: incluye callbacks, eventos, templates, etc. | Específico: solo gestión de dependencias |
| **Ejemplos** | Template Method, Observer, Callbacks, Framework lifecycle | Constructor injection, Setter injection, Field injection |
| **Relación** | Concepto más amplio | Subconjunto específico de IoC |

---

## Ejemplo sin Inversión de Dependencia

En este caso, la clase `ClienteService` crea directamente su dependencia `ClienteRepository`:

```java
public class ClienteService {
    private ClienteRepository repo = new ClienteRepository();
    // ...
}
```

## Ejemplo con Inversión de Dependencia (DI)

Aquí, la dependencia se inyecta desde el exterior (por ejemplo, usando Spring):

```java
public class ClienteService {
    private final ClienteRepository repo;

    public ClienteService(ClienteRepository repo) {
        this.repo = repo;
    }
    // ...
}
```

---

## ¿Qué es el Spring Container?

El **Spring Container** (contenedor de Spring) es el núcleo del framework Spring y es responsable de crear, configurar y
gestionar el ciclo de vida de los beans (objetos) definidos en la aplicación. El contenedor aplica los principios de
Inversión de Control e Inyección de Dependencias, permitiendo que las dependencias sean gestionadas automáticamente y
facilitando el desacoplamiento entre componentes.

El contenedor de Spring se implementa principalmente a través de las interfaces `BeanFactory` y `ApplicationContext`,
siendo esta última la más utilizada en aplicaciones modernas.

El Spring Container se encarga de:

- Crear y configurar los beans definidos en la aplicación.
- Inyectar las dependencias necesarias en cada bean.
- Gestionar el ciclo de vida de los beans (inicialización, destrucción, etc.).

En resumen, el Spring Container es el "cerebro" de la aplicación Spring, responsable de aplicar la Inversión de Control
y la Inyección de Dependencias de manera automática y eficiente.

---

Para más información, consulta la documentación oficial de Spring:

- [Inyección de Dependencias (Spring Framework)](https://docs.spring.io/spring-framework/reference/core/beans/dependencies.html)
- [Contenedor de Spring (Spring Container)](https://docs.spring.io/spring-framework/reference/core/beans/basics.html)

---

> **Resumen:**
> - **IoC** es un principio amplio de inversión de control del flujo de ejecución
> - **DI** es una forma específica de IoC enfocada en la gestión de dependencias
> - **Toda DI es IoC**, pero existen muchas formas de IoC que no son DI (callbacks, observers, templates, etc.)
> - **Spring Container** implementa tanto IoC como DI en aplicaciones Spring Boot 4

# Inyección de Dependencias en Spring

La **Inyección de Dependencias (Dependency Injection, DI)** es un principio fundamental en el desarrollo de aplicaciones
con Spring. Permite desacoplar los componentes de una aplicación, facilitando la reutilización, el mantenimiento y las
pruebas.

## ¿Qué es la Inyección de Dependencias?

La inyección de dependencias es un patrón de diseño que consiste en proporcionar a una clase sus dependencias desde el
exterior, en lugar de que la propia clase las cree o gestione. Así, los objetos no son responsables de instanciar sus
dependencias, sino que estas les son "inyectadas" por un contenedor externo (en este caso, el contenedor de Spring).

## ¿Cómo se realiza en Spring?

Spring gestiona la inyección de dependencias a través de su contenedor de inversión de control (IoC). Existen varias
formas de realizar la inyección de dependencias en Spring:

### 1. Inyección por constructor (recomendada)

- Las dependencias se pasan a través del constructor de la clase.
- Permite definir las dependencias obligatorias de manera explícita.
- Facilita la inmutabilidad y las pruebas unitarias.
- Ejemplo:
  ```java
  @Service
  public class MiServicio {
      private final MiRepositorio repositorio;

      // Inyección por constructor
      public MiServicio(MiRepositorio repositorio) {
          this.repositorio = repositorio;
      }
  }
  ```

### 2. Inyección por setter

- Las dependencias se establecen mediante métodos setter.
- Útil para dependencias opcionales.
- Ejemplo:
  ```java
  @Service
  public class MiServicio {
      private MiRepositorio repositorio;

      // Inyección por setter
      @Autowired
      public void setRepositorio(MiRepositorio repositorio) {
          this.repositorio = repositorio;
      }
  }
  ```

### 3. Inyección por campo (no recomendada)

- Las dependencias se inyectan directamente en los campos de la clase.
- Dificulta las pruebas y el mantenimiento.
- Ejemplo:
  ```java
  @Service
  public class MiServicio {
      // Inyección directa en el campo
      @Autowired
      private MiRepositorio repositorio;
  }
  ```

## Uso de la anotación `@Autowired`

La anotación `@Autowired` es utilizada por Spring para indicar que una dependencia debe ser inyectada automáticamente
por el contenedor. Puede aplicarse a constructores, métodos setter o directamente a campos.

### ¿Qué hace @Autowired exactamente?

`@Autowired` le dice a Spring: **"Busca en tu contenedor un bean del tipo que necesito e inyéctamelo automáticamente"**.

**Proceso interno:**

1. **Escaneo**: Spring encuentra la anotación `@Autowired`
2. **Búsqueda**: Busca en su contenedor de beans uno que coincida por tipo
3. **Resolución**: Si hay múltiples candidatos, usa estrategias de resolución (`@Primary`, `@Qualifier`)
4. **Inyección**: Asigna automáticamente la instancia del bean
5. **Gestión**: Spring gestiona el ciclo de vida de ambos objetos

### ¿Es obligatoria?

- **Constructor:** Desde Spring 4.3, si una clase tiene un único constructor, la anotación `@Autowired` no es
  obligatoria en el constructor, ya que Spring lo detecta automáticamente. Esta práctica continúa en Spring 7.
- **Setter y campo:** Es obligatoria para que Spring realice la inyección.
- **Varios constructores:** Se debe indicar explícitamente cuál debe usar Spring con `@Autowired`.

### ¿Qué función cumple?

- Indica al contenedor de Spring que debe resolver e inyectar la dependencia correspondiente.
- Permite la inyección automática de beans, facilitando el desacoplamiento y la configuración de la aplicación.
- Elimina la necesidad de crear manualmente las instancias de las dependencias.

### Ejemplos de uso

- **En constructor (no obligatoria si es el único constructor):**
  ```java
  @Service
  public class ClienteService {
      private final ClienteRepository clienteRepository;

      // Constructor único, @Autowired es opcional
      public ClienteService(ClienteRepository clienteRepository) {
          this.clienteRepository = clienteRepository;
      }
      
      public Cliente buscarPorId(Long id) {
          return clienteRepository.findById(id);
      }
  }
  ```

- **En setter (obligatoria):**
  ```java
  @Service
  public class PedidoService {
      private PedidoRepository pedidoRepository;

      @Autowired
      public void setPedidoRepository(PedidoRepository pedidoRepository) {
          this.pedidoRepository = pedidoRepository;
      }
      
      public Pedido crearPedido(Pedido pedido) {
          return pedidoRepository.save(pedido);
      }
  }
  ```

- **En campo (obligatoria, pero no recomendada):**
  ```java
  @Service
  public class FacturaService {
      @Autowired
      private FacturaRepository facturaRepository;
      
      public List<Factura> obtenerTodasLasFacturas() {
          return facturaRepository.findAll();
      }
  }
  ```

- **Dependencias opcionales:**
  Se puede indicar que una dependencia es opcional usando `@Autowired(required = false)`.
  ```java
  @Service
  public class EmailService {
      @Autowired(required = false)
      private NotificacionService notificacionService;
      
      public void enviarEmail(String mensaje) {
          // Verificar si el servicio está disponible
          if (notificacionService != null) {
              notificacionService.notificar(mensaje);
          }
          // Continuar con la lógica de envío de email
      }
  }
  ```

### Casos de uso comunes en Spring Boot

1. **Inyección de Repository en Service:**
   ```java
   @Service
   public class UsuarioService {
       private final UsuarioRepository usuarioRepository;
       
       public UsuarioService(UsuarioRepository usuarioRepository) {
           this.usuarioRepository = usuarioRepository;
       }
   }
   ```

2. **Inyección de Service en Controller:**
   ```java
   @RestController
   @RequestMapping("/api/usuarios")
   public class UsuarioController {
       private final UsuarioService usuarioService;
       
       public UsuarioController(UsuarioService usuarioService) {
           this.usuarioService = usuarioService;
       }
       
       @GetMapping("/{id}")
       public Usuario obtenerUsuario(@PathVariable Long id) {
           return usuarioService.buscarPorId(id);
       }
   }
   ```

3. **Inyección de múltiples dependencias:**
   ```java
   @Service
   public class ProcesadorPedido {
       private final PedidoRepository pedidoRepository;
       private final EmailService emailService;
       private final InventarioService inventarioService;
       
       public ProcesadorPedido(PedidoRepository pedidoRepository,
                             EmailService emailService,
                             InventarioService inventarioService) {
           this.pedidoRepository = pedidoRepository;
           this.emailService = emailService;
           this.inventarioService = inventarioService;
       }
       
       public void procesarPedido(Pedido pedido) {
           // Lógica de procesamiento usando todas las dependencias
           inventarioService.verificarDisponibilidad(pedido);
           pedidoRepository.save(pedido);
           emailService.enviarConfirmacion(pedido);
       }
   }
   ```

### ¿Por qué usar @Autowired?

**Ventajas:**

- **Automatización**: Spring gestiona las dependencias automáticamente
- **Desacoplamiento**: Las clases no necesitan saber cómo crear sus dependencias
- **Flexibilidad**: Fácil cambio de implementaciones
- **Testabilidad**: Fácil inyección de mocks en pruebas unitarias

**Comparación - Sin Spring vs Con Spring:**

```java
// SIN SPRING - Acoplamiento fuerte
public class PedidoService {
	private PedidoRepository repository = new PedidoRepositoryJpaImpl();
	private EmailService emailService = new EmailServiceImpl();
	// Difícil de testear y cambiar implementaciones
}

// CON SPRING - Bajo acoplamiento
@Service
public class PedidoService {
	private final PedidoRepository repository;
	private final EmailService emailService;

	// Spring inyecta las implementaciones correctas
	public PedidoService(PedidoRepository repository, EmailService emailService) {
		this.repository = repository;
		this.emailService = emailService;
	}
}
```

> **Resumen**
>
> La clave está en entender que `@Autowired` elimina la necesidad de usar `new` para crear dependencias manualmente. *
*Spring** se encarga de todo: crear, gestionar y proporcionar los objetos que tu código necesita.

---

## Ventajas de la Inyección de Dependencias

La inyección de dependencias ofrece múltiples beneficios que mejoran significativamente la calidad y mantenibilidad del código:

### 1. **Bajo Acoplamiento**
- Las clases no dependen de implementaciones concretas, sino de abstracciones (interfaces).
- Facilita el cambio de implementaciones sin modificar el código cliente.
- Ejemplo:
  ```java
  // Bajo acoplamiento - depende de la interfaz
  @Service
  public class PedidoService {
      private final PedidoRepository repository; // Interfaz, no implementación concreta
      
      public PedidoService(PedidoRepository repository) {
          this.repository = repository;
      }
  }
  ```

### 2. **Facilita las Pruebas Unitarias**
- Permite inyectar mocks o stubs durante las pruebas.
- No requiere modificar el código de producción para realizar pruebas.
- Ejemplo:
  ```java
  @Test
  void testProcesarPedido() {
      // Crear mock del repositorio
      PedidoRepository mockRepository = Mockito.mock(PedidoRepository.class);
      
      // Inyectar el mock en el servicio
      PedidoService service = new PedidoService(mockRepository);
      
      // Realizar pruebas sin base de datos real
  }
  ```

### 3. **Reutilización de Código**
- Los componentes pueden ser reutilizados en diferentes contextos.
- Facilita la creación de bibliotecas y módulos reutilizables.

### 4. **Flexibilidad y Configuración**
- Fácil intercambio de implementaciones según el entorno (desarrollo, pruebas, producción).
- Configuración centralizada de dependencias.

### 5. **Mantenimiento Simplificado**
- Cambios en las dependencias no afectan a las clases que las usan.
- Facilita la refactorización y evolución del código.

### 6. **Principio de Responsabilidad Única**
- Las clases se enfocan en su lógica de negocio, no en crear sus dependencias.
- Mejora la cohesión y reduce la complejidad.

---

## ¿Qué es un Spring Bean?

Un **Spring Bean** es cualquier objeto que es instanciado, configurado, ensamblado y gestionado por el contenedor IoC (Inversión de Control) de Spring. Los beans son los componentes fundamentales de una aplicación Spring.

### Características de un Spring Bean:

1. **Gestionado por Spring**: Su ciclo de vida es controlado completamente por el contenedor.
2. **Singleton por defecto**: Una sola instancia por contexto de aplicación (configurable).
3. **Inyectable**: Puede ser inyectado en otros beans mediante `@Autowired`.
4. **Configurable**: Puede tener propiedades y configuraciones específicas.

### ¿Cómo se define un Bean?

#### 1. **Mediante anotaciones (más común):**
```java
@Service  // Define este objeto como un bean gestionado por Spring
public class UsuarioService {
    // Lógica del servicio
}

@Repository
public class UsuarioRepository {
    // Lógica de acceso a datos
}

@Controller
public class UsuarioController {
    // Lógica del controlador
}
```

#### 2. **Mediante configuración Java:**
```java
@Configuration
public class AppConfig {
    
    @Bean
    public UsuarioService usuarioService() {
        return new UsuarioService();
    }
    
    @Bean
    public DatabaseConnection databaseConnection() {
        return new DatabaseConnection("jdbc:mysql://localhost:3306/mydb");
    }
}
```

#### 3. **Mediante configuración XML (menos usado):**
```xml
<bean id="usuarioService" class="com.ejemplo.UsuarioService"/>
<bean id="usuarioRepository" class="com.ejemplo.UsuarioRepository"/>
```

### Ciclo de vida de un Bean:
1. **Instanciación**: Spring crea la instancia del bean.
2. **Inyección de propiedades**: Se inyectan las dependencias.
3. **Inicialización**: Se ejecutan métodos de inicialización (`@PostConstruct`).
4. **Uso**: El bean está disponible para ser usado.
5. **Destrucción**: Se ejecutan métodos de limpieza (`@PreDestroy`) antes de ser destruido.

### Ejemplo completo:
```java
@Service
public class NotificacionService {
    
    @Autowired
    private EmailService emailService; // Dependencia inyectada
    
    @PostConstruct  // Se ejecuta después de la creación e inyección
    public void inicializar() {
        System.out.println("NotificacionService inicializado");
    }
    
    public void enviarNotificacion(String mensaje) {
        emailService.enviar(mensaje);
    }
    
    @PreDestroy  // Se ejecuta antes de la destrucción del bean
    public void limpiar() {
        System.out.println("Limpiando recursos del NotificacionService");
    }
}
```

---

## Anotaciones Principales de Spring

Spring proporciona un conjunto de anotaciones que facilitan la configuración y gestión de componentes. Aquí están las más importantes:

### 1. **Anotaciones de Estereotipos (Stereotype Annotations)**

#### `@Component`
- **Qué hace**: Marca una clase como un componente de Spring, convirtiéndola en un bean.
- **Para qué sirve**: Componente genérico, base para otras anotaciones más específicas.
- **Ejemplo**:
  ```java
  @Component
  public class UtilityHelper {
      public String formatearTexto(String texto) {
          return texto.toUpperCase();
      }
  }
  ```

#### `@Service`
- **Qué hace**: Especialización de `@Component` para la capa de servicio.
- **Para qué sirve**: Indica que la clase contiene lógica de negocio.
- **Ejemplo**:
  ```java
  @Service
  public class UsuarioService {
      public Usuario crearUsuario(String nombre, String email) {
          // Lógica de negocio para crear usuario
          return new Usuario(nombre, email);
      }
  }
  ```

#### `@Repository`
- **Qué hace**: Especialización de `@Component` para la capa de acceso a datos.
- **Para qué sirve**: Indica que la clase maneja operaciones de base de datos.
- **Ventaja adicional**: Automáticamente traduce excepciones de base de datos.
- **Ejemplo**:
  ```java
  @Repository
  public class UsuarioRepository {
      public Usuario findById(Long id) {
          // Lógica para buscar usuario en la base de datos
          return usuarioEncontrado;
      }
  }
  ```

#### `@Controller`
- **Qué hace**: Especialización de `@Component` para la capa de presentación.
- **Para qué sirve**: Maneja peticiones web y devuelve vistas (páginas web).
- **Ejemplo**:
  ```java
  @Controller
  public class UsuarioController {
      @GetMapping("/usuarios")
      public String listarUsuarios(Model model) {
          // Lógica del controlador
          return "usuarios"; // Nombre de la vista
      }
  }
  ```

#### `@RestController`
- **Qué hace**: Combina `@Controller` + `@ResponseBody`.
- **Para qué sirve**: Para APIs REST que devuelven datos en formato JSON/XML.
- **Ejemplo**:
  ```java
  @RestController
  @RequestMapping("/api/usuarios")
  public class UsuarioRestController {
      @GetMapping("/{id}")
      public Usuario obtenerUsuario(@PathVariable Long id) {
          // Devuelve directamente el objeto Usuario como JSON
          return usuarioService.findById(id);
      }
  }
  ```

### 2. **Anotaciones de Inyección de Dependencias**

#### `@Autowired`
- **Qué hace**: Inyecta automáticamente dependencias por tipo.
- **Para qué sirve**: Elimina la necesidad de crear manualmente las dependencias.
- **Dónde se usa**: Constructores, métodos setter, campos.
- **Ejemplo**:
  ```java
  @Service
  public class PedidoService {
      @Autowired
      private PedidoRepository repository;
  }
  ```

#### `@Qualifier`
- **Qué hace**: Especifica cuál bean inyectar cuando hay múltiples candidatos del mismo tipo.
- **Para qué sirve**: Resolver ambigüedad en la inyección de dependencias.
- **Ejemplo**:
  ```java
  @Service
  public class NotificacionService {
      @Autowired
      @Qualifier("emailService")  // Especifica cuál implementación usar
      private MensajeService mensajeService;
  }
  ```

#### `@Primary`
- **Qué hace**: Marca un bean como la opción predeterminada cuando hay múltiples candidatos.
- **Para qué sirve**: Establecer prioridad entre implementaciones.
- **Ejemplo**:
  ```java
  @Service
  @Primary  // Esta implementación será la preferida por defecto
  public class EmailServiceImpl implements MensajeService {
      // Implementación principal
  }
  ```

### 3. **Anotaciones de Configuración**

#### `@Configuration`
- **Qué hace**: Indica que la clase contiene configuración de beans.
- **Para qué sirve**: Definir beans mediante métodos anotados con `@Bean`.
- **Ejemplo**:
  ```java
  @Configuration
  public class DatabaseConfig {
      @Bean
      public DataSource dataSource() {
          return new HikariDataSource();
      }
  }
  ```

#### `@Bean`
- **Qué hace**: Define un bean dentro de una clase `@Configuration`.
- **Para qué sirve**: Crear beans de clases de terceros o configuraciones complejas.
- **Ejemplo**:
  ```java
  @Configuration
  public class AppConfig {
      @Bean
      public RestTemplate restTemplate() {
          return new RestTemplate();  // Bean de una clase externa
      }
  }
  ```

#### `@ComponentScan`
- **Qué hace**: Indica a Spring dónde buscar componentes para escanear.
- **Para qué sirve**: Configurar el escaneo automático de componentes.
- **Ejemplo**:
  ```java
  @Configuration
  @ComponentScan(basePackages = "com.ejemplo.servicios")
  public class AppConfig {
      // Spring escaneará el paquete especificado buscando @Component, @Service, etc.
  }
  ```

### 4. **Anotaciones del Ciclo de Vida**

#### `@PostConstruct`
- **Qué hace**: Ejecuta un método después de que el bean sea creado e inyectado.
- **Para qué sirve**: Inicialización personalizada del bean.
- **Ejemplo**:
  ```java
  @Service
  public class CacheService {
      @PostConstruct
      public void inicializarCache() {
          // Código de inicialización
          System.out.println("Cache inicializado");
      }
  }
  ```

#### `@PreDestroy`
- **Qué hace**: Ejecuta un método antes de que el bean sea destruido.
- **Para qué sirve**: Limpieza de recursos (cerrar conexiones, archivos, etc.).
- **Ejemplo**:
  ```java
  @Service
  public class DatabaseService {
      @PreDestroy
      public void cerrarConexiones() {
          // Código de limpieza
          System.out.println("Cerrando conexiones de base de datos");
      }
  }
  ```

### 5. **Anotaciones de Alcance (Scope)**

#### `@Scope`
- **Qué hace**: Define el alcance del bean (singleton, prototype, etc.).
- **Para qué sirve**: Controlar cómo Spring gestiona las instancias del bean.
- **Ejemplo**:
  ```java
  @Service
  @Scope("prototype")  // Nueva instancia cada vez que se solicite
  public class TemporaryService {
      // Cada inyección creará una nueva instancia
  }
  ```

### 6. **Anotaciones de Inicialización Diferida**

#### `@Lazy`
- **Qué hace**: Retrasa la creación del bean hasta que sea realmente necesario.
- **Para qué sirve**: Optimizar el tiempo de arranque de la aplicación.
- **Ejemplo**:
  ```java
  @Service
  @Lazy  // No se creará hasta que alguien lo necesite
  public class HeavyProcessingService {
      // Bean que consume muchos recursos
  }
  ```

### Resumen de Uso por Capas:

```java
// Capa de Controlador
@RestController
@RequestMapping("/api/productos")
public class ProductoController {
    @Autowired
    private ProductoService productoService;
}

// Capa de Servicio  
@Service
public class ProductoService {
    @Autowired
    private ProductoRepository productoRepository;
}

// Capa de Repositorio
@Repository
public class ProductoRepository {
    // Acceso a datos
}

// Configuración
@Configuration
public class AppConfig {
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
```

## Anotaciones principales en Spring

- `@Component`, `@Service`, `@Repository`, `@Controller`: Indican que una clase es un componente gestionado por Spring.
- `@Autowired`: Indica a Spring que debe inyectar la dependencia correspondiente.
- `@Qualifier`: Permite especificar cuál implementación inyectar cuando hay varias disponibles.

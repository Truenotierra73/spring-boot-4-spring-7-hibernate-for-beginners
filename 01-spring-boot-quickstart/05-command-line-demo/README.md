# ¿Qué es Maven?
Maven es una herramienta de gestión y automatización de proyectos Java. Permite compilar, probar, empaquetar y gestionar dependencias de forma sencilla.

## ¿Es necesario instalar Maven globalmente?
No es obligatorio instalar Maven en tu sistema. Los proyectos Spring Boot suelen incluir el Maven Wrapper (`mvnw` y `mvnw.cmd`), que permite ejecutar Maven sin instalarlo globalmente. Solo necesitas tener Java 25 o superior instalado.

## Empaquetar la aplicación
Para empaquetar tu aplicación en un archivo JAR ejecutable:

```
./mvnw clean package -DskipTests
```
O simplemente:
```
./mvnw package
```
Esto genera el archivo JAR en la carpeta `target/`.

## Ejecutar la aplicación empaquetada
Para ejecutar el JAR generado:
```
java -jar target/myfirtsapp-0.0.1-SNAPSHOT.jar
```

## Ejecutar la aplicación sin empaquetar
Puedes ejecutar la aplicación directamente (útil para desarrollo):
```
./mvnw spring-boot:run
```

---

Este README ahora incluye información sobre Maven, su instalación y los comandos básicos para empaquetar y ejecutar tu aplicación Spring Boot.

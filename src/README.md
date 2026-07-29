# Academic Events API

Este es el backend de nuestro Proyecto Integrador para la asignatura de Programación y Plataformas Web. Es una API REST para gestionar eventos académicos: categorías, eventos, sesiones e inscripciones, con autenticación JWT, roles, límites de peticiones con Redis y todo lo que pide la rúbrica.

Lo hicimos entre tres personas, cada quien encargado de una parte del backend:

- **Esteban Hernández** – autenticación (registro, login, refresh tokens) y los módulos de categorías y eventos.
- **Christian Astudillo** – módulo de sesiones (sessions), es decir, las charlas/actividades dentro de cada evento.
- **Alex** – módulo de inscripciones (registrations), donde los participantes se anotan a los eventos.

## Tecnologías

Java 21, Spring Boot 4.1.0, Gradle, PostgreSQL, Redis, Spring Security con JWT, Flyway para las migraciones, Spring Data JPA y Springdoc para la documentación OpenAPI/Swagger.
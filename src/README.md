# Academic Events API

Este es el backend de nuestro Proyecto Integrador para la asignatura de Programación y Plataformas Web. Es una API REST para gestionar eventos académicos: categorías, eventos, sesiones e inscripciones, con autenticación JWT, roles, límites de peticiones con Redis y todo lo que pide la rúbrica.

Lo hicimos entre tres personas, cada quien encargado de una parte del backend:

- **Esteban Hernández** – autenticación (registro, login, refresh tokens) y los módulos de categorías y eventos.
- **Christian Astudillo** – módulo de sesiones (sessions), es decir, las charlas/actividades dentro de cada evento.
- **Alex Paucar** – módulo de inscripciones (registrations), donde los participantes se anotan a los eventos.

## Tecnologías

Java 21, Spring Boot 4.1.0, Gradle, PostgreSQL, Redis, Spring Security con JWT, Flyway para las migraciones, Spring Data JPA y Springdoc para la documentación OpenAPI/Swagger.

## El proyecto ya está desplegado

No hace falta clonar nada para probarlo, ya está corriendo en Render:

- **Servidor:** https://academic-events-api-thyu.onrender.com
- **Swagger UI:** https://academic-events-api-thyu.onrender.com/api/swagger-ui/index.html
- **Health check:** https://academic-events-api-thyu.onrender.com/api/actuator/health

Una aclaración importante para quien lo revise: si entran directo a la URL raíz (sin `/api`) les va a salir un **404**, y si entran a `/api` a secas les va a salir un **401**. Ninguno de los dos es un error nuestro ni significa que el despliegue falló — es que toda la API vive bajo el contexto `/api`, y no hay ningún endpoint público registrado justo en la raíz. Las rutas reales son cosas como `/api/events`, `/api/categories`, `/api/swagger-ui/index.html`, etc.

### Credenciales para probar

**Swagger (autenticación básica, solo para entrar a la documentación):**
- Usuario: `evaluador`
- Contraseña: `evaluador12@`

**Login como administrador** (rol `ADMIN`, puede gestionar todo):
- Correo: `admin@academic.test`
- Contraseña: `Password123*`

**Login como usuario normal** (rol `PARTICIPANT`):
- Correo: `carlos.velez@academic.test`
- Contraseña: `Password123*`


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

Una aclaración importante: si entran directo a la URL raíz (sin `/api`) les va a salir un **404**, y si entran a `/api` a secas les va a salir un **401**. Ninguno de los dos es un error nuestro ni significa que el despliegue falló — es que toda la API vive bajo el contexto `/api`, y no hay ningún endpoint público registrado justo en la raíz. Las rutas reales son cosas como `/api/events`, `/api/categories`, `/api/swagger-ui/index.html`, etc.

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

## Cómo correrlo en local

Si de todas formas quieren levantarlo en su máquina:

```bash
git clone https://github.com/Esteban12f/ppw-icc-backend-proyecto-final.git
cd ppw-icc-backend-proyecto-final
docker compose up -d
./gradlew bootRun
```

Con eso deberían tener PostgreSQL en el puerto 5435 y Redis en el 6380 corriendo vía Docker, y la API en `http://localhost:8080/api`. El perfil `dev` usa Flyway para crear el esquema automáticamente, así que no hay que correr ningún script a mano en local.

## Qué hace cada módulo

### Autenticación (`/auth`)

Registro, login, renovación de token (refresh) y logout. Las contraseñas se guardan con BCrypt, y los refresh tokens se rotan y se guardan hasheados (nunca el token en texto plano) en la tabla `refresh_tokens`.

### Categorías y eventos (`/categories`, `/events`)

CRUD básico de eventos, con modalidades (presencial, virtual, híbrida), estados (borrador, publicado, finalizado, cancelado) y control de cupos. El organizador de un evento sale siempre del token, nunca del body, para que nadie pueda crear un evento a nombre de otra persona.

### Sesiones (`/sessions`, `/events/{eventId}/sessions`)

Esta es la parte que hice yo. Una sesión es una charla o actividad puntual dentro de un evento (por ejemplo, "Charla de arquitectura de software" dentro del evento "Semana de la Ingeniería").

Los 6 endpoints:

| Método | Ruta | Quién puede |
|---|---|---|
| GET | `/events/{eventId}/sessions` | Cualquiera |
| GET | `/sessions/{id}` | Cualquiera |
| GET | `/sessions/upcoming` | Cualquiera |
| POST | `/events/{eventId}/sessions` | ADMIN u ORGANIZER dueño del evento |
| PUT | `/sessions/{id}` | ADMIN u ORGANIZER dueño del evento |
| DELETE | `/sessions/{id}` | ADMIN u ORGANIZER dueño del evento |

Reglas que se validan:

- Solo un ADMIN o el ORGANIZER dueño del evento pueden crear, editar o borrar sesiones. Si un organizador intenta tocar sesiones de un evento que no es suyo, le responde 403.
- La sesión tiene que caer dentro del rango de fechas del evento (no puedes crear una charla que empiece antes de que arranque el evento, por ejemplo).
- No se puede agregar ni editar sesiones si el evento ya está finalizado o cancelado.
- No se permite repetir el mismo título y la misma hora de inicio dos veces en el mismo evento (responde 409 si se intenta).
- El borrado es físico de verdad (no como en eventos, que usan borrado lógico), porque la tabla `sessions` no tiene una columna para eso.
- Los listados soportan paginación y ordenamiento (`?page=0&size=20&sort=startAt,desc`).

### Inscripciones (`/registrations`)

Esta parte es de Alex. Permite que un participante se inscriba a un evento, consulte sus propias inscripciones, las cancele, y que el organizador confirme o rechace inscripciones de sus eventos.



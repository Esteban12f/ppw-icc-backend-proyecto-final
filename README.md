# Academic Events API

API REST para la gestión de eventos académicos: categorías, eventos, sesiones e inscripciones, con autenticación JWT, roles, límites de peticiones con Redis, reportes descargables y todo lo que pide la rúbrica del Proyecto Integrador de Programación y Plataformas Web.

**Repositorio:** https://github.com/Esteban12f/ppw-icc-backend-proyecto-final

Lo hicimos entre tres personas, cada quien encargado de una parte del backend:

- **Esteban Hernández** – autenticación (registro, login, refresh tokens), categorías y eventos.
- **Christian Astudillo** – sesiones (sessions): las charlas o actividades dentro de cada evento.
- **Alex Paucar** – inscripciones, rate limiting, reportes, CORS, manejo de excepciones y despliegue.

---

## El proyecto ya está desplegado

No hace falta clonar nada para probarlo, ya está corriendo en Render:

- **Servidor:** https://academic-events-api-thyu.onrender.com
- **Swagger UI:** https://academic-events-api-thyu.onrender.com/api/swagger-ui/index.html
- **Health check:** https://academic-events-api-thyu.onrender.com/api/actuator/health

Si entran directo a la URL raíz (sin `/api`) les va a salir un **404**, y si entran a `/api` a secas les va a salir un **401**. Ninguno de los dos es un error ni significa que el despliegue falló — toda la API vive bajo el contexto `/api`, y no hay ningún endpoint público registrado justo en la raíz.

### Credenciales para probar

**Swagger (autenticación básica):**
- Usuario: `evaluador`
- Contraseña: `evaluador12@`

**Login como administrador** (rol `ADMIN`):
- Correo: `admin@academic.test`
- Contraseña: `Password123*`

**Login como usuario normal** (rol `PARTICIPANT`):
- Correo: `carlos.velez@academic.test`
- Contraseña: `Password123*`

---

## Tecnologías

- Java 21 + Spring Boot 4.1.0
- PostgreSQL 17 (Flyway para migraciones)
- Redis 7 (rate limiting y bloqueo de intentos de login)
- JWT (access token + refresh token con rotación)
- Swagger / OpenAPI 3
- Apache POI + OpenPDF (reportes Excel y PDF)
- Docker + Render (despliegue)

---

## Requisitos previos

- Java 21
- Docker Desktop
- Git

---

## Instalación y ejecución local

**1. Clonar el repositorio**

```bash
git clone https://github.com/Esteban12f/ppw-icc-backend-proyecto-final.git
cd ppw-icc-backend-proyecto-final
```

**2. Levantar PostgreSQL y Redis con Docker**

```bash
docker run -d \
  --name academic-events-postgres \
  -e POSTGRES_DB=academic_events_db \
  -e POSTGRES_USER=ups \
  -e POSTGRES_PASSWORD=ups123 \
  -p 5435:5432 \
  postgres:17-alpine

docker run -d \
  --name academic-events-redis \
  -p 6380:6379 \
  redis:7-alpine
```

**3. Configurar variables de entorno**

```bash
cp .env.example .env
```

Los valores por defecto de `.env.example` ya coinciden con los contenedores del paso anterior.

**4. Ejecutar la aplicación**

```bash
chmod +x gradlew
./gradlew bootRun
```

Flyway ejecuta las migraciones automáticamente al iniciar. La aplicación queda disponible en `http://localhost:8080/api`.

---

## Variables de entorno

| Variable | Descripción | Ejemplo dev |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Perfil activo (`dev` o `prod`) | `dev` |
| `PORT` | Puerto del servidor | `8080` |
| `DB_URL` | URL de conexión a PostgreSQL | `jdbc:postgresql://localhost:5435/academic_events_db` |
| `DB_USERNAME` | Usuario de la BD | `ups` |
| `DB_PASSWORD` | Contraseña de la BD | `ups123` |
| `REDIS_HOST` | Host de Redis | `localhost` |
| `REDIS_PORT` | Puerto de Redis | `6380` |
| `REDIS_PASSWORD` | Contraseña de Redis (vacío en dev) | |
| `JWT_SECRET` | Clave secreta JWT (mínimo 256 bits) | (ver `.env.example`) |
| `JWT_ACCESS_EXPIRATION` | Duración del access token (ms) | `1800000` (30 min) |
| `JWT_REFRESH_EXPIRATION` | Duración del refresh token (ms) | `604800000` (7 días) |
| `JWT_ISSUER` | Emisor del JWT | `academic-events-api` |
| `ALLOWED_ORIGINS` | Orígenes permitidos para CORS | `http://localhost:3000` |

---

## Qué hace cada módulo

### Autenticación (`/auth`)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/auth/register` | Público | Registrar usuario |
| `POST` | `/api/auth/login` | Público | Iniciar sesión |
| `POST` | `/api/auth/refresh` | Público | Renovar access token |
| `POST` | `/api/auth/logout` | Público | Revocar refresh token |

Las contraseñas se guardan con BCrypt, y los refresh tokens se rotan y se guardan hasheados (nunca en texto plano) en la tabla `refresh_tokens`.

### Categorías y eventos (`/categories`, `/events`)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/api/events` | Público | Listar eventos |
| `GET` | `/api/events/{id}` | Público | Obtener evento |
| `POST` | `/api/events` | ADMIN / ORGANIZER | Crear evento |
| `PUT` | `/api/events/{id}` | ADMIN / ORGANIZER dueño | Actualizar evento |
| `DELETE` | `/api/events/{id}` | ADMIN / ORGANIZER dueño | Eliminar evento |

Modalidades (presencial, virtual, híbrida), estados (borrador, publicado, finalizado, cancelado) y control de cupos. El organizador de un evento sale siempre del token, nunca del body, para que nadie pueda crear un evento a nombre de otra persona.

### Sesiones (`/sessions`, `/events/{eventId}/sessions`)

Esta es la parte que hice yo. Una sesión es una charla o actividad puntual dentro de un evento (por ejemplo, "Charla de arquitectura de software" dentro del evento "Semana de la Ingeniería").

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/api/events/{id}/sessions` | Público | Listar sesiones de un evento |
| `POST` | `/api/events/{id}/sessions` | ADMIN / ORGANIZER dueño | Crear sesión |
| `GET` | `/api/sessions/{id}` | Público | Obtener sesión |
| `PUT` | `/api/sessions/{id}` | ADMIN / ORGANIZER dueño | Actualizar sesión |
| `DELETE` | `/api/sessions/{id}` | ADMIN / ORGANIZER dueño | Eliminar sesión |
| `GET` | `/api/sessions/upcoming` | Público | Próximas sesiones |

Reglas que se validan:

- Solo un ADMIN o el ORGANIZER dueño del evento pueden crear, editar o borrar sesiones. Si un organizador intenta tocar sesiones de un evento que no es suyo, le responde 403.
- La sesión tiene que caer dentro del rango de fechas del evento.
- No se puede agregar ni editar sesiones si el evento ya está finalizado o cancelado.
- No se permite repetir el mismo título y la misma hora de inicio dos veces en el mismo evento (409 si se intenta).
- El borrado es físico de verdad (no como en eventos, que usan borrado lógico), porque la tabla `sessions` no tiene columna para eso.
- Los listados soportan paginación y ordenamiento (`?page=0&size=20&sort=startAt,desc`).

### Inscripciones (`/registrations`)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/events/{id}/registrations` | PARTICIPANT | Inscribirse |
| `GET` | `/api/registrations/me` | Autenticado | Mis inscripciones |
| `GET` | `/api/registrations/{id}` | Dueño / ORGANIZER / ADMIN | Ver inscripción |
| `DELETE` | `/api/registrations/{id}` | Dueño / ADMIN | Cancelar inscripción |
| `GET` | `/api/events/{id}/registrations` | ADMIN / ORGANIZER | Listar inscritos |
| `PATCH` | `/api/registrations/{id}/status` | ADMIN / ORGANIZER | Cambiar estado |

Un participante solo puede tener una inscripción por evento, y solo eventos `PUBLISHED` aceptan inscripciones.

### Reportes (`/reports`)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/api/reports/events/{id}/registrations.pdf` | ADMIN / ORGANIZER | Listado en PDF |
| `GET` | `/api/reports/events/{id}/registrations.xlsx` | ADMIN / ORGANIZER | Listado en Excel |
| `GET` | `/api/registrations/{id}/certificate.pdf` | PARTICIPANT dueño | Comprobante PDF |

Los reportes de listado aceptan parámetros opcionales `from` y `to` en formato ISO 8601.

---

## Roles y permisos

| Rol | Puede |
|---|---|
| `PARTICIPANT` | Inscribirse, ver sus inscripciones, descargar su comprobante |
| `ORGANIZER` | Crear y gestionar eventos propios, ver inscritos de sus eventos, gestionar sus sesiones |
| `ADMIN` | Todo lo anterior sin restricción de propiedad |

Al registrarse, todos los usuarios reciben el rol `PARTICIPANT` por defecto.

---

## Rate limiting

| Endpoint | Límite |
|---|---|
| `POST /auth/login` | 5 intentos / minuto por IP y por correo |
| `POST /auth/register` | 3 registros / hora por IP |
| Endpoints públicos | 60 solicitudes / minuto por IP |
| Endpoints autenticados | 120 solicitudes / minuto por usuario |
| Endpoints de reportes | 5 solicitudes / minuto por usuario |

Las cuentas se bloquean 15 minutos después de 5 intentos fallidos consecutivos. Las respuestas `429` incluyen el header `Retry-After`.

---

## Documentación interactiva (Swagger)

Con la aplicación corriendo: [text](http://localhost:8080/api/swagger-ui/index.html)

Para probar endpoints protegidos: hacer `POST /api/auth/login`, copiar el `accessToken`, clic en `Authorize` y pegarlo (sin el prefijo `Bearer`).

---

## Pruebas

```bash
./gradlew test
```

El reporte HTML queda en `build/reports/tests/test/index.html`. El módulo de sessions además tiene pruebas de integración con MockMvc que verifican los códigos de respuesta HTTP reales y la validación de los datos de entrada.

---

## Cómo desplegamos esto

Terminamos usando Render con un Blueprint (`render.yaml`), que levanta tres cosas juntas: el web service con Docker, una base PostgreSQL administrada y un Redis (técnicamente el "Key Value" de Render, pero compatible con clientes Redis normales). El Dockerfile es multietapa: una etapa compila el proyecto con Gradle sobre una imagen JDK, y la etapa final solo copia el jar ya compilado sobre una imagen JRE más liviana, corriendo con un usuario sin privilegios de administrador.

**Variables a configurar manualmente en Render:**

| Variable | Valor |
|---|---|
| `DB_URL` | URL interna de PostgreSQL de Render |
| `DB_USERNAME` | Usuario de PostgreSQL de Render |
| `DB_PASSWORD` | Contraseña de PostgreSQL de Render |
| `REDIS_HOST` | Host interno de Redis de Render |
| `REDIS_PORT` | Puerto de Redis de Render |
| `REDIS_PASSWORD` | Contraseña de Redis de Render |
| `ALLOWED_ORIGINS` | URL del frontend desplegado |

`JWT_SECRET` se genera automáticamente por Render.

### Problemas que nos encontramos y cómo los resolvimos

**El contenedor no arrancaba (`exec format error`).** El script `docker-entrypoint.sh` se había guardado con saltos de línea de Windows (CRLF) en vez de Linux (LF), y Linux no lo podía ejecutar. Se arregló guardándolo en UTF-8 sin BOM con saltos LF, y agregando `.gitattributes` para que Git no lo vuelva a convertir mal.

**El driver de PostgreSQL rechazaba la URL de conexión.** Render entrega la URL como `postgresql://usuario:contraseña@host/base`, pero el driver JDBC necesita `jdbc:postgresql://host/base`, sin las credenciales metidas ahí. Se ajustó el entrypoint para transformar esa URL antes de arrancar la aplicación, pasando usuario y contraseña por separado.

**Swagger no pedía usuario y contraseña aunque las variables ya estaban en Render.** Tener `SWAGGER_USERNAME`/`SWAGGER_PASSWORD` configuradas no activa ninguna seguridad por sí sola — hubo que agregar una configuración de Spring Security que las use de verdad, activa solo cuando el perfil es `prod`.

**Render no se actualizaba después de un push.** El Blueprint apuntaba a la rama `deploy/render`, y los cambios se estaban subiendo a otra rama. Al hacer merge y push a `deploy/render`, Render desplegó automáticamente.

La estructura de la base de datos la sigue manejando Flyway con `ddl-auto: validate` (nunca `update`), y la migración inicial ya aplicada no se debe tocar — cualquier cambio nuevo va en una migración V2, V3, etc.

---

## Integrantes

| Nombre | Módulos |
|---|---|
| Esteban Hernández | Auth, Categorías, Eventos |
| Christian Astudillo | Sesiones |
| Alex Paucar | Inscripciones, Rate Limiting, Reportes, CORS, Excepciones, Deployment |
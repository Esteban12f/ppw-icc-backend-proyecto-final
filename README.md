# Academic Events API

API REST segura para la gestión de eventos académicos.

**Repositorio:** https://github.com/Esteban12f/ppw-icc-backend-proyecto-final

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

Crear un archivo `.env` en la raíz del proyecto basándose en `.env.example`:

```bash
cp .env.example .env
```

Editar `.env` con los valores correctos. Los valores por defecto de `.env.example` ya coinciden con los contenedores Docker del paso anterior.

**4. Ejecutar la aplicación**

```bash
chmod +x gradlew
./gradlew bootRun
```

Flyway ejecuta las migraciones automáticamente al iniciar. La aplicación queda disponible en:

```
http://localhost:8080/api
```

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
| `REDIS_PASSWORD` | Contraseña de Redis (vacío en dev) | `` |
| `JWT_SECRET` | Clave secreta JWT (mínimo 256 bits) | (ver `.env.example`) |
| `JWT_ACCESS_EXPIRATION` | Duración del access token (ms) | `1800000` (30 min) |
| `JWT_REFRESH_EXPIRATION` | Duración del refresh token (ms) | `604800000` (7 días) |
| `JWT_ISSUER` | Emisor del JWT | `academic-events-api` |
| `ALLOWED_ORIGINS` | Orígenes permitidos para CORS | `http://localhost:3000` |

---

## Endpoints principales

La API usa el prefijo `/api`. Los endpoints protegidos requieren:

```
Authorization: Bearer <access_token>
```

### Autenticación

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/auth/register` | Público | Registrar usuario |
| `POST` | `/api/auth/login` | Público | Iniciar sesión |
| `POST` | `/api/auth/refresh` | Público | Renovar access token |
| `POST` | `/api/auth/logout` | Público | Revocar refresh token |

### Eventos

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/api/events` | Público | Listar eventos |
| `GET` | `/api/events/{id}` | Público | Obtener evento |
| `POST` | `/api/events` | ADMIN / ORGANIZER | Crear evento |
| `PUT` | `/api/events/{id}` | ADMIN / ORGANIZER dueño | Actualizar evento |
| `DELETE` | `/api/events/{id}` | ADMIN / ORGANIZER dueño | Eliminar evento |

### Sesiones

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/api/events/{id}/sessions` | Público | Listar sesiones |
| `POST` | `/api/events/{id}/sessions` | ADMIN / ORGANIZER | Crear sesión |
| `GET` | `/api/sessions/{id}` | Público | Obtener sesión |
| `PUT` | `/api/sessions/{id}` | ADMIN / ORGANIZER | Actualizar sesión |
| `DELETE` | `/api/sessions/{id}` | ADMIN / ORGANIZER | Eliminar sesión |
| `GET` | `/api/sessions/upcoming` | Público | Próximas sesiones |

### Inscripciones

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/events/{id}/registrations` | PARTICIPANT | Inscribirse |
| `GET` | `/api/registrations/me` | Autenticado | Mis inscripciones |
| `GET` | `/api/registrations/{id}` | Dueño / ORGANIZER / ADMIN | Ver inscripción |
| `DELETE` | `/api/registrations/{id}` | Dueño / ADMIN | Cancelar inscripción |
| `GET` | `/api/events/{id}/registrations` | ADMIN / ORGANIZER | Listar inscritos |
| `PATCH` | `/api/registrations/{id}/status` | ADMIN / ORGANIZER | Cambiar estado |

### Reportes

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
| `ORGANIZER` | Crear y gestionar eventos propios, ver inscritos de sus eventos |
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

Con la aplicación corriendo:

```
http://localhost:8080/api/swagger-ui/index.html
```

Para probar endpoints protegidos:
1. Hacer `POST /api/auth/login` y copiar el `accessToken`
2. Clic en el botón `Authorize`
3. Pegar el token (sin el prefijo `Bearer`)

---

## Pruebas

```bash
./gradlew test
```

El reporte HTML queda en `build/reports/tests/test/index.html`.

---

## Despliegue en Render

El proyecto incluye `Dockerfile` multi-stage y `render.yaml` para despliegue automático.

**Servicios necesarios en Render Dashboard:**
- PostgreSQL
- Redis
- Web Service conectado a este repositorio

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

---

## Integrantes

| Nombre | Módulos |
|---|---|
| Esteban | Auth, Categorías, Eventos |
| Christian | Sesiones |
| Aleks | Inscripciones, Rate Limiting, Reportes, CORS, Excepciones, Deployment |

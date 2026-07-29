#!/bin/sh

set -eu

if [ -z "${DB_URL:-}" ]; then
    echo "ERROR: La variable DB_URL no está definida." >&2
    exit 1
fi

# Render entrega postgresql://...
# JDBC necesita jdbc:postgresql://...
case "$DB_URL" in
    jdbc:postgresql://*)
        ;;
    postgresql://*)
        export DB_URL="jdbc:${DB_URL}"
        ;;
    postgres://*)
        export DB_URL="jdbc:postgresql://${DB_URL#postgres://}"
        ;;
    *)
        echo "ERROR: DB_URL no tiene un formato PostgreSQL válido." >&2
        exit 1
        ;;
esac

exec java -jar /app/app.jar
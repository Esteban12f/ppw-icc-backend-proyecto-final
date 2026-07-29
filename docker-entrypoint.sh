#!/bin/sh

set -eu

if [ -z "${DB_URL:-}" ]; then
    echo "ERROR: La variable DB_URL no está definida." >&2
    exit 1
fi

case "$DB_URL" in
    jdbc:postgresql://*)
        # Ya tiene formato JDBC correcto.
        ;;

    postgresql://*|postgres://*)
        # Render entrega:
        # postgresql://usuario:password@host:puerto/base
        #
        # PostgreSQL JDBC necesita:
        # jdbc:postgresql://host:puerto/base
        #
        # DB_USERNAME y DB_PASSWORD ya se proporcionan por separado.
        URL_WITHOUT_SCHEME="${DB_URL#*://}"
        HOST_PORT_DATABASE="${URL_WITHOUT_SCHEME#*@}"

        export DB_URL="jdbc:postgresql://${HOST_PORT_DATABASE}"
        ;;

    *)
        echo "ERROR: DB_URL no tiene un formato PostgreSQL válido." >&2
        exit 1
        ;;
esac

echo "URL de PostgreSQL convertida correctamente al formato JDBC."

exec java -jar /app/app.jar
#!/usr/bin/env sh
set -eu

: "${SPRING_DATASOURCE_URL:=jdbc:postgresql://${DB_HOST:-host.docker.internal}:${DB_PORT:-5432}/${DB_NAME:-moviedb}}"
: "${SPRING_DATASOURCE_USERNAME:=${DB_USER:-postgres}}"
: "${SPRING_DATASOURCE_PASSWORD:=${DB_PASSWORD:?DB_PASSWORD must be set.}}"
: "${JWT_SECRET:?JWT_SECRET must be set and at least 32 characters long.}"

export SPRING_DATASOURCE_URL
export SPRING_DATASOURCE_USERNAME
export SPRING_DATASOURCE_PASSWORD
export JWT_SECRET

exec java -jar /app/fabflix.jar

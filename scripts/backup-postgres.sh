#!/bin/sh
set -eu
umask 077
: "${PGHOST:?Configure PGHOST}" "${PGDATABASE:?Configure PGDATABASE}" "${PGUSER:?Configure PGUSER}"
destino=${BACKUP_DIR:-/backup}
mkdir -p "$destino"
arquivo="$destino/criare3d-$(date -u +%Y%m%dT%H%M%SZ).dump"
pg_dump --format=custom --no-owner --no-acl --file="$arquivo"
pg_restore --list "$arquivo" > /dev/null
(cd "$destino" && sha256sum "$(basename "$arquivo")") > "$arquivo.sha256"
printf '%s\n' "$arquivo"

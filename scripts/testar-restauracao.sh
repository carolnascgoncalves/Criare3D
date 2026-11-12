#!/bin/sh
set -eu
: "${PGHOST:?Configure PGHOST}" "${PGDATABASE:?Configure PGDATABASE}" "${PGUSER:?Configure PGUSER}"
arquivo=${1:?Informe o arquivo de backup}
teste=${BACKUP_TEST_DB:-criare3d_restore_test}
case "$teste" in *[!a-z0-9_]*|'') echo 'Nome de banco de ensaio inválido' >&2; exit 1;; esac
case "$teste" in *_restore_test) ;; *) echo 'O banco de ensaio deve terminar em _restore_test' >&2; exit 1;; esac
[ "$teste" != "$PGDATABASE" ] || { echo 'O ensaio precisa de outro banco' >&2; exit 1; }
[ -f "$arquivo" ] || { echo 'Backup não encontrado' >&2; exit 1; }
if [ -f "$arquivo.sha256" ]; then
    (cd "$(dirname "$arquivo")" && sha256sum -c "$(basename "$arquivo").sha256")
fi
# createdb falha se o destino existir. Nunca restaura por cima de um banco existente.
createdb -- "$teste"
trap 'dropdb --if-exists -- "$teste"' EXIT HUP INT TERM
pg_restore --exit-on-error --no-owner --no-acl --dbname="$teste" "$arquivo"
quantidade=$(psql -XAt --dbname="$teste" -v ON_ERROR_STOP=1 -c "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'")
[ "$quantidade" -gt 0 ] || { echo 'O backup não contém tabelas' >&2; exit 1; }
printf 'Restauração concluída: %s tabelas. Banco de ensaio descartado ao sair.\n' "$quantidade"

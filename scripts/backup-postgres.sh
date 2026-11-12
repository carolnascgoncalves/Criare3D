#!/bin/sh
set -eu
umask 077
: "${PGHOST:?Configure PGHOST}" "${PGDATABASE:?Configure PGDATABASE}" "${PGUSER:?Configure PGUSER}"
destino=${BACKUP_DIR:-/backup}
mkdir -p "$destino"
arquivo="$destino/criare3d-$(date -u +%Y%m%dT%H%M%SZ).dump"
temporario=$(mktemp -d)
owner=''
limpar() {
    if [ -n "$owner" ]; then kill "$owner" 2>/dev/null || :; wait "$owner" 2>/dev/null || :; fi
    rm -f "$temporario/comandos" "$temporario/origem" "$temporario/erro"
    rmdir "$temporario"
}
trap limpar EXIT
trap 'exit 1' HUP INT TERM
mkfifo "$temporario/comandos"
# Esta conexão mantém o snapshot vivo até o fim do dump. As contagens pertencem
# ao MESMO snapshot: vendas concorrentes não provocam divergências falsas.
psql -XqAt -v ON_ERROR_STOP=1 < "$temporario/comandos" > "$temporario/origem" 2> "$temporario/erro" &
owner=$!
exec 3> "$temporario/comandos"
printf '%s\n' 'BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY;' 'SELECT pg_export_snapshot();' >&3
cat >&3 <<'SQL'
SELECT 'calculo_historico|' || count(*) FROM calculo_historico
UNION ALL SELECT 'embalagem|' || count(*) FROM embalagem
UNION ALL SELECT 'filamento|' || count(*) FROM filamento
UNION ALL SELECT 'impressora|' || count(*) FROM impressora
UNION ALL SELECT 'item_calculo_historico|' || count(*) FROM item_calculo_historico
UNION ALL SELECT 'pedido_orcamento|' || count(*) FROM pedido_orcamento
UNION ALL SELECT 'item_pedido|' || count(*) FROM item_pedido
UNION ALL SELECT 'foto_item_pedido|' || count(*) FROM foto_item_pedido
UNION ALL SELECT 'peca|' || count(*) FROM peca
UNION ALL SELECT 'tenant|' || count(*) FROM tenant
UNION ALL SELECT 'usuario|' || count(*) FROM usuario
UNION ALL SELECT 'venda_calculo|' || count(*) FROM venda_calculo
ORDER BY 1;
\echo SNAPSHOT_PRONTO
SQL
espera=0
until grep -q '^SNAPSHOT_PRONTO$' "$temporario/origem"; do
    if ! kill -0 "$owner" 2>/dev/null || [ "$espera" -ge 120 ]; then
        echo 'Não foi possível preparar o snapshot do backup.' >&2
        exit 1
    fi
    espera=$((espera + 1))
    sleep 1
done
snapshot=$(head -n 1 "$temporario/origem")
case "$snapshot" in ''|*[!0-9A-Fa-f-]*) echo 'Snapshot inválido' >&2; exit 1;; esac
grep '^[a-z_]*|[0-9][0-9]*$' "$temporario/origem" > "$arquivo.rows"
[ "$(wc -l < "$arquivo.rows")" -eq 12 ] || { echo 'Contagens incompletas' >&2; exit 1; }
pg_dump --snapshot="$snapshot" --format=custom --no-owner --no-acl --file="$arquivo"
printf 'ROLLBACK;\n\\q\n' >&3
exec 3>&-
wait "$owner"
owner=''
pg_restore --list "$arquivo" > /dev/null
(cd "$destino" && sha256sum "$(basename "$arquivo")" "$(basename "$arquivo").rows") > "$arquivo.sha256"
printf '%s\n' "$arquivo"

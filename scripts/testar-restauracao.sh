#!/bin/sh
set -eu
: "${PGHOST:?Configure PGHOST}" "${PGDATABASE:?Configure PGDATABASE}" "${PGUSER:?Configure PGUSER}"
arquivo=${1:?Informe o arquivo de backup}
teste=${BACKUP_TEST_DB:-criare3d_restore_test}
case "$teste" in *[!a-z0-9_]*|'') echo 'Nome de banco de ensaio inválido' >&2; exit 1;; esac
case "$teste" in *_restore_test) ;; *) echo 'O banco de ensaio deve terminar em _restore_test' >&2; exit 1;; esac
[ "$teste" != "$PGDATABASE" ] || { echo 'O ensaio precisa de outro banco' >&2; exit 1; }
[ -f "$arquivo" ] || { echo 'Backup não encontrado' >&2; exit 1; }
[ -f "$arquivo.rows" ] || { echo 'Manifesto de contagens não encontrado' >&2; exit 1; }
if [ -f "$arquivo.sha256" ]; then
    (cd "$(dirname "$arquivo")" && sha256sum -c "$(basename "$arquivo").sha256")
fi
# createdb falha se o destino existir. Nunca restaura por cima de um banco existente.
createdb -- "$teste"
trap 'dropdb --if-exists -- "$teste"' EXIT HUP INT TERM
pg_restore --exit-on-error --no-owner --no-acl --dbname="$teste" "$arquivo"
temporario=$(mktemp)
limpar() { rm -f "$temporario"; dropdb --if-exists -- "$teste"; }
trap limpar EXIT
psql -XqAt --dbname="$teste" -v ON_ERROR_STOP=1 > "$temporario" <<'SQL'
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
SQL
if ! cmp -s "$arquivo.rows" "$temporario"; then
    echo 'A restauração diverge das contagens do snapshot original.' >&2
    exit 1
fi
printf 'Restauração conferida: contagens idênticas nas doze tabelas principais.\n'

package br.com.criare3d.service;

import br.com.criare3d.models.*;
import br.com.criare3d.repository.*;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AcaoEmLoteService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private final ProducaoService producaoService;
    private final VendaService vendaService;
    public AcaoEmLoteService(ProducaoService producaoService, VendaService vendaService) {
        this.producaoService = producaoService;
        this.vendaService = vendaService;
    }

    public ResultadoOperacao registrarVendasEmLote(Collection<UUID> ids, Integer unidadesPorLote) {
        if (ids == null || ids.isEmpty()) return ResultadoOperacao.NENHUM_SELECIONADO;
        if (unidadesPorLote == null || unidadesPorLote < 1) return ResultadoOperacao.QUANTIDADE_VENDA_INVALIDA;
        List<UUID> unicos = ids.stream().filter(java.util.Objects::nonNull).distinct().sorted().toList();
        if (unicos.isEmpty()) return ResultadoOperacao.NENHUM_SELECIONADO;
        for (UUID id : unicos) {
            ResultadoOperacao resultado = vendaService.registrarVenda(id, unidadesPorLote);
            if (!ResultadoOperacao.OK.equals(resultado)) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return resultado;
            }
        }
        return ResultadoOperacao.OK;
    }

    public ResultadoOperacao atualizarStatusEmLote(Collection<UUID> ids, String acao) {
        if (ids == null || ids.isEmpty()) return ResultadoOperacao.NENHUM_SELECIONADO;
        if (acao == null || !List.of("PRODUZIR", "VENDER", "DESFAZER_PRODUCAO", "CANCELAR_VENDA").contains(acao)) {
            return ResultadoOperacao.ACAO_INVALIDA;
        }

        List<UUID> unicos = ids.stream().filter(java.util.Objects::nonNull).distinct().sorted().toList();
        if (unicos.isEmpty()) return ResultadoOperacao.NENHUM_SELECIONADO;
        for (UUID id : unicos) {
            ResultadoOperacao resultado = switch (acao) {
                case "PRODUZIR" -> producaoService.atualizarProduzido(id, true);
                case "VENDER" -> vendaService.atualizarVendido(id, true);
                case "DESFAZER_PRODUCAO" -> producaoService.atualizarProduzido(id, false);
                case "CANCELAR_VENDA" -> vendaService.cancelarVendas(id);
                default -> ResultadoOperacao.ACAO_INVALIDA;
            };
            if (!ResultadoOperacao.OK.equals(resultado)) {
                // Não deixa uma seleção parcialmente aplicada se algum lote falhar.
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return resultado;
            }
        }
        return ResultadoOperacao.OK;
    }
}

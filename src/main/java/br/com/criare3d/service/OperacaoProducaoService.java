package br.com.criare3d.service;

import br.com.criare3d.dto.AjusteEstoqueCalculoResponse;
import br.com.criare3d.models.*;
import br.com.criare3d.repository.*;
import br.com.criare3d.service.interfaces.FilamentoService;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Executado na transação da fachada CalculoHistoricoServiceImp. */
final class OperacaoProducaoService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private final CalculoHistoricoRepository calculoHistoricoRepository;
    private final VendaCalculoRepository vendaCalculoRepository;
    private final FilamentoService filamentoService;
    OperacaoProducaoService(CalculoHistoricoRepository historicos, VendaCalculoRepository vendas, FilamentoService filamentos) {
        this.calculoHistoricoRepository = historicos;
        this.vendaCalculoRepository = vendas;
        this.filamentoService = filamentos;
    }
    public String atualizarProduzido(UUID id, boolean produzido) {
        CalculoHistorico bloqueado = calculoHistoricoRepository.bloquearPorId(id).orElse(null);
        if (bloqueado == null) return "naoEncontrado";
        List<AjusteEstoqueCalculoResponse> dados =
                calculoHistoricoRepository.buscarDadosParaAtualizarStatus(id);
        if (dados.isEmpty()) return "naoEncontrado";

        AjusteEstoqueCalculoResponse historico = dados.get(0);
        boolean jaProduzido = Boolean.TRUE.equals(historico.produzido());
        if (jaProduzido == produzido) return "ok";

        if (!produzido) {
            CalculoHistorico calculo = calculoHistoricoRepository.bloquearPorId(id).orElse(null);
            if (calculo == null) return "naoEncontrado";
            if (Boolean.TRUE.equals(historico.vendido())
                    || valorOuZero(calculo.getUnidadesVendidas()) > 0
                    || vendaCalculoRepository.existeVendaAtiva(id)) {
                return "vendaAtiva";
            }
        }

        if (produzido && !ajustarEstoque(dados, false)) {
            return "estoqueInsuficiente";
        }
        if (!produzido) ajustarEstoque(dados, true);

        bloqueado.setProduzido(produzido);
        bloqueado.setDataProducao(produzido ? LocalDateTime.now(ZoneId.of("America/Sao_Paulo")) : null);
        return "ok";
    }

    public String atualizarVendido(UUID id, boolean vendido) {
        CalculoHistorico calculo = calculoHistoricoRepository.bloquearPorId(id).orElse(null);
        if (calculo == null) return "naoEncontrado";
        if (!vendido) return cancelarVendas(id);
        int restantes = Math.max(0, calculo.getUnidades() - valorOuZero(calculo.getUnidadesVendidas()));
        return restantes == 0 ? "ok" : registrarVenda(id, restantes);
    }

    public String registrarVenda(UUID id, Integer unidades) {
        return registrarVenda(id, unidades, null, null, null);
    }

    public String registrarVenda(UUID id, Integer unidades, BigDecimal preco, BigDecimal desconto, BigDecimal taxas) {
        for (BigDecimal valor : new BigDecimal[]{preco, desconto, taxas}) {
            if (valor != null && (valor.signum() < 0 || valor.compareTo(new BigDecimal("99999999")) > 0)) return "valorVendaInvalido";
        }
        if (unidades == null || unidades < 1) return "quantidadeVendaInvalida";
        CalculoHistorico calculo = calculoHistoricoRepository.bloquearPorId(id).orElse(null);
        if (calculo == null) return "naoEncontrado";

        int vendidas = valorOuZero(calculo.getUnidadesVendidas());
        int restantes = calculo.getUnidades() - vendidas;
        if (unidades > restantes) return "quantidadeVendaAcimaDisponivel";

        if (!Boolean.TRUE.equals(calculo.getProduzido())) return "producaoNecessaria";

        LocalDateTime agora = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
        VendaCalculo venda = new VendaCalculo();
        venda.setCalculo(calculo);
        venda.setUnidades(unidades);
        venda.setDataVenda(agora);
        BigDecimal sugerido = calculo.getPrecoVendaPlataforma() != null ? calculo.getPrecoVendaPlataforma() : calculo.getPrecoSugerido();
        BigDecimal descontoAplicado = desconto == null ? ZERO : desconto;
        BigDecimal precoAplicado = preco == null ? sugerido : preco;
        if (precoAplicado == null || descontoAplicado.compareTo(precoAplicado) > 0) return "valorVendaInvalido";
        BigDecimal taxasAplicadas = taxas == null ? (calculo.getTaxasPlataforma() == null ? ZERO : calculo.getTaxasPlataforma()) : taxas;
        venda.setPrecoUnitario(precoAplicado);
        venda.setDescontoUnitario(descontoAplicado);
        venda.setTaxasUnitarias(taxasAplicadas);
        venda.setValoresReais(preco != null && taxas != null);
        venda.setLucroUnitario(precoAplicado.subtract(descontoAplicado).subtract(taxasAplicadas)
                .subtract(calculo.getCustoTotal()));
        vendaCalculoRepository.save(venda);

        int totalVendido = vendidas + unidades;
        calculo.setUnidadesVendidas(totalVendido);
        calculo.setVendido(totalVendido > 0);
        calculo.setDataVenda(agora);
        return "ok";
    }

    public String cancelarVendas(UUID id) {
        CalculoHistorico calculo = calculoHistoricoRepository.bloquearPorId(id).orElse(null);
        if (calculo == null) return "naoEncontrado";
        for (VendaCalculo venda : vendaCalculoRepository.findByCalculo_IdOrderByDataVendaDesc(id)) {
            venda.setUnidadesCanceladas(venda.getUnidades());
            venda.setDataCancelamento(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
        }
        recalcularVendas(calculo);
        return "ok";
    }

    public String registrarVendasEmLote(Collection<UUID> ids, Integer unidadesPorLote) {
        if (ids == null || ids.isEmpty()) return "nenhumSelecionado";
        if (unidadesPorLote == null || unidadesPorLote < 1) return "quantidadeVendaInvalida";
        for (UUID id : ids.stream().filter(java.util.Objects::nonNull).distinct().sorted().toList()) {
            String resultado = registrarVenda(id, unidadesPorLote);
            if (!"ok".equals(resultado)) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return resultado;
            }
        }
        return "ok";
    }

    private int valorOuZero(Integer valor) {
        return valor == null ? 0 : valor;
    }

    public String atualizarStatusEmLote(Collection<UUID> ids, String acao) {
        if (ids == null || ids.isEmpty()) return "nenhumSelecionado";
        if (!List.of("PRODUZIR", "VENDER", "DESFAZER_PRODUCAO", "CANCELAR_VENDA").contains(acao)) {
            return "acaoInvalida";
        }

        List<UUID> unicos = ids.stream().filter(java.util.Objects::nonNull).distinct().sorted().toList();
        if (unicos.isEmpty()) return "nenhumSelecionado";
        for (UUID id : unicos) {
            String resultado = switch (acao) {
                case "PRODUZIR" -> atualizarProduzido(id, true);
                case "VENDER" -> atualizarVendido(id, true);
                case "DESFAZER_PRODUCAO" -> atualizarProduzido(id, false);
                case "CANCELAR_VENDA" -> cancelarVendas(id);
                default -> "acaoInvalida";
            };
            if (!"ok".equals(resultado)) {
                // Não deixa uma seleção parcialmente aplicada se algum lote falhar.
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return resultado;
            }
        }
        return "ok";
    }

    /** Ajusta o estoque usando apenas os campos mínimos lidos para a troca de status. */
    private boolean ajustarEstoque(List<AjusteEstoqueCalculoResponse> dados, boolean devolver) {
        Map<UUID, BigDecimal> quantidades = new HashMap<>();
        for (AjusteEstoqueCalculoResponse item : dados) {
            if (item.filamentoItemId() != null && item.quantidadeItem() != null) {
                quantidades.merge(item.filamentoItemId(), item.quantidadeItem(), BigDecimal::add);
            }
        }

        // Compatibilidade com históricos antigos sem itens de filamento vinculados.
        if (quantidades.isEmpty()) {
            AjusteEstoqueCalculoResponse principal = dados.get(0);
            if (principal.filamentoPrincipalId() != null && principal.quantidadePrincipal() != null) {
                quantidades.put(principal.filamentoPrincipalId(), principal.quantidadePrincipal());
            }
        }

        if (!devolver && quantidades.isEmpty()) return false;

        Map<UUID, Filamento> filamentos = new HashMap<>();
        for (Filamento filamento : filamentoService.fetchByIds(quantidades.keySet())) {
            filamentos.put(filamento.getId(), filamento);
        }
        if (!devolver && filamentos.size() != quantidades.size()) return false;

        if (!devolver) {
            for (Map.Entry<UUID, Filamento> entrada : filamentos.entrySet()) {
                BigDecimal disponivel = entrada.getValue().getEstoqueGramas() == null
                        ? ZERO : entrada.getValue().getEstoqueGramas();
                if (disponivel.compareTo(quantidades.get(entrada.getKey())) < 0) return false;
            }
        }

        quantidades.forEach((filamentoId, quantidade) -> {
            Filamento filamento = filamentos.get(filamentoId);
            if (filamento == null) return;
            BigDecimal saldo = filamento.getEstoqueGramas() == null
                    ? ZERO : filamento.getEstoqueGramas();
            filamento.setEstoqueGramas(devolver ? saldo.add(quantidade) : saldo.subtract(quantidade));
        });
        return true;
    }

    public String cancelarVenda(UUID calculoId, UUID vendaId, Integer unidades) {
        CalculoHistorico calculo = calculoHistoricoRepository.bloquearPorId(calculoId).orElse(null);
        if (calculo == null) return "naoEncontrado";
        VendaCalculo venda = vendaCalculoRepository.findById(vendaId).orElse(null);
        if (venda == null || !calculoId.equals(venda.getCalculo().getId())) return "naoEncontrado";
        if (unidades == null || unidades < 1 || unidades > venda.getUnidadesAtivas()) return "quantidadeVendaInvalida";
        venda.setUnidadesCanceladas(venda.getUnidadesCanceladas() + unidades);
        venda.setDataCancelamento(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
        recalcularVendas(calculo);
        return "ok";
    }

    private void recalcularVendas(CalculoHistorico calculo) {
        List<VendaCalculo> vendas = vendaCalculoRepository.findByCalculo_IdOrderByDataVendaDesc(calculo.getId());
        int ativas = vendas.stream().mapToInt(VendaCalculo::getUnidadesAtivas).sum();
        calculo.setUnidadesVendidas(ativas);
        calculo.setVendido(ativas > 0);
        calculo.setDataVenda(vendas.stream().filter(v -> v.getUnidadesAtivas() > 0)
                .map(VendaCalculo::getDataVenda).max(LocalDateTime::compareTo).orElse(null));
    }
}

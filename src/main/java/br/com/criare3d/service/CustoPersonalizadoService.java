package br.com.criare3d.service;

import br.com.criare3d.models.CustoPersonalizado;
import br.com.criare3d.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Service @Transactional
public class CustoPersonalizadoService {
    @Autowired private CalculoHistoricoRepository historicos;
    @Autowired private CustoPersonalizadoRepository custos;

    public void registrar(UUID id, String descricao, BigDecimal valor) {
        if (descricao == null || descricao.trim().length() < 2 || descricao.trim().length() > 150
                || valor == null || valor.signum() <= 0 || valor.scale() > 2
                || valor.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException("Informe uma descrição e um valor maior que zero, com até duas casas decimais.");
        }
        var lote = historicos.bloquearPorId(id).orElseThrow(() -> new IllegalArgumentException("Lote não encontrado."));
        var custo = new CustoPersonalizado();
        custo.setCalculo(lote);
        custo.setDescricao(descricao.trim());
        custo.setValor(valor);
        custo.setDataRegistro(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
        custos.save(custo);
    }

    public void cancelar(UUID id, UUID custoId) {
        historicos.bloquearPorId(id).orElseThrow(() -> new IllegalArgumentException("Lote não encontrado."));
        var custo = custos.findById(custoId).orElseThrow(() -> new IllegalArgumentException("Custo não encontrado."));
        if (!id.equals(custo.getCalculo().getId())) throw new IllegalArgumentException("Custo não encontrado.");
        custo.setCancelado(true);
    }
}

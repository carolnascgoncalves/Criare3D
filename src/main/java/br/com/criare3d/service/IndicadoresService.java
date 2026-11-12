package br.com.criare3d.service;

import br.com.criare3d.models.*;
import br.com.criare3d.repository.CalculoHistoricoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Service
@Transactional(readOnly = true)
public class IndicadoresService {
    private final CalculoHistoricoRepository calculoHistoricoRepository;
    private final UsuarioLogadoService usuarioLogadoService;
    public IndicadoresService(CalculoHistoricoRepository calculoHistoricoRepository, UsuarioLogadoService usuarioLogadoService) {
        this.calculoHistoricoRepository = calculoHistoricoRepository;
        this.usuarioLogadoService = usuarioLogadoService;
    }

    public long contarProduzidos() {
        return calculoHistoricoRepository.contarUnidadesProduzidas();
    }

    public long contarVendidos() {
        return calculoHistoricoRepository.somarUnidadesVendidas();
    }

    @Transactional(readOnly = true)
    public long contarUnidadesParaProduzir() {
        return calculoHistoricoRepository.contarUnidadesParaProduzir();
    }

    @Transactional(readOnly = true)
    public BigDecimal somarCustoUnidadesEmEstoque() {
        return calculoHistoricoRepository.somarCustoUnidadesEmEstoque();
    }

    @Transactional(readOnly = true)
    public long contarUnidadesVendidasEntre(LocalDateTime inicio, LocalDateTime fim) {
        return calculoHistoricoRepository.contarUnidadesVendidasEntre(inicio, fim, usuarioLogadoService.buscarTenantIdAtual());
    }

    @Transactional(readOnly = true)
    public long contarLotesVendidosEntre(LocalDateTime inicio, LocalDateTime fim) {
        return calculoHistoricoRepository.contarLotesVendidosEntre(inicio, fim, usuarioLogadoService.buscarTenantIdAtual());
    }

    @Transactional(readOnly = true)
    public BigDecimal somarLucroVendidoEntre(LocalDateTime inicio, LocalDateTime fim) {
        return calculoHistoricoRepository.somarLucroVendidoEntre(inicio, fim);
    }
}

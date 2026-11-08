package br.com.criare3d.service;

import br.com.criare3d.dto.DashboardResumo;
import br.com.criare3d.repository.CalculoHistoricoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DashboardService {
    private final CalculoHistoricoRepository historicoRepository;

    public DashboardService(CalculoHistoricoRepository historicoRepository) {
        this.historicoRepository = historicoRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResumo resumir(UUID usuarioId, LocalDateTime inicio, LocalDateTime fim) {
        return DashboardResumo.from(historicoRepository.resumirDashboard(usuarioId, inicio, fim));
    }
}

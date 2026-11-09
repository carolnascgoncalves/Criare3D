package br.com.criare3d.service;

import br.com.criare3d.dto.TenantTemaProjection;
import br.com.criare3d.models.Tenant;
import br.com.criare3d.repository.TenantRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/** Cache local de tema e logos, sempre separado pela identidade do tenant. */
@Service
public class TenantTemaCache {
    private static final int LOGO_CACHE_MAX_BYTES = 32 * 1024 * 1024;

    private final TenantRepository tenantRepository;
    private final Cache<UUID, Optional<TenantTemaProjection>> temas = Caffeine.newBuilder()
            .maximumSize(1_000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();
    private final Cache<LogoChave, Optional<byte[]>> logos = Caffeine.<LogoChave, Optional<byte[]>>newBuilder()
            .maximumWeight(LOGO_CACHE_MAX_BYTES)
            .weigher((LogoChave chave, Optional<byte[]> bytes) -> bytes
                    .map(valor -> valor.length)
                    .orElse(1))
            .expireAfterAccess(Duration.ofHours(1))
            .build();

    public TenantTemaCache(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public Optional<TenantTemaProjection> buscarTema(UUID tenantId) {
        return temas.get(tenantId, tenantRepository::buscarTema);
    }

    public Optional<byte[]> buscarLogo(UUID tenantId, long versao) {
        Optional<byte[]> encontrado = logos.get(new LogoChave(tenantId, versao), chave ->
                tenantRepository.findById(chave.tenantId())
                        .map(Tenant::getLogo)
                        .filter(bytes -> bytes != null && bytes.length > 0)
                        .map(bytes -> bytes.clone()));
        return encontrado.map(bytes -> bytes.clone());
    }

    public void invalidar(UUID tenantId) {
        if (tenantId == null) return;
        temas.invalidate(tenantId);
        logos.asMap().keySet().removeIf(chave -> chave.tenantId().equals(tenantId));
    }

    private record LogoChave(UUID tenantId, long versao) { }
}

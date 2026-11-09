package br.com.criare3d.service;

import br.com.criare3d.dto.TenantTemaProjection;
import br.com.criare3d.models.Tenant;
import br.com.criare3d.repository.TenantRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantTemaCacheTest {

    @Test
    void temaFicaEmCacheAteSerInvalidado() {
        TenantRepository repository = mock(TenantRepository.class);
        TenantTemaProjection tema = mock(TenantTemaProjection.class);
        UUID tenantId = UUID.randomUUID();
        when(repository.buscarTema(tenantId)).thenReturn(Optional.of(tema));
        TenantTemaCache cache = new TenantTemaCache(repository);

        assertThat(cache.buscarTema(tenantId)).contains(tema);
        assertThat(cache.buscarTema(tenantId)).contains(tema);
        verify(repository, times(1)).buscarTema(tenantId);

        cache.invalidar(tenantId);
        assertThat(cache.buscarTema(tenantId)).contains(tema);
        verify(repository, times(2)).buscarTema(tenantId);
    }

    @Test
    void logoEmCacheNaoPermiteQueChamadorAltereOsBytesGuardados() {
        TenantRepository repository = mock(TenantRepository.class);
        UUID tenantId = UUID.randomUUID();
        byte[] logoOriginal = {1, 2, 3};
        Tenant tenant = new Tenant();
        tenant.setLogo(logoOriginal);
        when(repository.findById(tenantId)).thenReturn(Optional.of(tenant));
        TenantTemaCache cache = new TenantTemaCache(repository);

        byte[] primeiroRetorno = cache.buscarLogo(tenantId, 1).orElseThrow();
        primeiroRetorno[0] = 99;
        assertThat(cache.buscarLogo(tenantId, 1).orElseThrow()).containsExactly(1, 2, 3);
        verify(repository, times(1)).findById(tenantId);

        cache.invalidar(tenantId);
        cache.buscarLogo(tenantId, 1).orElseThrow();
        verify(repository, times(2)).findById(tenantId);
    }
}

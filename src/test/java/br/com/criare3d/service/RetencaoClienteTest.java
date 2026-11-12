package br.com.criare3d.service;

import br.com.criare3d.models.Tenant;
import br.com.criare3d.repository.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RetencaoClienteTest {
    @Test void novaTabelaFilhaEntraNaOrdemSemListaManual() {
        assertThat(RetencaoClienteService.ordenar(Map.of(
                "tenant", Set.of("usuario"), "usuario", Set.of("nova_tabela"), "nova_tabela", Set.of())))
                .containsExactly("nova_tabela", "usuario", "tenant");
    }
    @Test void cicloInterrompeExpurgo() {
        assertThatIllegalStateException().isThrownBy(() -> RetencaoClienteService.ordenar(
                Map.of("a", Set.of("b"), "b", Set.of("a"))));
    }
    @Test void excluirPreservaDadosEPermiteRestaurar() {
        var tenants = mock(TenantRepository.class);
        var cache = mock(TenantTemaCache.class);
        var jdbc = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        var service = new ClienteAdminService(tenants, null, null, jdbc, cache);
        var t = new Tenant(); t.setId(UUID.randomUUID()); t.setNome("Cliente");
        when(tenants.findById(t.getId())).thenReturn(Optional.of(t));
        service.excluir(t.getId(), "Cliente");
        assertThat(t.isAtivo()).isFalse(); assertThat(t.getExcluidoEm()).isNotNull();
        verifyNoInteractions(jdbc);
        assertThatIllegalArgumentException().isThrownBy(() -> service.alternarAtivo(t.getId()));
        service.restaurar(t.getId());
        assertThat(t.isAtivo()).isTrue(); assertThat(t.getExcluidoEm()).isNull();
    }
    @Test void prazoExpiradoNaoPermiteRestauracao() {
        var tenants = mock(TenantRepository.class);
        var service = new ClienteAdminService(tenants, null, null, null, null);
        var t = new Tenant(); t.setId(UUID.randomUUID()); t.setExcluidoEm(Instant.now().minusSeconds(31 * 86400));
        when(tenants.findById(t.getId())).thenReturn(Optional.of(t));
        assertThatIllegalArgumentException().isThrownBy(() -> service.restaurar(t.getId()));
        verify(tenants, never()).save(any());
    }
}

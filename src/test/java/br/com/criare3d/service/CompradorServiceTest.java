package br.com.criare3d.service;

import br.com.criare3d.dto.SalvarComprador;
import br.com.criare3d.models.Comprador;
import br.com.criare3d.repository.CompradorRepository;
import br.com.criare3d.repository.PedidoOrcamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompradorServiceTest {
    private final CompradorRepository repository = mock(CompradorRepository.class);
    private final CompradorService service = new CompradorService();

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(service, "compradores", repository);
        ReflectionTestUtils.setField(service, "pedidos", mock(PedidoOrcamentoRepository.class));
    }

    @Test
    void duplicidadeInformaAmbosOsCamposInclusiveDeClienteArquivadoSemSalvar() {
        var existente = new Comprador();
        existente.setId(UUID.randomUUID());
        existente.setNome("Cliente anterior");
        existente.setArquivado(true);
        when(repository.porEmail("CLIENTE@example.com")).thenReturn(List.of(existente));
        when(repository.porTelefone("11999991234")).thenReturn(List.of(existente));

        assertThatThrownBy(() -> service.salvar(new SalvarComprador(null, null, "Novo cliente",
            "+55 (11) 99999-1234", " CLIENTE@example.com ", null)))
            .isInstanceOfSatisfying(CompradorDuplicadoException.class, e -> {
                assertThat(e.getCampos()).containsOnlyKeys("email", "telefone");
                assertThat(e.getCampos().values()).allSatisfy(m -> assertThat(m).contains("arquivado"));
            });
        verify(repository, never()).save(any());
    }

    @Test
    void editarMesmoClienteNaoAcusaDuplicidadeENormalizaDados() {
        var existente = new Comprador();
        existente.setId(UUID.randomUUID());
        existente.setVersao(3L);
        when(repository.findById(existente.getId())).thenReturn(Optional.of(existente));
        when(repository.porEmail("CLIENTE@example.com")).thenReturn(List.of(existente));
        when(repository.porTelefone("11999991234")).thenReturn(List.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        var salvo = service.salvar(new SalvarComprador(existente.getId(), 3L, " Cliente editado ",
            " (11) 99999-1234 ", " CLIENTE@example.com ", " "));

        assertThat(salvo.getNome()).isEqualTo("Cliente editado");
        assertThat(salvo.getEmail()).isEqualTo("cliente@example.com");
        assertThat(salvo.getTelefone()).isEqualTo("(11) 99999-1234");
        assertThat(salvo.getObservacoes()).isNull();
    }

    @Test
    void versaoAntigaNaoSobrescreveClienteAlteradoEmOutraTela() {
        var existente = new Comprador();
        existente.setId(UUID.randomUUID());
        existente.setVersao(4L);
        existente.setNome("Nome atual");
        when(repository.findById(existente.getId())).thenReturn(Optional.of(existente));

        assertThatIllegalArgumentException().isThrownBy(() -> service.salvar(
            new SalvarComprador(existente.getId(), 3L, "Nome antigo", null, null, null)));

        assertThat(existente.getNome()).isEqualTo("Nome atual");
        verify(repository, never()).save(any());
    }
}

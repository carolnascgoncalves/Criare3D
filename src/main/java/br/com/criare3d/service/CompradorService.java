package br.com.criare3d.service;

import br.com.criare3d.dto.SalvarComprador;
import br.com.criare3d.models.Comprador;
import br.com.criare3d.repository.CompradorRepository;
import br.com.criare3d.repository.PedidoOrcamentoRepository;
import br.com.criare3d.util.ContatoCliente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CompradorService {
    public static final int DIAS_SEM_ORCAMENTO = 60;

    @Autowired
    private CompradorRepository compradores;
    @Autowired
    private PedidoOrcamentoRepository pedidos;
    @Autowired
    private UsuarioLogadoService usuario;
    @Autowired
    private Clock clock;

    public record ResumoCliente(Comprador comprador, long total, long abertos, LocalDateTime ultimo) {
    }

    public record Listagem(Page<ResumoCliente> pagina, CompradorRepository.Contadores contadores) {
    }

    public Listagem listar(String busca, boolean arquivados, Pageable pageable) {
        busca = busca == null ? "" : busca.strip();
        var numero = ContatoCliente.telefoneNormalizado(busca);
        if (numero.length() < 3) numero = "";
        var pagina = compradores.pesquisar(busca, numero, arquivados, pageable);
        var ids = pagina.getContent().stream().map(Comprador::getId).toList();
        var resumos = new HashMap<UUID, PedidoOrcamentoRepository.ResumoCliente>();
        if (!ids.isEmpty()) {
            pedidos.resumoPorComprador(ids).forEach(r -> resumos.put(r.getCompradorId(), r));
        }
        var linhas = pagina.map(c -> {
            var resumo = resumos.get(c.getId());
            return new ResumoCliente(c, resumo == null ? 0 : resumo.getTotal(),
                resumo == null ? 0 : resumo.getAbertos(), resumo == null ? null : resumo.getUltimo());
        });
        var contadores = compradores.contadores(usuario.buscarTenantIdAtual(),
            LocalDateTime.now(clock).minusDays(DIAS_SEM_ORCAMENTO));
        return new Listagem(linhas, contadores);
    }

    public Comprador buscar(UUID id) {
        return compradores.findById(id).orElseThrow();
    }

    @Transactional
    public Comprador salvar(SalvarComprador comando) {
        var cliente = comando.id() == null ? new Comprador() : buscar(comando.id());
        if (cliente.getId() != null && !Objects.equals(cliente.getVersao(), comando.versao())) {
            throw new IllegalArgumentException("O cliente foi alterado em outra tela. Reabra antes de salvar.");
        }
        verificarDuplicado(comando);
        cliente.setNome(comando.nome().strip());
        cliente.setTelefone(vazioParaNulo(comando.telefone()));
        var email = vazioParaNulo(comando.email());
        cliente.setEmail(email == null ? null : email.toLowerCase(Locale.ROOT));
        cliente.setObservacoes(vazioParaNulo(comando.observacoes()));
        return compradores.save(cliente);
    }

    @Transactional
    public void arquivar(UUID id, boolean arquivado) {
        buscar(id).setArquivado(arquivado);
    }

    private void verificarDuplicado(SalvarComprador comando) {
        Map<String, String> erros = new LinkedHashMap<>();
        var email = vazioParaNulo(comando.email());
        if (email != null) {
            compradores.porEmail(email).stream().filter(c -> !Objects.equals(c.getId(), comando.id()))
                .findFirst().ifPresent(c -> erros.put("email", mensagemDuplicado("e-mail", c)));
        }
        var numero = ContatoCliente.telefoneNormalizado(comando.telefone());
        if (numero.length() >= 8) {
            compradores.porTelefone(numero).stream().filter(c -> !Objects.equals(c.getId(), comando.id()))
                .findFirst().ifPresent(c -> erros.put("telefone", mensagemDuplicado("telefone", c)));
        }
        if (!erros.isEmpty()) throw new CompradorDuplicadoException(erros);
    }

    private String mensagemDuplicado(String campo, Comprador existente) {
        return "Já existe um cliente com este " + campo + ": " + existente.getNome()
            + (existente.isArquivado() ? " (arquivado; restaure em Ver arquivados)." : ".");
    }

    private static String vazioParaNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}

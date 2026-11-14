package br.com.criare3d.service;

import br.com.criare3d.models.Filamento;
import br.com.criare3d.dto.CompraFilamentoRequest;
import br.com.criare3d.repository.FilamentoRepository;
import br.com.criare3d.repository.CalculoHistoricoRepository;
import br.com.criare3d.service.interfaces.FilamentoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import java.util.Set;

@Service
@Transactional
public class FilamentoServiceImp implements FilamentoService {

    @org.springframework.beans.factory.annotation.Autowired private br.com.criare3d.repository.FalhaProducaoRepository falhaProducaoRepository;
    @org.springframework.beans.factory.annotation.Autowired private MovimentacaoService movimentos;
    private final FilamentoRepository filamentoRepository;
    private final UsuarioLogadoService usuarioLogadoService;
    private final CalculoHistoricoRepository calculoHistoricoRepository;

    public FilamentoServiceImp(FilamentoRepository filamentoRepository,
                               UsuarioLogadoService usuarioLogadoService,
                               CalculoHistoricoRepository calculoHistoricoRepository) {
        this.filamentoRepository = filamentoRepository;
        this.usuarioLogadoService = usuarioLogadoService;
        this.calculoHistoricoRepository = calculoHistoricoRepository;
    }

    @Override
    public Filamento create(Filamento filamento) {
        if (filamento.getEstoqueGramas() == null) {
            filamento.setEstoqueGramas(filamento.getPesoRoloGramas());
        }
        filamento.setUsuario(usuarioLogadoService.buscarUsuarioAtual());
        var salvo=filamentoRepository.save(filamento);
        if(movimentos!=null)movimentos.registrar("SALDO_INICIAL",salvo.getNome()+" · "+salvo.getCor(),salvo.getEstoqueGramas(),"g","filamento",salvo.getId());
        return salvo;
    }

    @Override
    public Optional<Filamento> update(UUID id, Filamento patch) {
        return fetchById(id).map(existing -> {
            if (patch.getNome() != null)
                existing.setNome(patch.getNome());

            if (patch.getCor() != null)
                existing.setCor(patch.getCor());

            if (patch.getCodigoCor() != null)
                existing.setCodigoCor(patch.getCodigoCor());

            if (patch.getMarca() != null)
                existing.setMarca(patch.getMarca());

            if (patch.getPesoRoloGramas() != null)
                existing.setPesoRoloGramas(patch.getPesoRoloGramas());

            if (patch.getEstoqueGramas() != null) {
                var antes=existing.getEstoqueGramas()==null?BigDecimal.ZERO:existing.getEstoqueGramas();
                existing.setEstoqueGramas(patch.getEstoqueGramas());
                if(movimentos!=null)movimentos.registrar("AJUSTE",existing.getNome()+" · "+existing.getCor(),patch.getEstoqueGramas().subtract(antes),"g","filamento",id);
            }

            if (patch.getPrecoRolo() != null)
                existing.setPrecoRolo(patch.getPrecoRolo());

            return filamentoRepository.save(existing);
        });
    }

    @Override
    public void delete(UUID id) {
        fetchById(id).ifPresent(filamentoRepository::delete);
    }

    @Override
    public List<UUID> deleteUnused(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<UUID> solicitados = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (solicitados.isEmpty()) return List.of();
        List<UUID> existentes = filamentoRepository.findAllByIdIn(solicitados)
                .stream().map(Filamento::getId).toList();
        if (existentes.isEmpty()) return List.of();
        Set<UUID> bloqueados = new java.util.HashSet<>(calculoHistoricoRepository.buscarIdsFilamentoComHistorico(existentes));
        if (falhaProducaoRepository != null) bloqueados.addAll(falhaProducaoRepository.buscarFilamentosComRegistro(existentes));
        List<UUID> removiveis = existentes.stream().filter(id -> !bloqueados.contains(id)).toList();
        if (!removiveis.isEmpty()) filamentoRepository.deleteAllByIdInBatch(removiveis);
        return existentes.stream().filter(bloqueados::contains).toList();
    }

    @Override
    public Page<Filamento> fetchAll(Pageable pageable) {
        return filamentoRepository.findByArquivadoFalse(pageable);
    }

    @Override
    public long contar() {
        return filamentoRepository.countByArquivadoFalse();
    }

    @Override
    public Page<Filamento> fetchAll(Pageable pageable, String busca) {
        if (busca == null || busca.isBlank()) {
            return filamentoRepository.findByArquivadoFalse(pageable);
        }

        String[] termos = busca.trim()
                .toLowerCase(Locale.ROOT)
                .split("\\s+");

        Specification<Filamento> especificacao = (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> filtros = new java.util.ArrayList<>();
            filtros.add(criteriaBuilder.isFalse(root.get("arquivado")));

            for (String termo : termos) {
                String padrao = "%" + termo + "%";

                var correspondeNome = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("nome")), padrao
                );

                var correspondeCor = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("cor")), padrao
                );

                filtros.add(criteriaBuilder.or(correspondeNome, correspondeCor));
            }

            return criteriaBuilder.and(filtros.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };

        return filamentoRepository.findAll(especificacao, pageable);
    }

    @Override
    public Optional<Filamento> fetchById(UUID id) {
        return filamentoRepository.findById(id);
    }

    @Override
    public List<Filamento> fetchByIds(java.util.Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return filamentoRepository.findAllByIdIn(ids);
    }

    @Override
    public List<Filamento> fetchRecentes(int limite) {
        if (limite <= 0) {
            return List.of();
        }
        return filamentoRepository.findByArquivadoFalseOrderByDataCadastroDescNomeAsc(PageRequest.of(0, limite));
    }

    @Override
    public Optional<Filamento> fetchEstoqueCritico() {
        List<Filamento> encontrados = filamentoRepository.buscarComEstoqueCritico(
                PageRequest.of(0, 1));
        return encontrados.stream().findFirst();
    }

    @Override
    public boolean existsById(UUID id) {
        return filamentoRepository.existsById(id);
    }

    @Override
    public boolean registrarCompra(UUID id, CompraFilamentoRequest compra) {
        Filamento filamento = fetchById(id).orElse(null);
        if (filamento == null) return false;

        BigDecimal estoqueAtual = filamento.getEstoqueGramas() == null ? BigDecimal.ZERO : filamento.getEstoqueGramas();
        BigDecimal pesoNovoRolo = compra.getPesoRoloGramas();
        BigDecimal precoNovoRolo = compra.getPrecoRolo();
        BigDecimal gramasAdicionadas = compra.getGramasCompradas() == null
                ? pesoNovoRolo : compra.getGramasCompradas();
        BigDecimal estoqueNovo = estoqueAtual.add(gramasAdicionadas);

        BigDecimal valorEstoqueAtual = BigDecimal.ZERO;
        if (estoqueAtual.signum() > 0) {
            BigDecimal pesoRoloAtual = filamento.getPesoRoloGramas();
            BigDecimal precoRoloAtual = filamento.getPrecoRolo();
            if (pesoRoloAtual == null || pesoRoloAtual.signum() <= 0
                    || precoRoloAtual == null || precoRoloAtual.signum() < 0) {
                throw new IllegalStateException("O custo atual do filamento não está válido para calcular a média.");
            }
            valorEstoqueAtual = estoqueAtual.multiply(precoRoloAtual)
                    .divide(pesoRoloAtual, 10, java.math.RoundingMode.HALF_UP);
        }
        BigDecimal valorCompraAdicionada = gramasAdicionadas.multiply(precoNovoRolo)
                .divide(pesoNovoRolo, 10, java.math.RoundingMode.HALF_UP);
        BigDecimal custoMedioPorGrama = valorEstoqueAtual.add(valorCompraAdicionada)
                .divide(estoqueNovo, 12, java.math.RoundingMode.HALF_UP);

        filamento.setEstoqueGramas(estoqueNovo);
        if(movimentos!=null)movimentos.registrar("COMPRA",filamento.getNome()+" · "+filamento.getCor(),gramasAdicionadas,"g","filamento",id);
        filamento.setPesoRoloGramas(pesoNovoRolo);
        filamento.setPrecoRolo(custoMedioPorGrama.multiply(pesoNovoRolo)
                .setScale(2, java.math.RoundingMode.HALF_UP));
        filamentoRepository.saveAndFlush(filamento);
        return true;
    }
}

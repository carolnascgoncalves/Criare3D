package br.com.criare3d.service;

import br.com.criare3d.dto.Modelo3mfFilamentoResponse;
import br.com.criare3d.dto.Modelo3mfPlacaResponse;
import br.com.criare3d.dto.Modelo3mfResponse;
import br.com.criare3d.dto.PecaArquivoResumo;
import br.com.criare3d.dto.PecaMaterialResumo;
import br.com.criare3d.dto.PecaModeloImportacaoRequest;
import br.com.criare3d.models.Peca;
import br.com.criare3d.models.PecaArquivoPlaca;
import br.com.criare3d.repository.PecaArquivoPlacaReferencia;
import br.com.criare3d.repository.PecaArquivoPlacaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class PecaArquivoService {
    private static final int MAX_JSON_CHARS = 2_000_000;
    private static final int MAX_PREVIEW_BYTES = 180 * 1024;
    private static final int MAX_TOTAL_PREVIEW_BYTES = 900 * 1024;
    private static final Pattern PREVIEW_DATA_URI = Pattern.compile("^data:(image/(?:png|jpeg|webp));base64,([A-Za-z0-9+/=]+)$", Pattern.CASE_INSENSITIVE);

    private final PecaArquivoPlacaRepository placaRepository;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public PecaArquivoService(PecaArquivoPlacaRepository placaRepository) {
        this.placaRepository = placaRepository;
    }

    public Optional<Preparacao> preparar(String json) {
        if (json == null || json.isBlank()) return Optional.empty();
        if (json.length() > MAX_JSON_CHARS) throw new IllegalArgumentException("As prévias do arquivo ficaram grandes demais. Tente ler o .3mf novamente.");
        try {
            PecaModeloImportacaoRequest pedido = jsonMapper.readValue(json, PecaModeloImportacaoRequest.class);
            if (pedido == null || pedido.dados() == null) throw new IllegalArgumentException("Os dados do arquivo 3MF estão incompletos.");
            Modelo3mfResponse dados = pedido.dados();
            int placasDeclaradas = Math.max(1, Math.min(dados.numeroPlacas(), 100));
            List<Modelo3mfPlacaResponse> placasOriginais = dados.placas() == null ? List.of() : dados.placas().stream().limit(8).toList();
            List<ImagemPreparada> imagens = new ArrayList<>();
            Set<Integer> numerosComImagem = new HashSet<>();
            int totalBytes = 0;
            List<Modelo3mfPlacaResponse> placasSemImagem = new ArrayList<>();
            for (Modelo3mfPlacaResponse placa : placasOriginais) {
                if (placa == null) continue;
                int numero = Math.max(1, Math.min(placa.numero(), 100));
                String uri = placa.imagemPreviewDataUri();
                if (uri != null && numero <= 8 && imagens.size() < 8 && numerosComImagem.add(numero)) {
                    Matcher matcher = PREVIEW_DATA_URI.matcher(uri);
                    if (matcher.matches()) {
                        byte[] bytes = Base64.getDecoder().decode(matcher.group(2));
                        if (bytes.length <= MAX_PREVIEW_BYTES && totalBytes + bytes.length <= MAX_TOTAL_PREVIEW_BYTES
                                && imagemValida(bytes, matcher.group(1))) {
                            String mime = matcher.group(1).toLowerCase(Locale.ROOT);
                            imagens.add(new ImagemPreparada(numero, mime, bytes));
                            totalBytes += bytes.length;
                        }
                    }
                }
                placasSemImagem.add(new Modelo3mfPlacaResponse(
                        numero, null, numeroSeguro(placa.tempoSegundos()), numeroSeguro(placa.pesoGramas()),
                        limitarFilamentos(placa.filamentos())));
            }

            Modelo3mfResponse metadados = new Modelo3mfResponse(
                    texto(dados.nome(), 150), texto(dados.modeloImpressora(), 150), texto(dados.material(), 500),
                    texto(dados.corMaterial(), 500), texto(dados.alturaCamada(), 40), texto(dados.diametroBico(), 100),
                    texto(dados.perfil(), 150), texto(dados.fatiador(), 80), dados.jaFatiado(), dados.temEstatisticas(),
                    dados.estatisticasCompletas(), Math.max(0, Math.min(dados.numeroPlacasComEstatisticas(), 100)),
                    numeroSeguro(dados.tempoTotalSegundos()), numeroSeguro(dados.pesoTotalGramas()),
                    limitarFilamentos(dados.filamentosUsados()), placasDeclaradas, List.copyOf(placasSemImagem));
            PecaModeloImportacaoRequest guardado = new PecaModeloImportacaoRequest(texto(pedido.nomeArquivo(), 255), metadados);
            return Optional.of(new Preparacao(jsonMapper.writeValueAsString(guardado), List.copyOf(imagens)));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Não foi possível guardar os dados lidos do arquivo 3MF. Leia o arquivo novamente.");
        }
    }

    public void salvar(UUID pecaId, Preparacao preparacao) {
        if (preparacao == null) return;
        placaRepository.deleteAllByPecaId(pecaId);
        for (ImagemPreparada imagem : preparacao.imagens()) {
            PecaArquivoPlaca entidade = new PecaArquivoPlaca();
            entidade.setPecaId(pecaId);
            entidade.setNumero(imagem.numero());
            entidade.setMimeType(imagem.mimeType());
            entidade.setPreview(imagem.bytes());
            placaRepository.save(entidade);
        }
    }

    @Transactional(readOnly = true)
    public Optional<ImagemPeca> buscarImagem(UUID pecaId, int numero) {
        return placaRepository.findByPecaIdAndNumero(pecaId, numero)
                .map(imagem -> new ImagemPeca(imagem.getMimeType(), imagem.getPreview()));
    }

    @Transactional(readOnly = true)
    public Optional<PecaArquivoResumo> buscarResumo(Peca peca) {
        if (peca == null || peca.getDadosArquivo3mf() == null || peca.getDadosArquivo3mf().isBlank()) return Optional.empty();
        List<PecaArquivoPlacaReferencia> referencias = placaRepository.listarReferenciasPorPecaIds(List.of(peca.getId()));
        return resumo(peca, referencias);
    }

    /** Materiais e gramagens do arquivo para sugerir os itens do próximo cálculo. */
    @Transactional(readOnly = true)
    public List<PecaMaterialResumo> buscarMateriaisParaCalculo(Peca peca) {
        if (peca == null || peca.getDadosArquivo3mf() == null || peca.getDadosArquivo3mf().isBlank()) {
            return List.of();
        }
        try {
            PecaModeloImportacaoRequest pedido = jsonMapper.readValue(
                    peca.getDadosArquivo3mf(), PecaModeloImportacaoRequest.class);
            Modelo3mfResponse dados = pedido == null ? null : pedido.dados();
            if (dados == null || !dados.estatisticasCompletas()) return List.of();
            List<PecaMaterialResumo> materiais = resumirFilamentos(dados.filamentosUsados());
            return materiais.stream().filter(item -> item.pesoGramas() > 0).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public Map<UUID, PecaArquivoResumo> buscarResumos(Collection<Peca> pecas) {
        if (pecas == null || pecas.isEmpty()) return Map.of();
        List<Peca> comArquivo = pecas.stream()
                .filter(peca -> peca.getDadosArquivo3mf() != null && !peca.getDadosArquivo3mf().isBlank())
                .toList();
        if (comArquivo.isEmpty()) return Map.of();
        List<UUID> ids = comArquivo.stream().map(Peca::getId).toList();
        List<PecaArquivoPlacaReferencia> referencias = placaRepository.listarReferenciasPorPecaIds(ids);
        Map<UUID, PecaArquivoResumo> resultado = new LinkedHashMap<>();
        for (Peca peca : comArquivo) resumo(peca, referencias).ifPresent(resumo -> resultado.put(peca.getId(), resumo));
        return resultado;
    }

    private Optional<PecaArquivoResumo> resumo(Peca peca, List<PecaArquivoPlacaReferencia> referencias) {
        try {
            PecaModeloImportacaoRequest pedido = jsonMapper.readValue(peca.getDadosArquivo3mf(), PecaModeloImportacaoRequest.class);
            if (pedido == null || pedido.dados() == null) return Optional.empty();
            Modelo3mfResponse dados = pedido.dados();
            List<PecaArquivoResumo.PlacaResumo> placas = new ArrayList<>();
            if (dados.placas() != null) {
                for (Modelo3mfPlacaResponse placa : dados.placas()) {
                    boolean temImagem = referencias.stream().anyMatch(ref -> ref.getPecaId().equals(peca.getId()) && ref.getNumero() == placa.numero());
                    String url = temImagem ? "/peca/" + peca.getId() + "/arquivo/placa/" + placa.numero() : null;
                    placas.add(new PecaArquivoResumo.PlacaResumo(placa.numero(), url, placa.tempoSegundos(),
                            placa.pesoGramas(), resumirFilamentos(placa.filamentos())));
                }
            }
            List<PecaMaterialResumo> materiais = resumirFilamentos(dados.filamentosUsados());
            if (materiais.isEmpty()) materiais = materiaisConfigurados(dados.material(), dados.corMaterial());
            return Optional.of(new PecaArquivoResumo(peca.getId(), pedido.nomeArquivo(), dados.modeloImpressora(),
                    dados.fatiador(), dados.numeroPlacas(), materiais, List.copyOf(placas)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private List<PecaMaterialResumo> resumirFilamentos(List<Modelo3mfFilamentoResponse> filamentos) {
        if (filamentos == null) return List.of();
        Map<String, MaterialAcumulado> agrupados = new LinkedHashMap<>();
        for (Modelo3mfFilamentoResponse item : filamentos) {
            if (item == null) continue;
            String material = texto(item.material(), 60);
            String cor = corSegura(item.cor());
            String chave = (material == null ? "" : material.toLowerCase(Locale.ROOT)) + "|" + (cor == null ? "" : cor);
            MaterialAcumulado acum = agrupados.computeIfAbsent(chave, ignorado -> new MaterialAcumulado(material, cor));
            acum.peso += Math.max(0, numeroSeguro(item.pesoGramas()) == null ? 0 : item.pesoGramas());
        }
        return agrupados.values().stream().map(item -> new PecaMaterialResumo(item.material, item.cor, item.peso)).toList();
    }

    private List<PecaMaterialResumo> materiaisConfigurados(String materiais, String cores) {
        String[] nomes = materiais == null ? new String[0] : materiais.split(",");
        String[] hex = cores == null ? new String[0] : cores.split(",");
        List<PecaMaterialResumo> resultado = new ArrayList<>();
        for (int i = 0; i < Math.max(nomes.length, hex.length) && i < 16; i++) {
            String nome = i < nomes.length ? texto(nomes[i], 60) : null;
            String cor = i < hex.length ? corSegura(hex[i]) : null;
            if (nome != null || cor != null) resultado.add(new PecaMaterialResumo(nome, cor, 0));
        }
        return List.copyOf(resultado);
    }

    private List<Modelo3mfFilamentoResponse> limitarFilamentos(List<Modelo3mfFilamentoResponse> origem) {
        if (origem == null) return List.of();
        return origem.stream().filter(java.util.Objects::nonNull).limit(64)
                .map(item -> new Modelo3mfFilamentoResponse(Math.max(0, Math.min(item.id(), 255)),
                        texto(item.material(), 60), corSegura(item.cor()),
                        numeroSeguro(item.pesoGramas()) == null ? 0 : Math.max(0, item.pesoGramas())))
                .toList();
    }

    private String texto(String valor, int max) {
        if (valor == null) return null;
        String limpo = valor.strip();
        return limpo.isBlank() ? null : limpo.substring(0, Math.min(max, limpo.length()));
    }

    private String corSegura(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String cor = valor.strip().toUpperCase(Locale.ROOT);
        if (cor.matches("#[0-9A-F]{8}")) cor = cor.substring(0, 7);
        return cor.matches("#(?:[0-9A-F]{3}|[0-9A-F]{6})") ? cor : null;
    }

    private Double numeroSeguro(Double valor) {
        return valor != null && Double.isFinite(valor) && valor >= 0 ? valor : null;
    }

    private boolean imagemValida(byte[] bytes, String mime) {
        if (bytes == null || bytes.length < 12) return false;
        return switch (mime.toLowerCase(Locale.ROOT)) {
            case "image/png" -> bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50
                    && bytes[2] == 0x4E && bytes[3] == 0x47;
            case "image/jpeg" -> bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8;
            case "image/webp" -> bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
    }

    public record Preparacao(String json, List<ImagemPreparada> imagens) { }
    public record ImagemPreparada(int numero, String mimeType, byte[] bytes) { }
    public record ImagemPeca(String mimeType, byte[] bytes) { }

    private static final class MaterialAcumulado {
        private final String material;
        private final String cor;
        private double peso;
        private MaterialAcumulado(String material, String cor) { this.material = material; this.cor = cor; }
    }
}

package br.com.criare3d.service;

import br.com.criare3d.dto.EstimativaDepreciacaoResponse;
import br.com.criare3d.repository.CalculoHistoricoRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Consulta apenas agregados de anúncios; o payload bruto do marketplace não é persistido. */
@Service
public class MercadoLivreDepreciacaoService {
    private static final int MINIMO_ANUNCIOS = 3;
    private static final BigDecimal HORAS_ENTRADA = new BigDecimal("3000");
    private static final BigDecimal HORAS_ROBUSTA = new BigDecimal("5000");
    private static final BigDecimal LIMITE_OUTLIER = new BigDecimal("0.50");

    private final CalculoHistoricoRepository historicoRepository;
    private final ObjectMapper objectMapper;
    private final String accessToken;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2)).build();
    private final Cache<String, ResumoMercado> cache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(7, TimeUnit.DAYS)
            .build();

    public MercadoLivreDepreciacaoService(
            CalculoHistoricoRepository historicoRepository,
            ObjectMapper objectMapper,
            @Value("${criare.integracoes.mercado-livre.access-token:}") String accessToken) {
        this.historicoRepository = historicoRepository;
        this.objectMapper = objectMapper;
        this.accessToken = accessToken == null ? "" : accessToken.trim();
    }

    public EstimativaDepreciacaoResponse estimar(String modelo, java.util.UUID impressoraId) {
        String consulta = modelo == null ? "" : modelo.trim().replaceAll("\\s+", " ");
        if (consulta.length() < 2 || consulta.length() > 100) {
            throw new IllegalArgumentException("Informe um modelo de impressora entre 2 e 100 caracteres.");
        }
        long minutos = impressoraId == null
                ? valor(historicoRepository.somarMinutosMaquinaProduzidosDesde(LocalDateTime.now().minusDays(30)))
                : valor(historicoRepository.somarMinutosMaquinaProduzidosDaImpressoraDesde(
                        LocalDateTime.now().minusDays(30), impressoraId));
        boolean horasMensaisPadrao = minutos <= 0;
        BigDecimal horasMensais = minutos > 0
                ? BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                : new BigDecimal("100.00");

        if (accessToken.isBlank()) {
            return indisponivel(consulta, horasMensais, horasMensaisPadrao,
                    "A busca de preços do Mercado Livre ainda não está configurada. Informe o token da integração no servidor.");
        }

        String chave = consulta.toLowerCase(Locale.ROOT);
        ResumoMercado resumo;
        try {
            resumo = cache.get(chave, ignorado -> {
                try { return consultar(consulta); }
                catch (InterruptedException erro) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(erro);
                }
                catch (Exception erro) { throw new IllegalStateException(erro); }
            });
        } catch (Exception ex) {
            return indisponivel(consulta, horasMensais, horasMensaisPadrao,
                    "Não foi possível consultar os anúncios agora. Tente novamente mais tarde; nenhum dado foi alterado.");
        }

        BigDecimal horasVida = horasPadrao(consulta);
        boolean temNovos = resumo.novos().quantidade() >= MINIMO_ANUNCIOS;
        boolean temUsados = resumo.usados().quantidade() >= MINIMO_ANUNCIOS;
        boolean fallback = !temUsados || resumo.usados().mediana() == null;
        BigDecimal precoNovo = temNovos ? resumo.novos().mediana() : null;
        BigDecimal precoUsado = fallback ? null : resumo.usados().mediana();
        BigDecimal depreciacao = null;
        if (precoNovo != null) {
            BigDecimal base = fallback ? precoNovo : precoNovo.subtract(precoUsado).max(BigDecimal.ZERO);
            depreciacao = base.divide(horasVida, 8, RoundingMode.HALF_UP);
        }
        String confianca = temNovos && temUsados
                ? (resumo.novos().quantidade() >= 10 && resumo.usados().quantidade() >= 10 ? "Boa base" : "Base limitada")
                : "Poucos anúncios; estimativa de reserva";
        String mensagem = fallback
                ? "Há poucos anúncios usados compatíveis. A estimativa considera valor residual zero e horas até substituição editáveis."
                : precoUsado.compareTo(precoNovo) >= 0
                    ? "A mediana usada não ficou abaixo da nova; a taxa estimada ficou em zero. Confira se os modelos comparados são equivalentes."
                    : "Referência baseada em preços anunciados; anúncios não confirmam o valor de venda.";
        return new EstimativaDepreciacaoResponse(precoNovo != null, mensagem, consulta,
                precoNovo, resumo.novos().quantidade(), precoUsado, resumo.usados().quantidade(),
                horasVida, horasMensais, horasMensaisPadrao, depreciacao, fallback, confianca);
    }

    private EstimativaDepreciacaoResponse indisponivel(String modelo, BigDecimal horasMensais,
                                                        boolean horasMensaisPadrao, String mensagem) {
        return new EstimativaDepreciacaoResponse(false, mensagem, modelo, null, 0,
                null, 0, horasPadrao(modelo), horasMensais, horasMensaisPadrao,
                null, true, "Sem referência");
    }

    private ResumoMercado consultar(String modelo) throws Exception {
        return new ResumoMercado(buscar(modelo, "new"), buscar(modelo, "used"));
    }

    private DadosAnuncios buscar(String modelo, String condicao) throws Exception {
        String query = URLEncoder.encode(modelo, StandardCharsets.UTF_8);
        URI uri = URI.create("https://api.mercadolibre.com/sites/MLB/search?q=" + query
                + "&condition=" + condicao + "&limit=50");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(4))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET().build();
        HttpResponse<String> resposta = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (resposta.statusCode() < 200 || resposta.statusCode() >= 300) {
            throw new IllegalStateException("Mercado Livre respondeu HTTP " + resposta.statusCode());
        }
        JsonNode results = objectMapper.readTree(resposta.body()).path("results");
        List<BigDecimal> precos = new ArrayList<>();
        String[] termos = termosModelo(modelo);
        for (JsonNode item : results) {
            String titulo = normalizar(item.path("title").asText(""));
            java.util.Set<String> palavras = new java.util.HashSet<>(java.util.Arrays.asList(titulo.split(" ")));
            boolean modeloCompativel = termos.length > 0;
            for (String termo : termos) modeloCompativel &= palavras.contains(termo);
            if (!modeloCompativel || !condicao.equalsIgnoreCase(item.path("condition").asText(""))) continue;
            BigDecimal preco = item.path("price").decimalValue();
            if (preco.signum() > 0) precos.add(preco);
        }
        if (precos.isEmpty()) return new DadosAnuncios(null, 0);
        BigDecimal primeiraMediana = mediana(precos);
        List<BigDecimal> semAcessorios = precos.stream()
                .filter(p -> p.compareTo(primeiraMediana.multiply(LIMITE_OUTLIER)) >= 0)
                .toList();
        return new DadosAnuncios(mediana(semAcessorios), semAcessorios.size());
    }

    private static String[] termosModelo(String modelo) {
        return normalizar(modelo).split(" ").length == 0 ? new String[0]
                : java.util.Arrays.stream(normalizar(modelo).split(" "))
                    .filter(t -> !t.equals("bambu") && !t.equals("lab") && t.length() > 1)
                    .toArray(String[]::new);
    }

    private static String normalizar(String texto) {
        return java.text.Normalizer.normalize(texto.toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static BigDecimal mediana(List<BigDecimal> valores) {
        List<BigDecimal> ordenados = valores.stream().sorted(Comparator.naturalOrder()).toList();
        int meio = ordenados.size() / 2;
        if (ordenados.size() % 2 == 1) return ordenados.get(meio);
        return ordenados.get(meio - 1).add(ordenados.get(meio))
                .divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal horasPadrao(String modelo) {
        String normalizado = normalizar(modelo);
        return normalizado.contains("mini") || normalizado.matches(".*\\ba1\\b.*")
                ? HORAS_ENTRADA : HORAS_ROBUSTA;
    }

    private static long valor(Long minutos) { return minutos == null ? 0 : minutos; }

    private record DadosAnuncios(BigDecimal mediana, int quantidade) {}
    private record ResumoMercado(DadosAnuncios novos, DadosAnuncios usados) {}
}

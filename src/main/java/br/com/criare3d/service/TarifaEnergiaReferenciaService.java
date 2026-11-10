package br.com.criare3d.service;

import br.com.criare3d.dto.TarifaEnergiaReferenciaResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Busca a tarifa residencial convencional homologada publicada pela ANEEL. */
@Service
public class TarifaEnergiaReferenciaService {
    private static final String RESOURCE_ID = "fcf2906c-7c32-4b9b-a637-054e7a5234f4";
    private static final String API = "https://dadosabertos.aneel.gov.br/api/3/action/datastore_search";
    private static final int LIMITE_POR_PAGINA = 1000;
    private static final Set<String> DISTRIBUIDORAS_SP = Set.of(
            "cpfl paulista", "companhia paulista de forca e luz",
            "cpfl piratininga", "cpfl santa cruz",
            "edp sp", "edp sao paulo", "edp sao paulo distribuicao de energia",
            "enel sp", "enel sao paulo", "enel distribuicao sao paulo", "eletropaulo",
            "energisa sul sudeste", "ess",
            "elektro", "neoenergia elektro", "elektro redes",
            "cedrap", "cedri", "cemirim", "cerim", "ceripa", "ceris", "cermc",
            "cernhe", "cerpro", "cerrp", "cervam", "cetril"
    );

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4)).build();
    private final Cache<String, List<TarifaEnergiaReferenciaResponse>> cache = Caffeine.newBuilder()
            .maximumSize(1).expireAfterWrite(12, TimeUnit.HOURS).build();

    public List<TarifaEnergiaReferenciaResponse> listar() {
        try {
            return cache.get("residencial-b1", ignorado -> {
                try { return consultar(); }
                catch (Exception e) { throw new IllegalStateException(e); }
            });
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível consultar as tarifas públicas da ANEEL agora. Tente novamente mais tarde.", e);
        }
    }

    public Map<String, String> localizarCidade(double latitude, double longitude) throws Exception {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Coordenadas inválidas.");
        }
        String url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=10&addressdetails=1&lat="
                + latitude + "&lon=" + longitude;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .header("User-Agent", "Criare3D/1.0 (sugestao de cidade para tarifa de energia)")
                .header("Accept-Language", "pt-BR").GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IllegalStateException("Não foi possível localizar a cidade agora.");
        JsonNode endereco = jsonMapper.readTree(response.body()).path("address");
        String cidade = primeiroTexto(endereco, "city", "town", "village", "municipality");
        String estado = texto(endereco, "ISO3166-2-lvl4");
        String uf = estado.contains("-") ? estado.substring(estado.lastIndexOf('-') + 1) : texto(endereco, "state");
        if (cidade.isBlank()) cidade = "Cidade não identificada";
        if (uf.isBlank()) uf = "UF não identificada";
        return Map.of("cidade", cidade, "uf", uf);
    }

    private static String primeiroTexto(JsonNode n, String... campos) {
        for (String campo : campos) {
            String valor = texto(n, campo);
            if (!valor.isBlank()) return valor;
        }
        return "";
    }

    private List<TarifaEnergiaReferenciaResponse> consultar() throws Exception {
        Map<String, TarifaEnergiaReferenciaResponse> porDistribuidora = new LinkedHashMap<>();
        LocalDate hoje = LocalDate.now();
        for (int offset = 0; offset < 20_000; offset += LIMITE_POR_PAGINA) {
            String filtros = "{\"DscSubGrupo\":\"B1\",\"DscClasse\":\"Residencial\",\"DscSubClasse\":\"Residencial\",\"DscDetalhe\":\"Não se aplica\",\"NomPostoTarifario\":\"Não se aplica\",\"DscModalidadeTarifaria\":\"Convencional\",\"DscBaseTarifaria\":\"Tarifa de Aplicação\"}";
            String campos = "SigAgente,DatInicioVigencia,DatFimVigencia,DscBaseTarifaria,DscSubGrupo,DscModalidadeTarifaria,DscClasse,DscSubClasse,DscDetalhe,NomPostoTarifario,DscUnidadeTerciaria,VlrTUSD,VlrTE";
            String url = API + "?resource_id=" + RESOURCE_ID + "&limit=" + LIMITE_POR_PAGINA
                    + "&offset=" + offset + "&fields=" + URLEncoder.encode(campos, StandardCharsets.UTF_8)
                    + "&filters=" + URLEncoder.encode(filtros, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(12))
                    .header("Accept", "application/json").header("User-Agent", "Criare3D/1.0 (referencia tarifaria)")
                    .GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new IllegalStateException("ANEEL respondeu HTTP " + response.statusCode());
            JsonNode raiz = jsonMapper.readTree(response.body());
            if (!raiz.path("success").asBoolean(false)) throw new IllegalStateException("Resposta inválida da ANEEL");
            JsonNode resultado = raiz.path("result");
            JsonNode registros = resultado.path("records");
            if (!registros.isArray() || registros.isEmpty()) break;
            for (JsonNode registro : registros) {
                TarifaEnergiaReferenciaResponse tarifa = converter(registro);
                if (tarifa == null || tarifa.inicioVigencia().isAfter(hoje)
                        || (tarifa.fimVigencia() != null && tarifa.fimVigencia().isBefore(hoje))) continue;
                String chave = tarifa.distribuidora().toUpperCase(Locale.ROOT);
                TarifaEnergiaReferenciaResponse anterior = porDistribuidora.get(chave);
                if (anterior == null || tarifa.inicioVigencia().isAfter(anterior.inicioVigencia()))
                    porDistribuidora.put(chave, tarifa);
            }
            if (registros.size() < LIMITE_POR_PAGINA || offset + LIMITE_POR_PAGINA >= resultado.path("total").asInt()) break;
        }
        return porDistribuidora.values().stream()
                .sorted(Comparator.comparing(TarifaEnergiaReferenciaResponse::distribuidora, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private TarifaEnergiaReferenciaResponse converter(JsonNode r) {
        String distribuidora = texto(r, "SigAgente");
        LocalDate inicio = data(texto(r, "DatInicioVigencia"));
        LocalDate fim = data(texto(r, "DatFimVigencia"));
        BigDecimal tusd = decimal(texto(r, "VlrTUSD"));
        BigDecimal te = decimal(texto(r, "VlrTE"));
        String unidade = texto(r, "DscUnidadeTerciaria");
        String modalidade = texto(r, "DscModalidadeTarifaria");
        String classe = texto(r, "DscClasse");
        String subgrupo = texto(r, "DscSubGrupo");
        String base = texto(r, "DscBaseTarifaria");
        if (distribuidora.isBlank() || inicio == null || tusd == null || te == null
                || !normalizar(subgrupo).equals("b1") || !normalizar(classe).contains("residencial")
                || !normalizar(modalidade).contains("convencional")
                || !normalizar(base).contains("aplicacao")
                || !normalizar(unidade).contains("mwh")
                || !DISTRIBUIDORAS_SP.contains(normalizar(distribuidora))) return null;
        // Valores residenciais de aplicação publicados em R$/MWh: converte para R$/kWh.
        BigDecimal tarifaKwh = tusd.add(te).divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP);
        return new TarifaEnergiaReferenciaResponse(distribuidora, tarifaKwh, inicio, fim, unidade);
    }

    private static String texto(JsonNode n, String campo) { return n.path(campo).asText("").trim(); }
    private static BigDecimal decimal(String valor) {
        if (valor == null || valor.isBlank() || valor.equalsIgnoreCase("null")) return null;
        try { return new BigDecimal(valor.replace(',', '.')); } catch (NumberFormatException e) { return null; }
    }
    private static LocalDate data(String valor) {
        if (valor == null || valor.isBlank() || valor.equalsIgnoreCase("null")) return null;
        try { return LocalDate.parse(valor.substring(0, 10)); } catch (Exception e) { return null; }
    }
    private static String normalizar(String s) {
        return java.text.Normalizer.normalize(s.toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^a-z0-9]+", " ").trim();
    }
}

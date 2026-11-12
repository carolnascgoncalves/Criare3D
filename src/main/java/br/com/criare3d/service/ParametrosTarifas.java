package br.com.criare3d.service;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.Properties;

/** Configuração global da instalação, carregada uma vez; não consulta banco nem rede. */
final class ParametrosTarifas {
    private final Properties valores = new Properties();

    ParametrosTarifas(Properties padrao, Properties substituicoes) {
        valores.putAll(padrao);
        for (String chave : substituicoes.stringPropertyNames()) {
            if (!padrao.containsKey(chave)) throw new IllegalArgumentException("Parâmetro de tarifa desconhecido: " + chave);
            valores.setProperty(chave, substituicoes.getProperty(chave));
        }
        validar();
    }

    static ParametrosTarifas carregar() {
        Properties padrao = new Properties(), externo = new Properties();
        try (InputStream entrada = ParametrosTarifas.class.getResourceAsStream("/tarifas-plataformas.properties")) {
            if (entrada == null) throw new IllegalStateException("Tabela de tarifas não encontrada");
            padrao.load(entrada);
            String caminho = System.getProperty("criare.tarifas.arquivo", System.getenv("CRIARE_TARIFAS_ARQUIVO"));
            if (caminho != null && !caminho.isBlank()) {
                try (InputStream arquivo = Files.newInputStream(Path.of(caminho))) { externo.load(arquivo); }
            }
        } catch (IOException erro) { throw new IllegalStateException("Não foi possível carregar a configuração de tarifas", erro); }
        return new ParametrosTarifas(padrao, externo);
    }

    BigDecimal decimal(String chave) { return numero(chave, valores.getProperty(chave)); }

    BigDecimal[] lista(String chave, int tamanho) {
        String valor = valores.getProperty(chave);
        if (valor == null) throw new IllegalArgumentException("Tarifa ausente: " + chave);
        String[] itens = valor.split(",", -1);
        if (itens.length != tamanho) throw new IllegalArgumentException("Quantidade de faixas inválida: " + chave);
        BigDecimal[] resultado = new BigDecimal[tamanho];
        for (int i = 0; i < tamanho; i++) resultado[i] = numero(chave, itens[i]);
        return resultado;
    }

    private BigDecimal numero(String chave, String valor) {
        try {
            BigDecimal numero = new BigDecimal(valor.strip());
            if (numero.scale() > 8 || numero.signum() < 0 || numero.compareTo(new BigDecimal("99999999")) > 0)
                throw new NumberFormatException();
            if ((!chave.equals("AMAZON_COMISSAO_MINIMA") && (chave.contains("COMISSAO") || chave.contains("PERCENTUAL"))) && numero.compareTo(new BigDecimal("100")) > 0)
                throw new NumberFormatException();
            return numero;
        } catch (NullPointerException | NumberFormatException erro) {
            throw new IllegalArgumentException("Valor de tarifa inválido: " + chave);
        }
    }

    void validar() {
        for (String chave : valores.stringPropertyNames()) {
            if (chave.startsWith("ML_FRETES_") || chave.equals("ML_LIMITES_PRECO")) lista(chave, 4);
            else decimal(chave);
        }
        BigDecimal[] limites = lista("ML_LIMITES_PRECO", 4);
        for (int i = 0; i < limites.length; i++) {
            if (limites[i].signum() <= 0 || (i > 0 && limites[i].compareTo(limites[i - 1]) <= 0))
                throw new IllegalArgumentException("Limites de preço do Mercado Livre devem ser positivos e crescentes");
        }
        for (String tipo : new String[]{"CLASSICO", "PREMIUM"}) {
            BigDecimal minimo = decimal("ML_" + tipo + "_COMISSAO_MINIMA");
            BigDecimal padrao = decimal("ML_" + tipo + "_COMISSAO_PADRAO");
            BigDecimal maximo = decimal("ML_" + tipo + "_COMISSAO_MAXIMA");
            if (minimo.compareTo(padrao) > 0 || padrao.compareTo(maximo) > 0)
                throw new IllegalArgumentException("Limites de comissão do Mercado Livre inconsistentes: " + tipo);
        }
        String[] finais = {"SHOPEE_LIMITE_80", "SHOPEE_LIMITE_100", "SHOPEE_LIMITE_200"};
        String[] inicios = {"SHOPEE_INICIO_FAIXA_80", "SHOPEE_INICIO_FAIXA_100", "SHOPEE_INICIO_FAIXA_200"};
        for (int i = 0; i < finais.length; i++) {
            BigDecimal fim = decimal(finais[i]);
            if (fim.signum() <= 0 || fim.add(new BigDecimal("0.01")).compareTo(decimal(inicios[i])) != 0
                    || (i > 0 && fim.compareTo(decimal(finais[i - 1])) <= 0))
                throw new IllegalArgumentException("Faixas de preço da Shopee inconsistentes");
        }
    }
}

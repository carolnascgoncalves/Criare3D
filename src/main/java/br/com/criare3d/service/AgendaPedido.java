package br.com.criare3d.service;

import java.math.*;
import java.time.*;
import java.util.*;

/** Impressões podem terminar fora do expediente; início e troca respeitam a jornada. */
public record AgendaPedido(LocalTime inicio, LocalTime fim, Set<DayOfWeek> dias) {
    public AgendaPedido {
        if (inicio == null || fim == null || !inicio.isBefore(fim) || dias == null || dias.isEmpty())
            throw new IllegalArgumentException("Defina o início, o fim e ao menos um dia de trabalho.");
        dias = Set.copyOf(dias);
    }
    public static AgendaPedido de(LocalTime inicio, LocalTime fim, String dias) {
        try {
            Set<DayOfWeek> semana = new HashSet<>();
            for (String dia : dias.split(",")) semana.add(DayOfWeek.of(Integer.parseInt(dia.trim())));
            return new AgendaPedido(inicio, fim, semana);
        } catch (RuntimeException erro) { throw new IllegalArgumentException("Confira os dias e os horários de trabalho."); }
    }
    public LocalDateTime proximoInicio(LocalDateTime horario) {
        for (int i = 0; i < 8; i++) {
            if (dias.contains(horario.getDayOfWeek())) {
                if (horario.toLocalTime().isBefore(inicio)) return horario.toLocalDate().atTime(inicio);
                if (horario.toLocalTime().isBefore(fim)) return horario;
            }
            horario = horario.toLocalDate().plusDays(1).atTime(inicio);
        }
        throw new IllegalArgumentException("Não foi possível encontrar um dia de trabalho.");
    }
    public Previsao prever(LocalDateTime pretendido, long minutosUnidade, int aquecimento,
                           int unidades, int capacidade, int intervalo, int folga) {
        if (pretendido == null || minutosUnidade < 1 || minutosUnidade > 525600 || aquecimento < 0
                || unidades < 1 || unidades > 500 || capacidade < 1 || capacidade > unidades
                || intervalo < 0 || intervalo > 1440 || folga < 0 || folga > 100)
            throw new IllegalArgumentException("Confira as quantidades e os ajustes de produção.");
        LocalDateTime inicioReal = proximoInicio(pretendido), cursor = inicioReal;
        List<Impressao> impressoes = new ArrayList<>();
        BigDecimal fator = BigDecimal.ONE.add(BigDecimal.valueOf(folga, 2));
        for (int restantes = unidades; restantes > 0; ) {
            int lote = Math.min(restantes, capacidade);
            long minutos = Math.addExact(Math.multiplyExact(minutosUnidade, lote), aquecimento);
            long previsto = BigDecimal.valueOf(minutos).multiply(fator).setScale(0,RoundingMode.CEILING).longValueExact();
            var comeco=cursor;
            cursor = cursor.plusMinutes(previsto);
            impressoes.add(new Impressao(comeco,cursor,lote,previsto-minutos));
            restantes -= lote;
            if (restantes > 0) cursor = proximoInicio(cursor.plusMinutes(intervalo));
            if (cursor.isAfter(inicioReal.plusYears(5))) throw new IllegalArgumentException("O prazo de produção ultrapassa cinco anos. Confira o tempo por unidade.");
        }
        return new Previsao(inicioReal,cursor,List.copyOf(impressoes));
    }
    public record Impressao(LocalDateTime inicio,LocalDateTime fim,int unidades,long minutosFolga) {public Impressao(LocalDateTime inicio,LocalDateTime fim,int unidades){this(inicio,fim,unidades,0);}}
    public record Previsao(LocalDateTime inicio, LocalDateTime fim,List<Impressao> impressoes) {}
}

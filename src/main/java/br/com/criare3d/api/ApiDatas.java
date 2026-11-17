package br.com.criare3d.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.*;

@Component
public class ApiDatas {
    private final ZoneId zona;

    public ApiDatas(@Value("${criare.api.fuso-legado:America/Sao_Paulo}") String zona) {
        this.zona = ZoneId.of(zona);
    }

    public OffsetDateTime converter(LocalDateTime data) {
        return data == null ? null : data.atZone(zona).toOffsetDateTime();
    }
}

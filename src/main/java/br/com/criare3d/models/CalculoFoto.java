package br.com.criare3d.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "calculo_foto")
@Getter
@Setter
@NoArgsConstructor
public class CalculoFoto {
    @Id
    @Column(name = "calculo_id")
    private UUID calculoId;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] bytes;

    @Column(columnDefinition = "bytea")
    private byte[] miniatura;
}

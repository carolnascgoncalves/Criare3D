package br.com.criare3d.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "impressora")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Impressora {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private @Getter @Setter UUID id;

    @NotBlank(message="O nome da impressora é obrigatório")
    @Size(min = 2, max = 150, message = "O nome da impressora deve ter entre 2 à 150 caracteres")
    @Column(name= "nome_imp", nullable = false, length = 150)
    private @Getter @Setter String nome;

    @NotBlank(message="O modelo da impressora é obrigatório")
    @Size(min = 2, max = 150, message = "O nome da impressora deve ter entre 2 à 150 caracteres")
    @Column(name="modelo_imp", nullable = false, length = 150)
    private @Getter @Setter String modelo;

    @NotNull(message = "A potencia da impressora é obrigatória")
    @Column(name="potencia_imp", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal potenciaWatts;


    @OneToMany(mappedBy = "impressora")
    private Set<CalculoHistorico> historicos;
}

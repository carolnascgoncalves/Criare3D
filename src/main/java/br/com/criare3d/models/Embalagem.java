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
@Table(name = "embalagem")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Embalagem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private @Getter @Setter UUID id;

    @NotBlank(message="O nome da embalagem é obrigatório")
    @Size(min = 2, max = 150, message = "O nome da embalagem deve ter entre 2 à 150 caracteres")
    @Column(name= "nome_emb", nullable = false, length = 150)
    private @Getter @Setter String nome;

    @NotNull(message="O custo unitário da embalagem é obrigatorio")
    @Column(name="custoUni_emb", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal custoUnitario;


    @OneToMany(mappedBy = "embalagem")
    private Set<CalculoHistorico> historicos;
}

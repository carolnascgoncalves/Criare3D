package br.com.criare3d.models;
import br.com.criare3d.models.Filamento;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "peca")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Peca {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private @Getter @Setter UUID id;

    @NotBlank(message="O nome da peca é obrigatório")
    @Size(min = 2, max = 150, message = "O nome da peca deve ter entre 2 à 150 caracteres")
    @Column(name= "nome_peca", nullable = false, length = 150)
    private @Getter @Setter String nome;

    @NotNull(message = "A quantidade de filamento utilizado na peca é obrigatorio")
    @Column(name="qntFil_peca",nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal quantidadeFilamentoGramas;

    @NotNull(message = "O Tempo de impressão da peca é obrigatorio")
    @Column(name= "tmp_peca",nullable = false)
    private @Getter @Setter Long tempoImpressaoMinutos;

    @OneToMany(mappedBy = "peca")
    private Set<CalculoHistorico> historicos;
}

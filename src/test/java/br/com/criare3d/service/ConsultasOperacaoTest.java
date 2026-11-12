package br.com.criare3d.service;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;
import static org.assertj.core.api.Assertions.assertThat;

/** Valida entidades e HQL sem abrir conexão com banco. As consultas SQL são testadas no PostgreSQL do CI. */
class ConsultasOperacaoTest {
    @Test
    void consultasDoHistoricoEVendasUsamCamposEProjecoesValidos() throws Exception {
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.boot.allow_jdbc_metadata_access",false)
                .applySetting("hibernate.dialect","org.hibernate.dialect.PostgreSQLDialect")
                .applySetting("hibernate.connection.provider_class","org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl")
                .applySetting("hibernate.hbm2ddl.auto","none")
                .build();
        try {
            var metadata = new MetadataSources(registry);
            for (String entidade : new String[]{"PedidoOrcamento","ItemPedido","FotoItemPedido","CalculoFoto","CustoPersonalizado","CalculoHistorico","Configuracao","Embalagem","FalhaProducao",
                    "Filamento","Impressora","ItemCalculoHistorico","ManutencaoImpressora","Peca","PecaArquivoPlaca",
                    "PlanejamentoProducao","Tenant","Usuario","VendaCalculo"}) {
                metadata.addAnnotatedClass(Class.forName("br.com.criare3d.models."+entidade));
            }
            try (var factory = (SessionFactoryImplementor) metadata.buildMetadata().buildSessionFactory()) {
                int consultas = 0;
                for (Class<?> repository : new Class<?>[]{br.com.criare3d.repository.CalculoHistoricoRepository.class,
                        br.com.criare3d.repository.VendaCalculoRepository.class,
                        br.com.criare3d.repository.PlanejamentoProducaoRepository.class,
                        br.com.criare3d.repository.ItemPedidoRepository.class,
                        br.com.criare3d.repository.PedidoOrcamentoRepository.class,
                        br.com.criare3d.repository.FalhaProducaoRepository.class}) {
                    for (var metodo : repository.getMethods()) {
                        Query query = metodo.getAnnotation(Query.class);
                        if (query == null || query.nativeQuery() || query.value().isBlank()) continue;
                        factory.getQueryEngine().getHqlTranslator().translate(query.value(),null);
                        if(!query.countQuery().isBlank())factory.getQueryEngine().getHqlTranslator().translate(query.countQuery(),null);
                        consultas++;
                    }
                }
                assertThat(consultas).isGreaterThan(15);
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}

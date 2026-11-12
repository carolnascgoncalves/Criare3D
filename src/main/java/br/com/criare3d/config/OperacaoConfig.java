package br.com.criare3d.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableScheduling
public class OperacaoConfig {
    @Bean public Clock clock() { return Clock.system(ZoneId.of("America/Sao_Paulo")); }

    @Bean public ThreadPoolTaskExecutor notificacoesExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notificacoes-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        return executor;
    }
}

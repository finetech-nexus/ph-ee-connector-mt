package org.mifos.connector.mt.config;

import io.camunda.zeebe.client.ZeebeClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "zeebe.enabled", havingValue = "true", matchIfMissing = true)
public class ZeebeClientConfiguration {

    @Value("${zeebe.broker.contactpoint}")
    private String contactPoint;

    @Value("${zeebe.client.max-execution-threads}")
    private int maxThreads;

    @Value("${zeebe.client.poll-interval}")
    private int pollIntervalMs;

    @Bean(destroyMethod = "close")
    public ZeebeClient zeebeClient() {
        return ZeebeClient.newClientBuilder()
                .gatewayAddress(contactPoint)
                .usePlaintext()
                .defaultJobPollInterval(Duration.ofMillis(pollIntervalMs))
                .numJobWorkerExecutionThreads(maxThreads)
                .build();
    }
}

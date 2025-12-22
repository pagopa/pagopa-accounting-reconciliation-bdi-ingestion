package it.pagopa.accounting.reconciliation.bdi.ingestion.config

import com.azure.identity.DefaultAzureCredentialBuilder
import com.microsoft.azure.kusto.data.Client
import com.microsoft.azure.kusto.data.ClientFactory
import com.microsoft.azure.kusto.data.auth.ConnectionStringBuilder
import com.microsoft.azure.kusto.ingest.IngestClientFactory
import com.microsoft.azure.kusto.ingest.QueuedIngestClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KustoConfig(@Value("\${azuredataexplorer.domain}") private val domain: String) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun kustoIngestClient(): QueuedIngestClient {
        logger.info("Initializing Kusto Ingest client")

        return IngestClientFactory.createClient(
            ConnectionStringBuilder.createWithTokenCredential(
                "https://ingest-$domain",
                DefaultAzureCredentialBuilder().build(),
            )
        )
    }

    @Bean
    fun kustoQueryClient(): Client {
        logger.info("Initializing Kusto Query client")
        return ClientFactory.createClient(
            ConnectionStringBuilder.createWithTokenCredential(
                "https://$domain",
                DefaultAzureCredentialBuilder().build(),
            )
        )
    }
}

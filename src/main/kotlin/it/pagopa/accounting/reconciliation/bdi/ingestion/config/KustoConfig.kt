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
class KustoConfig(@Value("\${azuredataexplorer.re.endpoint}") private val endpoint: String) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun reKustoClient(): QueuedIngestClient {
        logger.info("Initializing RE Kusto Ingest client")
        val kcsb =
            ConnectionStringBuilder.createWithTokenCredential(
                "https://ingest-$endpoint",
                DefaultAzureCredentialBuilder().build(),
            )
        return IngestClientFactory.createClient(kcsb)
    }

    @Bean
    fun reKustoQueryClient(): Client {
        logger.info("Initializing RE Kusto Query client")
        val kcsb =
            ConnectionStringBuilder.createWithTokenCredential(
                "https://$endpoint",
                DefaultAzureCredentialBuilder().build(),
            )
        return ClientFactory.createClient(kcsb)
    }
}

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
class KustoConfig {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean(name = ["reKustoClient"])
    fun reKustoClient(
        @Value("\${azuredataexplorer.re.endpoint}") endpoint: String
    ): QueuedIngestClient {

        logger.info("Initializing RE Kusto client")

        val ingestionEndpoint = "https://ingest-$endpoint"

        // 1. Create the DefaultAzureCredential.
        // In an AKS pod with Workload Identity enabled, this automatically
        // detects the OIDC token injected into the container.
        val credential = DefaultAzureCredentialBuilder().build()

        // 2. Configure the Kusto Connection String to use the TokenCredential
        val kcsb = ConnectionStringBuilder.createWithTokenCredential(ingestionEndpoint, credential)

        // 3. Create the Ingest Client (Queued Ingestion is recommended for high throughput)
        return IngestClientFactory.createClient(kcsb)
    }

    @Bean(name = ["reKustoQueryClient"])
    fun reKustoQueryClient(@Value("\${azuredataexplorer.re.endpoint}") endpoint: String): Client {
        logger.info("Initializing RE Kusto Query client for Startup Check")

        val queryEndpoint = "https://$endpoint"

        val credential = DefaultAzureCredentialBuilder().build()
        val kcsb = ConnectionStringBuilder.createWithTokenCredential(queryEndpoint, credential)

        return ClientFactory.createClient(kcsb)
    }
}

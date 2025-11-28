package it.pagopa.accounting.reconciliation.bdi.ingestion.config

import com.azure.identity.DefaultAzureCredentialBuilder
import com.microsoft.azure.kusto.data.Client
import com.microsoft.azure.kusto.data.ClientFactory
import com.microsoft.azure.kusto.data.auth.ConnectionStringBuilder
import com.microsoft.azure.kusto.data.auth.endpoints.KustoTrustedEndpoints
import com.microsoft.azure.kusto.data.auth.endpoints.MatchRule
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
        @Value("\${azuredataexplorer.re.endpoint}") endpoint: String,
        @Value("\${azuredataexplorer.re.clientId}") clientId: String,
        @Value("\${azuredataexplorer.re.applicationKey}") applicationKey: String,
        @Value("\${azuredataexplorer.re.applicationTenantId}") tenantId: String,
    ): QueuedIngestClient {

        logger.info("Initializing RE Kusto client")

        val ingestionEndpoint = "https://ingest-$endpoint"
        //        val csb: ConnectionStringBuilder? =
        //            ConnectionStringBuilder.createWithAadApplicationCredentials(
        //                ingestionEndpoint,
        //                clientId,
        //                applicationKey,
        //                tenantId,
        //            )
        //        val csb: ConnectionStringBuilder? =
        //            ConnectionStringBuilder.createWithAadManagedIdentity(
        //
        // "https://dataexplorer.azure.com/?cluster=ingest-pagopaddataexplorer.westeurope&workspace=empty",
        //                "cf9b8103-75d1-4275-ae93-24a2c745625d",
        //            )
        //        return IngestClientFactory.createClient(csb)

        val clusterUri = ingestionEndpoint

        // 1. Create the DefaultAzureCredential.
        // In an AKS pod with Workload Identity enabled, this automatically
        // detects the OIDC token injected into the container.
        val credential = DefaultAzureCredentialBuilder().build()

        // 2. Configure the Kusto Connection String to use the TokenCredential
        val kcsb = ConnectionStringBuilder.createWithTokenCredential(clusterUri, credential)

        // 3. Create the Ingest Client (Queued Ingestion is recommended for high throughput)
        return IngestClientFactory.createClient(kcsb)
    }
}

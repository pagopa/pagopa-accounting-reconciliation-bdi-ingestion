package it.pagopa.accounting.reconciliation.bdi.ingestion.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.azure.kusto.ingest.IngestClient
import com.microsoft.azure.kusto.ingest.IngestionProperties
import com.microsoft.azure.kusto.ingest.source.StreamSourceInfo
import java.io.ByteArrayInputStream
import java.time.Duration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class IngestionService(
    private val ingestClient: IngestClient,
    private val objectMapper: ObjectMapper,
    @Value("\${azuredataexplorer.database}") private val database: String,
    @Value("\${azuredataexplorer.database.table}") private val table: String,
    // @Value("\${azuredataexplorer.mapping.name}") private val mappingName: String,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun <T : Any> ingestDataStream(dataStream: Flux<T>): Mono<Unit> {
        return dataStream
            .bufferTimeout(1000, Duration.ofSeconds(5))
            .flatMap { batch -> sendBatchToAdx(batch) }
            .then()
            .thenReturn(Unit)
    }

    private fun <T : Any> sendBatchToAdx(batch: List<T>): Mono<Unit> {
        return Mono.fromCallable {
                val jsonPayload = batch.joinToString("\n") { objectMapper.writeValueAsString(it) }
                val inputStream = ByteArrayInputStream(jsonPayload.toByteArray())

                val ingestionProperties = IngestionProperties(database, table)
                ingestionProperties.reportLevel =
                    IngestionProperties.IngestionReportLevel.FAILURES_AND_SUCCESSES
                ingestionProperties.dataFormat = IngestionProperties.DataFormat.JSON
                // ingestionProperties.ingestionMapping.setIngestionMappingReference(mappingName,IngestionMapping.IngestionMappingKind.JSON)

                val sourceInfo = StreamSourceInfo(inputStream)
                ingestClient.ingestFromStream(sourceInfo, ingestionProperties)
            }
            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
            .map { Unit }
    }
}

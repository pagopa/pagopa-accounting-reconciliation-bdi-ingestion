package it.pagopa.accounting.reconciliation.bdi.ingestion.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.microsoft.azure.kusto.ingest.IngestClient
import com.microsoft.azure.kusto.ingest.IngestionProperties
import com.microsoft.azure.kusto.ingest.source.StreamSourceInfo
import java.io.ByteArrayInputStream
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class IngestionService(
    private val ingestClient: IngestClient,
    private val objectMapper: ObjectMapper,
    @Value("\${azuredataexplorer.database}") private val database: String,
    @Value("\${azuredataexplorer.database.table}") private val table: String,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val ingestionProperties: IngestionProperties by lazy {
        IngestionProperties(database, table).apply {
            reportLevel = IngestionProperties.IngestionReportLevel.FAILURES_AND_SUCCESSES
            dataFormat = IngestionProperties.DataFormat.JSON
        }
    }

    // Create a copy of the objectMapper, but it can handle the Instant kotlin class
    private val ingestionMapper: ObjectMapper by lazy {
        objectMapper.copy().registerModule(JavaTimeModule())
    }

    fun <T : Any> ingestElement(element: T): Mono<Unit> {
        return Mono.fromCallable {
                val jsonBytes = ingestionMapper.writeValueAsBytes(element)
                StreamSourceInfo(ByteArrayInputStream(jsonBytes))
            }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { ingestClient.ingestFromStreamAsync(it, ingestionProperties) }
            .doOnSuccess { logger.debug("Element successfully sent to ingestion queue") }
            .map {}
    }
}

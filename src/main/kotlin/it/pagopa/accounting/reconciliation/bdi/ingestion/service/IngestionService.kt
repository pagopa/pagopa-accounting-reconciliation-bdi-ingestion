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
    @Value("\${azuredataexplorer.re.database}") private val database: String,
    @Value("\${azuredataexplorer.re.table}") private val table: String,
    @Value("\${azuredataexplorer.re.mapping-name}") private val mappingName: String,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // Funzione principale che accetta un flusso di dati
    fun <T : Any> ingestDataStream(dataStream: Flux<T>): Mono<Void> {
        logger.info("ingestDataStream called for the dataStream: $dataStream")
        return dataStream
            // Raggruppa fino a 1000 elementi O aspetta 5 secondi (strategia buffer)
            .bufferTimeout(1000, Duration.ofSeconds(5))
            .flatMap { batch -> sendBatchToAdx(batch) }
            .then()
    }

    private fun <T : Any> sendBatchToAdx(batch: List<T>): Mono<Void> {
        logger.info("sendBatchToAdx called")
        return Mono.fromCallable {
                // Convertiamo la lista di oggetti in una singola stringa JSON separata da newline
                // (NDJSON)
                // oppure un array JSON, a seconda di come è configurato il mapping su ADX.
                // Solitamente per lo stream si usa multiline JSON.
                val jsonPayload = batch.joinToString("\n") { objectMapper.writeValueAsString(it) }
                val inputStream = ByteArrayInputStream(jsonPayload.toByteArray())

                val ingestionProperties = IngestionProperties(database, table)
                ingestionProperties.reportLevel =
                    IngestionProperties.IngestionReportLevel.FAILURES_ONLY
                ingestionProperties.dataFormat = IngestionProperties.DataFormat.JSON
                // ingestionProperties.ingestionMapping.setIngestionMappingReference(mappingName,IngestionMapping.IngestionMappingKind.JSON)

                // StreamSourceInfo richiede lo stream e possibilmente la dimensione (0 se ignota)
                val sourceInfo = StreamSourceInfo(inputStream)

                // L'SDK gestisce l'upload verso Azure Storage Queue
                val ingestionResult = ingestClient.ingestFromStream(sourceInfo, ingestionProperties)
                logger.info("IngestionResult: $ingestionResult")
            }
            .subscribeOn(
                reactor.core.scheduler.Schedulers.boundedElastic()
            ) // Spostiamo l'IO bloccante su un thread dedicato
            .then()
    }
}

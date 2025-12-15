package it.pagopa.accounting.reconciliation.bdi.ingestion.scheduledjob

import com.microsoft.azure.kusto.data.Client
import com.microsoft.azure.kusto.data.ClientRequestProperties
import it.pagopa.accounting.reconciliation.bdi.ingestion.exceptions.MatchingJobException
import java.time.Duration
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.util.retry.Retry

@Service
class DataMatchingScheduledJob(
    private val kustoClient: Client,
    @Value("\${matching-job.query.timeout}") private val timeout: Long,
    @Value("\${azuredataexplorer.database}") private val database: String,
    @Value("\${matching-job.query.bdi.timeshift}") private val bdiTimeshift: Int,
    @Value("\${matching-job.query.fdi.timeshift}") private val fdiTimeshift: Int,
    @Value("\${matching-job.database.table.bdi}") private val bdiTable: String,
    @Value("\${matching-job.database.table.fdi}") private val fdiTable: String,
    @Value("\${matching-job.database.table.matching}") private val matchingTable: String,
    @Value("\${matching-job.query.causale.regex}") private val regexCausaleQuery: String,
    @Value("\${matching-job.execution.retries}") private val retries: Long,
    @Value("\${matching-job.execution.minBackoffSeconds}") private val minBackoffSeconds: Long,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${matching-job.execution.cron}")
    fun matchingQuery(): Mono<Void> {
        logger.info("Starting Matching scheduled Job")

        val kqlCommand =
            """
            .set-or-append async $matchingTable <|
            let T1 = $bdiTable 
            | where ingestion_time()  > ago(${bdiTimeshift}d) 
            | project CAUSALE, END2END_ID, IMPORTO;
            
            let T2 = $fdiTable 
            | where ingestion_time()  > ago(${fdiTimeshift}d) 
            | project ID_FLUSSO, SOMMA_VERSATA;
        
            let matchEnd2EndId_IdFlusso = T1
            | join kind=inner (T2) on ${'$'}left.END2END_ID == ${'$'}right.ID_FLUSSO
            | project CAUSALE, END2END_ID, IMPORTO, SOMMA_VERSATA, DIFFERENZA_BDI_FDI_IMPORTO = IMPORTO - SOMMA_VERSATA;
            
            let matchCausale_IdFlusso = T1
            //| extend END2END_CAUSALE = extract(".*/(.*)", 1, CAUSALE)
            | extend END2END_CAUSALE = extract("$regexCausaleQuery", 1, CAUSALE)
            | where isnotempty(END2END_CAUSALE)
            | join kind=inner (T2) on ${'$'}left.END2END_CAUSALE == ${'$'}right.ID_FLUSSO
            | project CAUSALE, END2END_ID, IMPORTO, SOMMA_VERSATA, DIFFERENZA_BDI_FDI_IMPORTO = IMPORTO - SOMMA_VERSATA;
            
            matchEnd2EndId_IdFlusso
            | union matchCausale_IdFlusso
            // Remove duplication
            | distinct *
            | extend INSERTED_DATE = now()
            | project-rename 
                IMPORTO_BDI = IMPORTO,
                IMPORTO_FDI = SOMMA_VERSATA
            
            """
                .trimIndent()

        val properties = ClientRequestProperties()
        properties.setTimeoutInMilliSec(TimeUnit.MINUTES.toMillis(timeout))

        return kustoClient
            .executeQueryAsync(database, kqlCommand, properties)
            .retryWhen(
                Retry.backoff(retries, Duration.ofSeconds(minBackoffSeconds))
                    .onRetryExhaustedThrow { _, signal -> MatchingJobException(signal.failure()) }
            )
            .doOnNext { logger.info("Matching job complete successfully.") }
            .then()
    }
}

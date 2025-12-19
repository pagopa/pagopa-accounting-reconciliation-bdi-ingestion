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
    @Value("\${matching-job.query.bdi.timeshift}") private val bdiTimeshift: String,
    @Value("\${matching-job.query.fdi.timeshift}") private val fdiTimeshift: String,
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

        /*
           This KQL query filter the bdiTable and the fdiTable base on the timeshift set,
           then produce a `fullouter` join of the two table:
           - In the first case the join use the END2END_ID and the ID_FLUSSO as key
           - In the second case a new column is created using a regex for extract the END2END_ID from the CAUSALE,
             named END2END_CAUSALE, and used as key with ID_FLUSSO in the join
            In both the case a new column named DIFFERENZA_BDI_FDI_IMPORTO is added, containing the difference
            between the IMPORTO (BDI data for the amount of the payment) and the SOMMA_VERSATA ( FDR data for the
            amount of the payment registered in FLUSSO DI RENDICONTAZIONE).
            This data is useful to understand if the two data match ( value == 0 ) or no ( value !== 0 ).
            After that the two table produced with the join are unified and a `leftanti` join with the existing
            matchingTable is done, for remove element already present, a new column with INSERTED_DATE is added and
            the then append the result.
        */
        val kqlCommand =
            """
            .set-or-append async $matchingTable <|
            let T1 = materialize(
            $bdiTable 
            | where ingestion_time()  > ago(${bdiTimeshift})
            | where END2END_ID != "NOT PROVIDED"
            | summarize arg_max(INSERTED_TIMESTAMP, *) by END2END_ID
            | project CAUSALE, END2END_ID, IMPORTO);
            
            let T2 =  materialize( 
            $fdiTable 
            | where ingestion_time()  > ago(${fdiTimeshift})
            | summarize arg_max(INSERTED_TIMESTAMP, *) by ID_FLUSSO
            | project ID_FLUSSO, SOMMA_VERSATA);
        
            let matchEnd2EndId_IdFlusso = T1
            | where isnotempty(END2END_ID)
            | join kind=inner hint.strategy=shuffle (T2) on ${'$'}left.END2END_ID == ${'$'}right.ID_FLUSSO
            | project CAUSALE, END2END_ID, ID_FLUSSO, IMPORTO, SOMMA_VERSATA, DIFFERENZA_BDI_FDI_IMPORTO = IMPORTO - SOMMA_VERSATA;
            
            let matchCausale_IdFlusso = T1
            //| extend END2END_CAUSALE = extract(".*/(.*)", 1, CAUSALE)
            | extend END2END_CAUSALE = extract("$regexCausaleQuery", 1, CAUSALE)
            | where isnotempty(END2END_CAUSALE)
            | join kind=inner hint.strategy=shuffle (T2) on ${'$'}left.END2END_CAUSALE == ${'$'}right.ID_FLUSSO
            | project CAUSALE, END2END_ID, ID_FLUSSO, IMPORTO, SOMMA_VERSATA, DIFFERENZA_BDI_FDI_IMPORTO = IMPORTO - SOMMA_VERSATA;
            
            let newData = matchEnd2EndId_IdFlusso
            | union matchCausale_IdFlusso
            // Remove duplication
            | summarize take_any(*) by END2END_ID, ID_FLUSSO;
            
            newData
            // The `leftanti` join exclude the existing data in the matching table to avoid duplication
            | join kind=leftanti hint.strategy=shuffle (
                $matchingTable 
                    | project END2END_ID, ID_FLUSSO
            ) on END2END_ID, ID_FLUSSO
            | extend INSERTED_DATE = now()
            | project-rename 
                IMPORTO_BDI = IMPORTO,
                IMPORTO_FDI = SOMMA_VERSATA    
            """
                .trimIndent()

        logger.info(kqlCommand)

        val properties = ClientRequestProperties()
        properties.setTimeoutInMilliSec(TimeUnit.MINUTES.toMillis(timeout))

        return kustoClient
            .executeMgmtAsync(database, kqlCommand, properties)
            .retryWhen(
                Retry.backoff(retries, Duration.ofSeconds(minBackoffSeconds))
                    .onRetryExhaustedThrow { _, signal -> MatchingJobException(signal.failure()) }
            )
            .doOnNext { it ->
                logger.info("Matching job complete successfully.")
                val rowsCount = it.primaryResults.count()
                logger.info("Rows added: $rowsCount")
            }
            .then()
    }
}

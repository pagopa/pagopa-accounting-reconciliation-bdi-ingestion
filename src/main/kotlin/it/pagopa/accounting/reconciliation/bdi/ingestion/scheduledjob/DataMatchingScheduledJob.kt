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
    @Value("\${matching-job.query.fdr.timeshift}") private val fdrTimeshift: String,
    @Value("\${matching-job.database.table.bdi}") private val bdiTable: String,
    @Value("\${matching-job.database.table.fdr}") private val fdrTable: String,
    @Value("\${matching-job.database.table.matching}") private val matchingTable: String,
    @Value("\${matching-job.query.matching.timeshift}") private val matchingTableTimeshift: String,
    @Value("\${matching-job.query.causale.regex}") private val regexCausaleQuery: String,
    @Value("\${matching-job.execution.retries}") private val retries: Long,
    @Value("\${matching-job.execution.minBackoffSeconds}") private val minBackoffSeconds: Long,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${matching-job.execution.cron}")
    fun matchingQuery(): Mono<Void> {
        logger.info("Starting Matching scheduled Job")

        /*
           This KQL query filter the bdiTable and the fdrTable base on the timeshift set,
           then produce a `leftouter` join of the two table:
           - A new column is created using a regex for extract the END2END_ID from the CAUSALE,
             named END2END_CAUSALE, and used as key with ID_FLUSSO in the join
            In both the case a new column named DIFFERENZA_BDI_FDI_IMPORTO is added, containing the difference
            between the IMPORTO (BDI data for the amount of the payment) and the SOMMA_VERSATA ( FDR data for the
            amount of the payment registered in FLUSSO DI RENDICONTAZIONE).
            This data is useful to understand if the two data match ( value == 0 ) or no ( value !== 0 ).
            After that the two table produced with the join are unified and a `leftouter` join with the existing
            matchingTable is done, for remove element already present, but add element with a new value of
            DIFFERENZA_BDI_FDI_IMPORTO, a new column with INSERTED_DATE is added and the then append the result.
        */
        val kqlCommand =
            """
            .append async $matchingTable <|
            let T1 = materialize( 
                $bdiTable
                | where ingestion_time()  > ago($bdiTimeshift)
                | summarize IMPORTO = sum(IMPORTO) by CAUSALE, END2END_ID, BANCA_ORDINANTE
                | project CAUSALE, END2END_ID, IMPORTO, BANCA_ORDINANTE
            );
        
            let T2 = materialize(
                $fdrTable
                | where ingestion_time()  > ago($fdrTimeshift)
                | summarize arg_max(INSERTED_TIMESTAMP, *) by ID_FLUSSO
                | project ID_FLUSSO, SOMMA_VERSATA, ID_DOMINIO, PSP
            );
        
            let matchCausale_IdFlusso = T1
                | where isnotempty(CAUSALE)
                | extend END2END_CAUSALE = extract("$regexCausaleQuery", 1, CAUSALE)
                //| where isnotempty(END2END_CAUSALE)
                | join kind=leftouter hint.strategy=shuffle (T2) on ${'$'}left.END2END_CAUSALE == ${'$'}right.ID_FLUSSO
                | extend DIFFERENZA_BDI_FDI_IMPORTO = IMPORTO - SOMMA_VERSATA;
        
            let ExistingData = $matchingTable
            | where ingestion_time()  > ago($matchingTableTimeshift);
        
            matchCausale_IdFlusso
            | join kind=leftouter (ExistingData) on CAUSALE, ID_FLUSSO
            | where 
                isempty(CAUSALE1)
                or
                (DIFFERENZA_BDI_FDI_IMPORTO != DIFFERENZA_BDI_FDI_IMPORTO1)
            | extend INSERTED_DATE = now()
            | project  
                END2END_ID, 
                ID_FLUSSO,
                CAUSALE,
                BANCA_ORDINANTE, 
                ID_DOMINIO, 
                PSP, 
                IMPORTO, 
                SOMMA_VERSATA, 
                DIFFERENZA_BDI_FDI_IMPORTO, 
                INSERTED_DATE
            | project-rename 
                IMPORTO_BDI = IMPORTO,
                IMPORTO_FDI = SOMMA_VERSATA   
            """
                .trimIndent()

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
                val table = it.primaryResults
                if (table != null && table.hasNext()) {
                    table.next()
                    val rowsAffected = table.getLong("RowCount")
                    logger.info("Rows added: $rowsAffected")
                } else logger.info("Result is null")
            }
            .then()
    }
}

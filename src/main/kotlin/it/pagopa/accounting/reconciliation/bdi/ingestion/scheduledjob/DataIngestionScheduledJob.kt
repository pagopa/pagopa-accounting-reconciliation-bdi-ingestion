package it.pagopa.accounting.reconciliation.bdi.ingestion.scheduledjob

import it.pagopa.accounting.reconciliation.bdi.ingestion.jobs.AccountingDataIngestionJob
import java.time.Duration
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class DataIngestionScheduledJob(
    private val accountingDataIngestionJob: AccountingDataIngestionJob
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${accounting-data-ingestion-job.execution.cron}")
    fun accountingDataIngestion() {
        val startTime = Instant.now()

        accountingDataIngestionJob
            .process(null)
            .doOnSuccess {
                logger.info(
                    "Accounting data ingestion completed successfully. Downloaded {} files",
                    it,
                )
            }
            .doOnError { logger.error("Exception ingesting BDI accounting data", it) }
            .doFinally {
                logger.info(
                    "Overall processing completed. Elapsed time: [{}]",
                    Duration.between(startTime, Instant.now()),
                )
            }
            .onErrorResume { Mono.empty() }
            .subscribe()
    }
}

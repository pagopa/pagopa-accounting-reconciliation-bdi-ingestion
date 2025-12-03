package it.pagopa.accounting.reconciliation.bdi.ingestion.scheduledjob

import it.pagopa.accounting.reconciliation.bdi.ingestion.clients.BdiClient
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingZipDocument
import it.pagopa.accounting.reconciliation.bdi.ingestion.exceptions.AccountingFilesNotRetrievedException
import it.pagopa.accounting.reconciliation.bdi.ingestion.service.ReactiveP7mZipService
import it.pagopa.generated.bdi.model.FileMetadataDto
import java.time.Duration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.util.retry.Retry

@Service
class DataIngestionScheduledJob(
    private val bdiClient: BdiClient,
    private val reactiveP7mZipService: ReactiveP7mZipService,
    @Value("\${accounting-data-ingestion-job.retries}") private val retries: Long,
    @Value("\${accounting-data-ingestion-job.minBackoffSeconds}")
    private val minBackoffSeconds: Long,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${accounting-data-ingestion-job.execution.cron}")
    fun accountingDataIngestion() {
        logger.info("Starting accounting data ingestion scheduled job")
        bdiClient
            .getAvailableAccountingFiles()
            .flatMapIterable { it.files }
            .filterWhen { shouldDownloadFile(it) }
            .map { fileMetadataDto ->
                val accountingZipDocument =
                    AccountingZipDocument(
                        filename = fileMetadataDto.fileName,
                        status = "",
                    ) // TODO: set status
                // TODO: write on zip table
                accountingZipDocument
            }
            .retryWhen(
                Retry.backoff(retries, Duration.ofSeconds(minBackoffSeconds))
                    .onRetryExhaustedThrow { _, signal ->
                        AccountingFilesNotRetrievedException(signal.failure())
                    }
            )
            .doOnNext { logger.info("Retrieved BDI accounting file list successfully.") }
            .flatMap({ reactiveP7mZipService.processZipFile(it) }, 5)
            .subscribe()
    }

    private fun shouldDownloadFile(file: FileMetadataDto): Mono<Boolean> {
        return Mono.just(file.isRegularFile && !file.isDirectory && file.size > 0)
        // TODO: Check on zip table if the file was already downloaded
    }
}

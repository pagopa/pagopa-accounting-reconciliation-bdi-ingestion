package it.pagopa.accounting.reconciliation.bdi.ingestion.scheduledjob

import it.pagopa.accounting.reconciliation.bdi.ingestion.clients.BdiClient
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingZipDocument
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingZipStatus
import it.pagopa.accounting.reconciliation.bdi.ingestion.exceptions.AccountingFilesNotRetrievedException
import it.pagopa.accounting.reconciliation.bdi.ingestion.repositories.AccountingZipRepository
import it.pagopa.accounting.reconciliation.bdi.ingestion.service.ReactiveP7mZipService
import it.pagopa.generated.bdi.model.FileMetadataDto
import java.time.Duration
import java.util.stream.Collectors
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.extra.bool.not
import reactor.util.retry.Retry

@Service
class DataIngestionScheduledJob(
    private val bdiClient: BdiClient,
    private val reactiveP7mZipService: ReactiveP7mZipService,
    private val zipRepository: AccountingZipRepository,
    @Value("\${accounting-data-ingestion-job.retries}") private val retries: Long,
    @Value("\${accounting-data-ingestion-job.minBackoffSeconds}")
    private val minBackoffSeconds: Long,
    @Value("\${accounting-data-ingestion-job.unzip.concurrency}")
    private val zipServiceConcurrency: Int,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${accounting-data-ingestion-job.execution.cron}")
    fun accountingDataIngestion(): Mono<Void> {
        return bdiClient
            .getAvailableAccountingFiles()
            .doFirst { logger.info("Starting accounting data ingestion scheduled job") }
            .flatMapMany { response ->
                val candidates = response.files.filter { it.isDownloadableCandidate }

                if (candidates.isEmpty()) {
                    return@flatMapMany Flux.empty()
                }

                val candidateNames = candidates.map { it.fileName }

                zipRepository
                    .findByFilenameIn(candidateNames)
                    .map { it.filename }
                    .collect(Collectors.toSet())
                    .flatMapMany { existingFilenames ->
                        Flux.fromIterable(candidates).filter { candidate ->
                            !existingFilenames.contains(candidate.fileName)
                        }
                    }
            }
            .flatMap { fileMetadataDto ->
                logger.info("Saving ZIP filename: ${fileMetadataDto.fileName}")
                val accountingZipDocument =
                    AccountingZipDocument(
                        filename = fileMetadataDto.fileName,
                        status = AccountingZipStatus.TO_DOWNLOAD,
                    )
                zipRepository.save(accountingZipDocument)
            }
            .retryWhen(
                Retry.backoff(retries, Duration.ofSeconds(minBackoffSeconds))
                    .onRetryExhaustedThrow { _, signal ->
                        AccountingFilesNotRetrievedException(signal.failure())
                    }
            )
            .doOnComplete { logger.info("Retrieved BDI accounting file list successfully.") }
            .flatMap({ reactiveP7mZipService.processZipFile(it) }, zipServiceConcurrency)
            .then()
    }

    private val FileMetadataDto.isDownloadableCandidate: Boolean
        get() = isRegularFile && !isDirectory && size > 0
}

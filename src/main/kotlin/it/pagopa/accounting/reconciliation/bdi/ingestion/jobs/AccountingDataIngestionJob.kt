package it.pagopa.accounting.reconciliation.bdi.ingestion.jobs

import it.pagopa.accounting.reconciliation.bdi.ingestion.clients.BdiClient
import it.pagopa.accounting.reconciliation.bdi.ingestion.jobs.config.JobConfiguration
import it.pagopa.accounting.reconciliation.bdi.ingestion.service.IngestionService
import it.pagopa.accounting.reconciliation.bdi.ingestion.service.ReactiveP7mZipService
import it.pagopa.accounting.reconciliation.bdi.ingestion.service.XmlParserService
import it.pagopa.generated.bdi.model.FileMetadataDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Accounting data ingestion job: this job will get all available accounting file from BDI and will
 * download, decrypt, unzip and save the content of the files that have not already been downloaded
 */
@Component
class AccountingDataIngestionJob(
    private val bdiClient: BdiClient,
    private val ingestionService: IngestionService,
    private val reactiveP7mZipService: ReactiveP7mZipService,
    private val xmlParserService: XmlParserService,
) : ScheduledJob<JobConfiguration, Long> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun id(): String = "bdi-accounting-data-ingestion"

    // TODO: save bdiAccountingData content on Data Explorer
    // TODO: save downloaded file name
    override fun process(configuration: JobConfiguration?): Mono<Long> {
        logger.info("Starting BDI accounting data ingestion job")
        return bdiClient
            .getAvailableAccountingFiles()
            .onErrorResume { Mono.empty() }
            .flatMapIterable { it.files }
            .filterWhen { shouldDownloadFile(it) }
            .doOnNext { logger.info("Downloading file: ${it.fileName}") }
            .transform {
                ingestionService.ingestDataStream(
                    it.map { it.fileName }
                ) // Create a DTO with only file name as parameter
                it
            }
            .onErrorResume { e ->
                logger.error("", e)
                Flux.empty()
            }
            .flatMap(
                { fileMetadataDto ->
                    bdiClient
                        .getAccountingFile(fileMetadataDto.fileName)
                        .flatMapMany { resource ->
                            // stream data pipeline: Stream -> Decrypt -> Unzip -> Parse -> Object
                            reactiveP7mZipService.extractAndMap(
                                p7mZipInputStream = resource.inputStream,
                                entryNameFilter = { fileName ->
                                    fileName.endsWith(".xml", ignoreCase = true)
                                },
                                mapper = { stream ->
                                    xmlParserService.parseAccountingXmlFromStream(stream)
                                },
                            )
                        }
                        .doOnNext { bdiAccountingData ->
                            logger.debug(
                                "Processed: [end2endId={}, causale={}, importo={}, bancaOrdinante={}]",
                                bdiAccountingData.end2endId,
                                bdiAccountingData.causale,
                                bdiAccountingData.importo,
                                bdiAccountingData.bancaOrdinante,
                            )
                        }
                        .onErrorResume { e ->
                            if (e is IllegalArgumentException) {
                                logger.warn(
                                    "Skipping empty or invalid P7M file: ${fileMetadataDto.fileName}"
                                )
                            } else {
                                logger.error(
                                    "Unexpected error processing file: ${fileMetadataDto.fileName}",
                                    e,
                                )
                            }
                            Mono.empty()
                        }
                },
                5,
            )
            .count()
    }

    private fun shouldDownloadFile(file: FileMetadataDto): Mono<Boolean> {
        return Mono.just(file.isRegularFile && !file.isDirectory && file.size > 0)
        // TODO: Check if the file was already downloaded
    }
}

package it.pagopa.accounting.reconciliation.bdi.ingestion.jobs

import it.pagopa.accounting.reconciliation.bdi.ingestion.clients.BdiClient
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.BdiAccounting
import it.pagopa.accounting.reconciliation.bdi.ingestion.jobs.config.JobConfiguration
import it.pagopa.accounting.reconciliation.bdi.ingestion.service.DataExplorerQueryService
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
    private val dataExplorerQueryService: DataExplorerQueryService
) : ScheduledJob<JobConfiguration, Long> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun id(): String = "bdi-accounting-data-ingestion"

    override fun process(configuration: JobConfiguration?): Mono<Long> {
        logger.info("Starting BDI accounting data ingestion job")
        return bdiClient
            .getAvailableAccountingFiles()
            .onErrorResume { Mono.empty() }
            .flatMapIterable { it.files }
            .filterWhen { shouldDownloadFile(it) }
            .collectList()
            .flatMapMany { dataExplorerQueryService.getAllNotSavedFile(it) }
            .doOnNext { logger.info("Downloading file: ${it.fileName}") }
            .flatMap(
                { fileMetadataDto ->
                    val dataStream: Flux<BdiAccounting> =
                        bdiClient
                            .getAccountingFile(fileMetadataDto.fileName)
                            .flatMapMany { resource ->
                                reactiveP7mZipService.extractAndMap(
                                    p7mZipInputStream = resource.inputStream,
                                    entryNameFilter = { it.endsWith(".xml", ignoreCase = true) },
                                    mapper = { xmlName, stream ->
                                        val accountingData =
                                            xmlParserService.parseAccountingXmlFromStream(stream)
                                        BdiAccounting(
                                            zipFileName = fileMetadataDto.fileName,
                                            xmlFileName = xmlName,
                                            end2endId = accountingData.end2endId,
                                            causale = accountingData.causale,
                                            importo = accountingData.importo,
                                            bancaOrdinante = accountingData.bancaOrdinante,
                                        )
                                    },
                                )
                            }
                            .doOnNext {
                                logger.debug(
                                    "Saving: Zip={}, Xml={}, end2endId={}, causale={}, importo={}, bancaOrdinante={}",
                                    it.zipFileName,
                                    it.xmlFileName,
                                    it.end2endId,
                                    it.causale,
                                    it.importo,
                                    it.bancaOrdinante,
                                )
                            }
                    // Save data on Data Explorer
                    ingestionService.ingestDataStream(dataStream).onErrorResume { e ->
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

package it.pagopa.accounting.reconciliation.bdi.ingestion.service

import it.pagopa.accounting.reconciliation.bdi.ingestion.clients.BdiClient
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingXmlDocument
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingXmlStatus
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingZipDocument
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingZipStatus
import it.pagopa.accounting.reconciliation.bdi.ingestion.exceptions.AccountingZipFileProcessingException
import it.pagopa.accounting.reconciliation.bdi.ingestion.repositories.AccountingXmlRepository
import it.pagopa.accounting.reconciliation.bdi.ingestion.repositories.AccountingZipRepository
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.Security
import java.time.Duration
import java.util.zip.ZipInputStream
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class ReactiveP7mZipService(
    private val bdiClient: BdiClient,
    private val xmlParserService: XmlParserService,
    private val zipRepository: AccountingZipRepository,
    private val xmlRepository: AccountingXmlRepository,
    @Value("\${accounting-data-ingestion-job.concurrency_parsing}")
    private val parsingServiceConcurrency: Int,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        if (Security.getProvider("BC") == null) Security.addProvider(BouncyCastleProvider())
    }

    fun processZipFile(accountingZipDocument: AccountingZipDocument): Mono<Unit> {
        val filename = accountingZipDocument.filename
        logger.debug("Processing ZIP file: $filename")
        return bdiClient
            .getAccountingFile(filename)
            .flatMapMany { resource ->
                decryptSignedStream(filename, resource.inputStream).onErrorResume {
                    Mono.error {
                        AccountingZipFileProcessingException(
                            "Error decrypting/unzipping file $filename: ${it.message}",
                            it,
                        )
                    }
                }
            }
            .buffer(10)
            .zipWith(Flux.interval(Duration.ofMillis(1000)).onBackpressureDrop())
            .concatMap { tuple ->
                xmlRepository
                    .saveAll(tuple.t1)
                    .flatMap(
                        { savedXml ->
                            xmlParserService.processXmlFile(savedXml).onErrorResume { e ->
                                logger.error(
                                    "Error processing XML entry inside batch: ${e.message}"
                                )
                                Mono.empty()
                            }
                        },
                        parsingServiceConcurrency,
                    )
            }
            .then(
                Mono.defer {
                    logger.info(
                        "ZIP $filename processing completed. Updating status to DOWNLOADED."
                    )
                    val updatedZipDocument =
                        accountingZipDocument.copy(status = AccountingZipStatus.DOWNLOADED)
                    zipRepository.save(updatedZipDocument)
                }
            )
            .onErrorResume { error ->
                logger.error("Error during ZIP processing", error)
                Mono.empty()
            }
            .thenReturn(Unit)
    }

    private fun decryptSignedStream(
        zipFilename: String,
        p7mZipInputStream: InputStream,
    ): Flux<AccountingXmlDocument> {
        return Flux.create { sink ->
                try {
                    // P7M Unwrapping
                    BufferedInputStream(p7mZipInputStream).use { bufferedIn ->
                        val cmsData = CMSSignedData(bufferedIn)

                        val signedContent =
                            cmsData.signedContent
                                ?: throw IllegalArgumentException("No content found in P7M")

                        val contentStream: InputStream =
                            when (val contentObject = signedContent.content) {
                                is ByteArray -> ByteArrayInputStream(contentObject)
                                is InputStream -> contentObject
                                else ->
                                    error(
                                        "Unexpected content type: ${contentObject?.javaClass?.name}"
                                    )
                            }

                        contentStream.use { rawCmsStream ->
                            processZipEntries(zipFilename, rawCmsStream, sink)
                        }
                    }
                    sink.complete()
                } catch (e: Exception) {
                    sink.error(e)
                }
            }
            .subscribeOn(Schedulers.boundedElastic())
    }

    private fun processZipEntries(
        zipFilename: String,
        decryptedStream: InputStream,
        sink: FluxSink<AccountingXmlDocument>,
    ) {
        BufferedInputStream(decryptedStream).use { bufferedCmsStream ->
            ZipInputStream(bufferedCmsStream).use { zipStream ->
                var entry = zipStream.nextEntry
                while (entry != null && !sink.isCancelled) {
                    if (!entry.isDirectory && entry.name.endsWith(".xml", ignoreCase = true)) {
                        try {
                            val bytes = zipStream.readAllBytes()
                            val contentString = String(bytes, Charsets.UTF_8)
                            val accountingXmlDocument =
                                AccountingXmlDocument(
                                    zipFilename = zipFilename,
                                    filename = entry.name,
                                    xmlContent = contentString,
                                    status = AccountingXmlStatus.TO_PARSE,
                                )
                            sink.next(accountingXmlDocument)
                        } catch (e: Exception) {
                            logger.warn(
                                "Skipping corrupted file entry: ${entry.name}. Reason: ${e.message}"
                            )
                        } finally {
                            zipStream.closeEntry()
                        }
                    }
                    entry = zipStream.nextEntry
                }
            }
        }
    }
}

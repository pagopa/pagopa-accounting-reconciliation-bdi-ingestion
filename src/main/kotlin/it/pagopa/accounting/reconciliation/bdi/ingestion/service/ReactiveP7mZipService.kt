package it.pagopa.accounting.reconciliation.bdi.ingestion.service

import it.pagopa.accounting.reconciliation.bdi.ingestion.clients.BdiClient
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingXmlDocument
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingZipDocument
import java.io.BufferedInputStream
import java.io.InputStream
import java.security.Security
import java.util.zip.ZipInputStream
import org.bouncycastle.cms.CMSSignedDataParser
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class ReactiveP7mZipService(
    private val bdiClient: BdiClient,
    private val xmlParserService: XmlParserService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        if (Security.getProvider("BC") == null) Security.addProvider(BouncyCastleProvider())
    }

    fun processZipFile(accountingZipDocument: AccountingZipDocument): Mono<Unit> {
        logger.info("Processing ZIP file: ${accountingZipDocument.filename}")
        return bdiClient
            .getAccountingFile(accountingZipDocument.filename)
            .flatMapMany { resource -> decryptSignedStream(resource.inputStream) }
            // TODO: write on xml table
            // TODO: update zip table
            .flatMap { xmlParserService.processXmlFile(it) }
            .then()
            .thenReturn(Unit)
    }

    private fun decryptSignedStream(p7mZipInputStream: InputStream): Flux<AccountingXmlDocument> {
        return Flux.create { sink ->
                try {
                    // P7M Unwrapping
                    BufferedInputStream(p7mZipInputStream).use { bufferedIn ->
                        val digestProvider =
                            JcaDigestCalculatorProviderBuilder().setProvider("BC").build()
                        val cmsParser = CMSSignedDataParser(digestProvider, bufferedIn)

                        val signedContent =
                            cmsParser.signedContent
                                ?: throw IllegalArgumentException("No content found in P7M")

                        signedContent.contentStream.use { rawCmsStream ->
                            processZipEntries(rawCmsStream, sink)
                        }
                        // Drain the parser (Required to prevent EOFException)
                        cmsParser.signerInfos
                    }
                    sink.complete()
                } catch (e: Exception) {
                    sink.error(e)
                }
            }
            .subscribeOn(Schedulers.boundedElastic())
    }

    private fun processZipEntries(
        decryptedStream: InputStream,
        sink: FluxSink<AccountingXmlDocument>,
    ) {
        BufferedInputStream(decryptedStream).use { bufferedCmsStream ->
            ZipInputStream(bufferedCmsStream).use { zipStream ->
                var entry = zipStream.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".xml", ignoreCase = true)) {
                        try {
                            val contentString = String(zipStream.readAllBytes(), Charsets.UTF_8)
                            val accountingXmlDocument =
                                AccountingXmlDocument(
                                    zipFilename = "",
                                    filename = entry.name,
                                    xmlContent = contentString,
                                    status = "", // TODO: set status
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

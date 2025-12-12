package it.pagopa.accounting.reconciliation.bdi.ingestion.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingXmlDocument
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingXmlStatus
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.BdiAccountingData
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.OpiRendAnalitico
import it.pagopa.accounting.reconciliation.bdi.ingestion.repositories.AccountingXmlRepository
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class XmlParserService(
    private val ingestionService: IngestionService,
    private val xmlRepository: AccountingXmlRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val xmlMapper =
        XmlMapper().apply {
            registerKotlinModule()
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

    /**
     * Parses an [AccountingXmlDocument] and send the extracted values to the [IngestionService].
     *
     * @param accountingXmlDocument The [AccountingXmlDocument] containing the XML file content.
     */
    fun processXmlFile(accountingXmlDocument: AccountingXmlDocument): Mono<Unit> {
        return Mono.fromCallable {
                logger.info("Processing XML file: ${accountingXmlDocument.filename}")
                val opiRendAnalitico =
                    xmlMapper.readValue(
                        accountingXmlDocument.xmlContent,
                        OpiRendAnalitico::class.java,
                    )

                val end2endId = opiRendAnalitico.movimento?.end2endId
                val causale = opiRendAnalitico.movimento?.causale
                val importo = opiRendAnalitico.movimento?.importo
                val bancaOrdinante =
                    opiRendAnalitico.movimento?.dettaglioMovimento?.entrata?.bancaOrdinante

                BdiAccountingData(end2endId, causale, importo, bancaOrdinante, Instant.now())
            }
            .flatMap { ingestionService.ingestElement(it) }
            .flatMap { _ ->
                logger.info(
                    "XML ${accountingXmlDocument.filename} processing completed. Updating status to PARSED."
                )
                val updatedXmlDocument =
                    accountingXmlDocument.copy(status = AccountingXmlStatus.PARSED)
                // Update XML file status
                xmlRepository.save(updatedXmlDocument)
            }
            .thenReturn(Unit)
            .subscribeOn(Schedulers.boundedElastic())
    }
}

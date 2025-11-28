package it.pagopa.accounting.reconciliation.bdi.ingestion.service

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.BdiAccountingData
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.OpiRendAnalitico
import java.io.InputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class XmlParserService {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val xmlMapper =
        XmlMapper().apply {
            registerKotlinModule()
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            // Configure the mapper to not close the input stream automatically
            factory.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false)
        }

    /**
     * Parses an accounting XML [InputStream] and extract the needed data.
     *
     * @param inputStream The source [InputStream] containing the accounting XML data.
     * @return A [BdiAccountingData] containing the extracted values.
     */
    fun parseAccountingXmlFromStream(inputStream: InputStream): BdiAccountingData {
        val invoice = xmlMapper.readValue(inputStream, OpiRendAnalitico::class.java)

        val end2endId = invoice.movimento?.end2endId
        val causale = invoice.movimento?.causale
        val importo = invoice.movimento?.importo
        val bancaOrdinante = invoice.movimento?.dettaglioMovimento?.entrata?.bancaOrdinante

        return BdiAccountingData(end2endId, causale, importo, bancaOrdinante)
    }
}

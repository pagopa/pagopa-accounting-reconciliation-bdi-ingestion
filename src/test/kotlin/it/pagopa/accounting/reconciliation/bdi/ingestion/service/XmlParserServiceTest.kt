package it.pagopa.accounting.reconciliation.bdi.ingestion.service

import com.fasterxml.jackson.core.JsonProcessingException
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class XmlParserServiceTest {
    private val xmlParserService = XmlParserService()

    @Test
    fun `should parse valid XML and extract all fields correctly`() {
        // pre-requisites
        val xmlContent =
            """
                <OPI_REND_ANALITICO>
                    <movimento>
                        <end2endID>TEST-END2END-ID</end2endID>
                        <causale>Payment 001</causale>
                        <importo>10.50</importo>
                        <dettaglioMovimento>
                            <entrata>
                                <bancaOrdinante>Test Bank</bancaOrdinante>
                            </entrata>
                        </dettaglioMovimento>
                    </movimento>
                </OPI_REND_ANALITICO>
            """
                .trimIndent()

        val inputStream = ByteArrayInputStream(xmlContent.toByteArray(StandardCharsets.UTF_8))

        // test
        val result = xmlParserService.parseAccountingXmlFromStream(inputStream)

        // verifications
        assertThat(result).isNotNull
        assertThat(result.end2endId).isEqualTo("TEST-END2END-ID")
        assertThat(result.causale).isEqualTo("Payment 001")
        assertThat(result.bancaOrdinante).isEqualTo("Test Bank")
        assertThat(result.importo).isEqualByComparingTo(BigDecimal("10.50"))
    }

    @Test
    fun `should return nulls for missing optional fields`() {
        // pre-requisites
        val xmlContent =
            """
                <OPI_REND_ANALITICO>
                    <movimento>
                        <end2endID>TEST-END2END-ID</end2endID>
                        <importo>10.50</importo>
                        </movimento>
                </OPI_REND_ANALITICO>
            """
                .trimIndent()

        val inputStream = ByteArrayInputStream(xmlContent.toByteArray(StandardCharsets.UTF_8))

        // test
        val result = xmlParserService.parseAccountingXmlFromStream(inputStream)

        // verifications
        assertThat(result.end2endId).isEqualTo("TEST-END2END-ID")
        assertThat(result.importo).isEqualByComparingTo(BigDecimal("10.50"))
        assertThat(result.causale).isNull()
        assertThat(result.bancaOrdinante).isNull()
    }

    @Test
    fun `should ignore unknown properties without failing`() {
        // pre-requisites
        val xmlContent =
            """
                <OPI_REND_ANALITICO>
                    <movimento>
                        <UNKNOWN_TAG>data</UNKNOWN_TAG>
                        <end2endID>TEST-END2END-ID</end2endID>
                    </movimento>
                    <EXTRA_HEADER_DATA>data</EXTRA_HEADER_DATA>
                </OPI_REND_ANALITICO>
            """
                .trimIndent()

        val inputStream = ByteArrayInputStream(xmlContent.toByteArray(StandardCharsets.UTF_8))

        // test
        val result = xmlParserService.parseAccountingXmlFromStream(inputStream)

        // verifications
        assertThat(result.end2endId).isEqualTo("TEST-END2END-ID")
        assertThat(result.importo).isNull()
        assertThat(result.causale).isNull()
        assertThat(result.bancaOrdinante).isNull()
    }

    @Test
    fun `should throw exception for malformed XML`() {
        // pre-requisites
        val brokenXml = "<OPI_REND_ANALITICO><movimento>No Closing Tag"
        val inputStream = ByteArrayInputStream(brokenXml.toByteArray(StandardCharsets.UTF_8))

        // test
        assertThatThrownBy { xmlParserService.parseAccountingXmlFromStream(inputStream) }
            .isInstanceOf(JsonProcessingException::class.java)
    }
}

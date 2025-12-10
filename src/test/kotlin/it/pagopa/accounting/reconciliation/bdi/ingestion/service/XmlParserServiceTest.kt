package it.pagopa.accounting.reconciliation.bdi.ingestion.service

import com.fasterxml.jackson.core.JsonProcessingException
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingXmlDocument
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.BdiAccountingData
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class XmlParserServiceTest {
    private val ingestionService: IngestionService = mock()
    private val xmlParserService = XmlParserService(ingestionService)

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

        val accountingXmlDocument =
            AccountingXmlDocument(
                "test-id",
                "test_zipname",
                "test_filename",
                Instant.now(),
                xmlContent,
                "status_test",
            )

        given(ingestionService.ingestElement(any())).willReturn(Mono.just(Unit))

        val captor = argumentCaptor<BdiAccountingData>()

        // test
        StepVerifier.create(xmlParserService.processXmlFile(accountingXmlDocument))
            .expectSubscription()
            .expectNext(Unit)
            .verifyComplete()

        verify(ingestionService).ingestElement(captor.capture())

        val capturedValue = captor.firstValue

        // verifications
        assertThat(capturedValue).isNotNull
        assertThat(capturedValue.END2END_ID).isEqualTo("TEST-END2END-ID")
        assertThat(capturedValue.CAUSALE).isEqualTo("Payment 001")
        assertThat(capturedValue.BANCA_ORDINANTE).isEqualTo("Test Bank")
        assertThat(capturedValue.IMPORTO).isEqualByComparingTo(BigDecimal("10.50"))
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

        val accountingXmlDocument =
            AccountingXmlDocument(
                "test-id",
                "test_zipname",
                "test_filename",
                Instant.now(),
                xmlContent,
                "status_test",
            )

        given(ingestionService.ingestElement(any())).willReturn(Mono.just(Unit))

        val captor = argumentCaptor<BdiAccountingData>()

        // test
        StepVerifier.create(xmlParserService.processXmlFile(accountingXmlDocument))
            .expectSubscription()
            .expectNext(Unit)
            .verifyComplete()

        verify(ingestionService).ingestElement(captor.capture())

        val capturedValue = captor.firstValue

        // verifications
        assertThat(capturedValue.END2END_ID).isEqualTo("TEST-END2END-ID")
        assertThat(capturedValue.IMPORTO).isEqualByComparingTo(BigDecimal("10.50"))
        assertThat(capturedValue.CAUSALE).isNull()
        assertThat(capturedValue.BANCA_ORDINANTE).isNull()
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

        val accountingXmlDocument =
            AccountingXmlDocument(
                "test-id",
                "test_zipname",
                "test_filename",
                Instant.now(),
                xmlContent,
                "status_test",
            )

        given(ingestionService.ingestElement(any())).willReturn(Mono.just(Unit))

        val captor = argumentCaptor<BdiAccountingData>()

        // test
        StepVerifier.create(xmlParserService.processXmlFile(accountingXmlDocument))
            .expectSubscription()
            .expectNext(Unit)
            .verifyComplete()

        verify(ingestionService).ingestElement(captor.capture())

        val capturedValue = captor.firstValue

        // verifications
        assertThat(capturedValue.END2END_ID).isEqualTo("TEST-END2END-ID")
        assertThat(capturedValue.IMPORTO).isNull()
        assertThat(capturedValue.CAUSALE).isNull()
        assertThat(capturedValue.BANCA_ORDINANTE).isNull()
    }

    @Test
    fun `should return nulls for missing fields when there is no movimento`() {
        // pre-requisites
        val xmlContent =
            """
                <OPI_REND_ANALITICO>
               
                </OPI_REND_ANALITICO>
            """
                .trimIndent()

        val accountingXmlDocument =
            AccountingXmlDocument(
                "test-id",
                "test_zipname",
                "test_filename",
                Instant.now(),
                xmlContent,
                "status_test",
            )

        given(ingestionService.ingestElement(any())).willReturn(Mono.just(Unit))

        val captor = argumentCaptor<BdiAccountingData>()

        // test
        StepVerifier.create(xmlParserService.processXmlFile(accountingXmlDocument))
            .expectSubscription()
            .expectNext(Unit)
            .verifyComplete()

        verify(ingestionService).ingestElement(captor.capture())

        val capturedValue = captor.firstValue

        // verifications
        assertThat(capturedValue.END2END_ID).isNull()
        assertThat(capturedValue.IMPORTO).isNull()
        assertThat(capturedValue.CAUSALE).isNull()
        assertThat(capturedValue.BANCA_ORDINANTE).isNull()
    }

    @Test
    fun `should throw exception for malformed XML`() {
        // pre-requisites
        val brokenXml = "<OPI_REND_ANALITICO><movimento>No Closing Tag"

        val accountingXmlDocument =
            AccountingXmlDocument(
                "test-id",
                "test_zipname",
                "test_filename",
                Instant.now(),
                brokenXml,
                "status_test",
            )

        given(ingestionService.ingestElement(any())).willReturn(Mono.just(Unit))

        val captor = argumentCaptor<BdiAccountingData>()

        // test
        StepVerifier.create(xmlParserService.processXmlFile(accountingXmlDocument))
            .expectSubscription()
            .expectError(JsonProcessingException::class.java)
    }
}

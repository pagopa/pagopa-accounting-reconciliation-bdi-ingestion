package it.pagopa.accounting.reconciliation.bdi.ingestion.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.microsoft.azure.kusto.ingest.IngestClient
import com.microsoft.azure.kusto.ingest.IngestionProperties
import com.microsoft.azure.kusto.ingest.result.IngestionResult
import com.microsoft.azure.kusto.ingest.source.StreamSourceInfo
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.BdiAccountingData
import java.io.InputStream
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.mockito.kotlin.given
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

class IngestionServiceTest {

    private val ingestClient: IngestClient = mock()
    private val objectMapper: ObjectMapper = ObjectMapper().registerModule(JavaTimeModule())
    private val ingestionResult: IngestionResult = mock()

    private val databaseName = "test-db"
    private val tableName = "test-table"

    private var ingestionService =
        IngestionService(ingestClient, objectMapper, databaseName, tableName)

    @Test
    fun `ingestDataStream should batch data and send newline delimited JSON to ADX`() {
        // pre-requisites
        val data1 = TestData("item1")

        val bdiAccountingData =
            BdiAccountingData(
                "test_end2end",
                "test_causale",
                BigDecimal(10),
                "test_banca",
                Instant.now(),
            )

        given(ingestClient.ingestFromStream(any(), any())).willReturn(ingestionResult)

        val sourceInfoCaptor = ArgumentCaptor.forClass(StreamSourceInfo::class.java)
        val propsCaptor = ArgumentCaptor.forClass(IngestionProperties::class.java)

        // test
        StepVerifier.create(ingestionService.ingestElement(data1)).expectNext(Unit).verifyComplete()

        // verifications
        verify(ingestClient, times(1))
            .ingestFromStream(sourceInfoCaptor.capture(), propsCaptor.capture())

        val capturedProps = propsCaptor.value
        assertEquals(databaseName, capturedProps.databaseName)
        assertEquals(tableName, capturedProps.tableName)
        assertEquals(IngestionProperties.DataFormat.JSON, capturedProps.dataFormat)
        assertEquals(
            IngestionProperties.IngestionReportLevel.FAILURES_AND_SUCCESSES,
            capturedProps.reportLevel,
        )

        val capturedSourceInfo = sourceInfoCaptor.value
        val inputStream: InputStream = capturedSourceInfo.stream
        val content = String(inputStream.readAllBytes(), StandardCharsets.UTF_8)

        val expectedPayload = """{"id":"item1"}"""

        assertEquals(expectedPayload, content)

        StepVerifier.create(ingestionService.ingestElement(bdiAccountingData))
            .expectNext(Unit)
            .verifyComplete()
    }

    @Test
    fun `ingestDataStream should handle serialization errors gracefully`() {
        // pre-requisites
        val data = TestData("error-item")
        val stream = Flux.just(data)

        val objectMapperMock: ObjectMapper = mock()
        val ingestionServiceFail =
            IngestionService(ingestClient, objectMapperMock, databaseName, tableName)

        given(objectMapperMock.writeValueAsString(data))
            .willThrow(RuntimeException("Serialization error"))

        // test
        StepVerifier.create(ingestionServiceFail.ingestElement(stream))
            .expectError(RuntimeException::class.java)
            .verify()

        // verifications
        verifyNoInteractions(ingestClient)
    }

    data class TestData(val id: String)
}

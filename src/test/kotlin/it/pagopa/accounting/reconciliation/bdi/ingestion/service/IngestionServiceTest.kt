package it.pagopa.accounting.reconciliation.bdi.ingestion.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.azure.kusto.ingest.IngestClient
import com.microsoft.azure.kusto.ingest.IngestionProperties
import com.microsoft.azure.kusto.ingest.result.IngestionResult
import com.microsoft.azure.kusto.ingest.source.StreamSourceInfo
import java.io.InputStream
import java.nio.charset.StandardCharsets
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.mockito.kotlin.given
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

class IngestionServiceTest {

    private val ingestClient: IngestClient = mock()
    private val objectMapper: ObjectMapper = mock()
    private val ingestionResult: IngestionResult = mock()

    private val databaseName = "test-db"
    private val tableName = "test-table"

    private var ingestionService =
        IngestionService(ingestClient, objectMapper, databaseName, tableName)

    @Test
    fun `ingestDataStream should batch data and send newline delimited JSON to ADX`() {
        // pre-requisites
        val data1 = TestData("item1")
        val data2 = TestData("item2")
        val dataStream = Flux.just(data1, data2)

        given(objectMapper.writeValueAsString(data1)).willReturn("""{"id":"item1"}""")
        given(objectMapper.writeValueAsString(data2)).willReturn("""{"id":"item2"}""")
        given(ingestClient.ingestFromStream(any(), any())).willReturn(ingestionResult)

        val sourceInfoCaptor = ArgumentCaptor.forClass(StreamSourceInfo::class.java)
        val propsCaptor = ArgumentCaptor.forClass(IngestionProperties::class.java)

        // test
        StepVerifier.create(ingestionService.ingestDataStream(dataStream)).verifyComplete()

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

        val expectedPayload =
            """{"id":"item1"}
{"id":"item2"}"""

        assertEquals(expectedPayload, content)
    }

    @Test
    fun `ingestDataStream should handle serialization errors gracefully`() {
        // pre-requisites
        val data = TestData("error-item")
        val stream = Flux.just(data)

        given(objectMapper.writeValueAsString(data))
            .willThrow(RuntimeException("Serialization error"))

        // test
        StepVerifier.create(ingestionService.ingestDataStream(stream))
            .expectError(RuntimeException::class.java)
            .verify()

        // verifications
        verifyNoInteractions(ingestClient)
    }

    data class TestData(val id: String)
}

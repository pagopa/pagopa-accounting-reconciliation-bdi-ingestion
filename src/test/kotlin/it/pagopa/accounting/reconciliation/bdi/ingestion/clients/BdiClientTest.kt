package it.pagopa.accounting.reconciliation.bdi.ingestion.clients

import it.pagopa.generated.bdi.api.AccountingApi
import it.pagopa.generated.bdi.model.ListAccountingFiles200ResponseDto
import java.nio.charset.StandardCharsets
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.given
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@TestPropertySource(locations = ["classpath:application.test.properties"])
class BdiClientTest {

    private var bdiAccountingApi: AccountingApi = mock()
    private var bdiClient: BdiClient = BdiClient(bdiAccountingApi)

    @Test
    fun `getAvailableAccountingFiles returns a Mono emitting a ListAccountingFiles200ResponseDto`() {
        // pre-requisites
        val listAccountingFiles200ResponseDto = ListAccountingFiles200ResponseDto()

        given(bdiAccountingApi.listAccountingFiles())
            .willReturn(Mono.just(listAccountingFiles200ResponseDto))

        // test
        StepVerifier.create(bdiClient.getAvailableAccountingFiles())
            .expectNext(listAccountingFiles200ResponseDto)
            .verifyComplete()

        // verifications
        verify(bdiAccountingApi, times(1)).listAccountingFiles()
    }

    @Test
    fun `getAvailableAccountingFiles propagates exception on API error`() {
        // pre-requisites
        val ex =
            WebClientResponseException(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                HttpHeaders.EMPTY,
                null,
                StandardCharsets.UTF_8,
            )

        given(bdiAccountingApi.listAccountingFiles()).willReturn(Mono.error(ex))

        // test
        StepVerifier.create(bdiClient.getAvailableAccountingFiles())
            .expectError(WebClientResponseException::class.java)
            .verify()

        // verifications
        verify(bdiAccountingApi, times(1)).listAccountingFiles()
    }

    @Test
    fun `getAccountingFile returns a Mono emitting a Resource`() {
        // pre-requisites
        val mockResource = mock(Resource::class.java)
        val filename: String = "mock-file.zip"

        given(bdiAccountingApi.getAccountingFile(filename)).willReturn(Mono.just(mockResource))

        // test
        StepVerifier.create(bdiClient.getAccountingFile(filename))
            .expectNext(mockResource)
            .verifyComplete()

        // verifications
        verify(bdiAccountingApi, times(1)).getAccountingFile(filename)
    }

    @Test
    fun `getAccountingFile propagates exception on API error`() {
        val ex =
            WebClientResponseException(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                HttpHeaders.EMPTY,
                null,
                StandardCharsets.UTF_8,
            )
        val fileName = "mock-file.zip"

        given(bdiAccountingApi.getAccountingFile(fileName)).willReturn(Mono.error(ex))

        StepVerifier.create(bdiClient.getAccountingFile(fileName))
            .expectError(WebClientResponseException::class.java)
            .verify()

        // verifications
        verify(bdiAccountingApi, times(1)).getAccountingFile(fileName)
    }
}

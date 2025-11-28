package it.pagopa.accounting.reconciliation.bdi.ingestion.jobs

import it.pagopa.accounting.reconciliation.bdi.ingestion.clients.BdiClient
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.BdiAccountingData
import it.pagopa.accounting.reconciliation.bdi.ingestion.service.ReactiveP7mZipService
import it.pagopa.accounting.reconciliation.bdi.ingestion.service.XmlParserService
import it.pagopa.generated.bdi.model.FileMetadataDto
import it.pagopa.generated.bdi.model.ListAccountingFiles200ResponseDto
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.core.io.Resource
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class AccountingDataIngestionJobTest {

    private val bdiClient: BdiClient = mock()
    private val reactiveP7mZipService: ReactiveP7mZipService = mock()
    private val xmlParserService: XmlParserService = mock()

    private val job = AccountingDataIngestionJob(bdiClient, reactiveP7mZipService, xmlParserService)

    @Test
    fun `process should download, extract and count all valid files`() {
        // pre-requisites
        val file1 = createFileMetadataDto("valid1.zip.p7m")
        val file2 = createFileMetadataDto("valid2.zip.p7m")
        val availableFiles = createListAccountingFiles200ResponseDto(listOf(file1, file2))

        given(bdiClient.getAvailableAccountingFiles()).willReturn(Mono.just(availableFiles))

        val resource1 = mockResource()
        val resource2 = mockResource()
        given(bdiClient.getAccountingFile(file1.fileName)).willReturn(Mono.just(resource1))
        given(bdiClient.getAccountingFile(file2.fileName)).willReturn(Mono.just(resource2))

        val dataA = BdiAccountingData("TEST-END2END-ID", "Payment 001", BigDecimal("10.50"), "Bank")
        val dataB = BdiAccountingData("TEST-END2END-ID-2", "Payment 001", BigDecimal("5"), "Bank")

        given(
                reactiveP7mZipService.extractAndMap<BdiAccountingData>(
                    p7mZipInputStream = any(), // Matches resource1.inputStream
                    entryNameFilter = any(),
                    mapper = any(),
                )
            )
            .willReturn(Flux.just(dataA), Flux.just(dataB))

        // test
        StepVerifier.create(job.process(null)).expectNext(2L).verifyComplete()

        // verifications
        verify(bdiClient, times(1)).getAccountingFile(file1.fileName)
        verify(bdiClient, times(1)).getAccountingFile(file2.fileName)
    }

    @Test
    fun `process should filter out directories and empty files`() {
        // pre-requisites
        val goodFile = createFileMetadataDto("good.zip.p7m", size = 100)
        val directory = createFileMetadataDto("folder", isDirectory = true)
        val emptyFile = createFileMetadataDto("empty.zip.p7m", size = 0)
        val availableFiles =
            createListAccountingFiles200ResponseDto(listOf(goodFile, directory, emptyFile))

        given(bdiClient.getAvailableAccountingFiles()).willReturn(Mono.just(availableFiles))

        val resource = mockResource()
        given(bdiClient.getAccountingFile(goodFile.fileName)).willReturn(Mono.just(resource))

        given(
                reactiveP7mZipService.extractAndMap<BdiAccountingData>(
                    p7mZipInputStream = any(),
                    entryNameFilter = any(),
                    mapper = any(),
                )
            )
            .willReturn(
                Flux.just(
                    BdiAccountingData("TEST-END2END-ID", "Payment 001", BigDecimal("10.50"), "Bank")
                )
            )

        // test
        StepVerifier.create(job.process(null)).expectNext(1L).verifyComplete()

        // verifications
        verify(bdiClient, times(1)).getAccountingFile(goodFile.fileName)
        verify(bdiClient, times(0)).getAccountingFile(directory.fileName)
        verify(bdiClient, times(0)).getAccountingFile(emptyFile.fileName)
    }

    @Test
    fun `process should continue to next file if one file is corrupted`() {
        // pre-requisites
        val corruptFile = createFileMetadataDto("corrupt.zip.p7m")
        val goodFile = createFileMetadataDto("good.zip.p7m")
        val availableFiles = createListAccountingFiles200ResponseDto(listOf(goodFile, corruptFile))

        given(bdiClient.getAvailableAccountingFiles()).willReturn(Mono.just(availableFiles))

        val resCorrupt = mockResource()
        val resGood = mockResource()

        given(bdiClient.getAccountingFile(goodFile.fileName)).willReturn(Mono.just(resGood))
        given(bdiClient.getAccountingFile(corruptFile.fileName)).willReturn(Mono.just(resCorrupt))

        given(
                reactiveP7mZipService.extractAndMap<BdiAccountingData>(
                    p7mZipInputStream = any(),
                    entryNameFilter = any(),
                    mapper = any(),
                )
            )
            .willReturn(
                Flux.error(IllegalArgumentException("No content found in P7M")),
                Flux.just(
                    BdiAccountingData("TEST-END2END-ID", "Payment 001", BigDecimal("10.50"), "Bank")
                ),
            )

        // test
        StepVerifier.create(job.process(null)).expectNext(1L).verifyComplete()

        // verifications
        verify(bdiClient, times(1)).getAccountingFile(goodFile.fileName)
        verify(bdiClient, times(1)).getAccountingFile(corruptFile.fileName)
    }

    @Test
    fun `process should return 0 if file list fetch fails`() {
        // pre-requisites
        given(bdiClient.getAvailableAccountingFiles())
            .willReturn(Mono.error(RuntimeException("API error")))

        // test
        StepVerifier.create(job.process(null)).expectNext(0L).verifyComplete()
    }

    private fun createFileMetadataDto(
        name: String,
        isDirectory: Boolean = false,
        size: Long = 100,
    ): FileMetadataDto {
        val createFileMetadataDto = FileMetadataDto()
        createFileMetadataDto.fileName = name
        createFileMetadataDto.isDirectory = isDirectory
        createFileMetadataDto.isRegularFile = !isDirectory
        createFileMetadataDto.size = size
        createFileMetadataDto.lastModifiedTime = 1762456781293

        return createFileMetadataDto
    }

    private fun createListAccountingFiles200ResponseDto(
        files: List<FileMetadataDto>
    ): ListAccountingFiles200ResponseDto {
        val listAccountingFiles200ResponseDto = ListAccountingFiles200ResponseDto()
        listAccountingFiles200ResponseDto.files = files
        return listAccountingFiles200ResponseDto
    }

    private fun mockResource(): Resource {
        val resource = mock(Resource::class.java)
        given(resource.inputStream).willReturn(ByteArrayInputStream(ByteArray(0)))
        return resource
    }
}

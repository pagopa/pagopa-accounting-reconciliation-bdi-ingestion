package it.pagopa.accounting.reconciliation.bdi.ingestion.scheduledjob

import it.pagopa.accounting.reconciliation.bdi.ingestion.TestUtils
import it.pagopa.accounting.reconciliation.bdi.ingestion.clients.BdiClient
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingZipDocument
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingZipDocumentFilenameDto
import it.pagopa.accounting.reconciliation.bdi.ingestion.exceptions.AccountingFilesNotRetrievedException
import it.pagopa.accounting.reconciliation.bdi.ingestion.repositories.AccountingZipRepository
import it.pagopa.accounting.reconciliation.bdi.ingestion.service.ReactiveP7mZipService
import it.pagopa.generated.bdi.model.ListAccountingFiles200ResponseDto
import java.time.Duration
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class DataIngestionScheduledJobTest {

    private val bdiClient: BdiClient = mock()
    private val reactiveP7mZipService: ReactiveP7mZipService = mock()
    private val zipRepository: AccountingZipRepository = mock()
    private val retries: Long = 2
    private val minBackoffSeconds: Long = 5
    private val zipServiceConcurrency: Int = 1
    private val dataIngestionScheduledJob =
        DataIngestionScheduledJob(
            bdiClient,
            reactiveP7mZipService,
            zipRepository,
            retries,
            minBackoffSeconds,
            zipServiceConcurrency,
        )

    @Test
    fun `Should get files from bdiClient and pass to the reactiveP7mZipService`() {
        // pre-requisites
        val listAccountingFiles = ListAccountingFiles200ResponseDto()
        val fileMetadataDto = TestUtils.fileMetadataDto()
        listAccountingFiles.addFilesItem(fileMetadataDto)

        given(bdiClient.getAvailableAccountingFiles()).willReturn(Mono.just(listAccountingFiles))
        given(reactiveP7mZipService.processZipFile(any())).willReturn(Mono.just(Unit))
        given(zipRepository.findByFilenameIn(any())).willReturn(Flux.empty())
        given(zipRepository.save(any())).willAnswer { invocation ->
            val entityToSave = invocation.getArgument<AccountingZipDocument>(0)
            Mono.just(entityToSave)
        }

        // test
        StepVerifier.create(dataIngestionScheduledJob.accountingDataIngestion())
            .expectSubscription()
            .verifyComplete()

        // verifications
        verify(bdiClient, times(1)).getAvailableAccountingFiles()
        verify(zipRepository, times(1)).findByFilenameIn(any())
        verify(reactiveP7mZipService, times(1)).processZipFile(any())
    }

    @Test
    fun `Should receive error from bdiClient call and should retry 3 times`() {
        // pre-requisites
        val exception = Exception()
        given(bdiClient.getAvailableAccountingFiles()).willReturn(Mono.error(exception))

        // test
        StepVerifier.withVirtualTime { dataIngestionScheduledJob.accountingDataIngestion() }
            .expectSubscription()
            .thenAwait(Duration.ofSeconds(30))
            .expectErrorSatisfies { error ->
                assertTrue { error.javaClass == AccountingFilesNotRetrievedException::class.java }
                assertTrue { error.cause == exception }
            }
            .verify()
    }

    @Test
    fun `Should filter the file because is not regular`() {
        // pre-requisites
        val fileMetadataDtoSane = TestUtils.fileMetadataDto()
        val fileMetadataDtoNotSane = TestUtils.fileMetadataDto(isRegularFile = false)

        val listAccountingFiles = ListAccountingFiles200ResponseDto()
        listAccountingFiles.addFilesItem(fileMetadataDtoSane)
        listAccountingFiles.addFilesItem(fileMetadataDtoNotSane)

        val captor = argumentCaptor<AccountingZipDocument>()

        given(bdiClient.getAvailableAccountingFiles()).willReturn(Mono.just(listAccountingFiles))
        given(reactiveP7mZipService.processZipFile(any())).willReturn(Mono.just(Unit))
        given(zipRepository.findByFilenameIn(any())).willReturn(Flux.empty())
        given(zipRepository.save(any())).willAnswer { invocation ->
            val entityToSave = invocation.getArgument<AccountingZipDocument>(0)
            Mono.just(entityToSave)
        }

        // test
        StepVerifier.create(dataIngestionScheduledJob.accountingDataIngestion())
            .expectSubscription()
            .verifyComplete()

        // verifications
        verify(bdiClient, times(1)).getAvailableAccountingFiles()
        verify(zipRepository, times(1)).findByFilenameIn(any())
        verify(reactiveP7mZipService).processZipFile(captor.capture())

        val accountingZipDocuments = captor.allValues
        assertTrue { accountingZipDocuments.size == 1 }
        assertTrue { accountingZipDocuments[0].filename == fileMetadataDtoSane.fileName }
    }

    @Test
    fun `Should filter the file because is a directory`() {
        // pre-requisites
        val fileMetadataDtoSane = TestUtils.fileMetadataDto()
        val fileMetadataDtoNotSane = TestUtils.fileMetadataDto(isDirectory = true)

        val listAccountingFiles = ListAccountingFiles200ResponseDto()
        listAccountingFiles.addFilesItem(fileMetadataDtoSane)
        listAccountingFiles.addFilesItem(fileMetadataDtoNotSane)

        val captor = argumentCaptor<AccountingZipDocument>()

        given(bdiClient.getAvailableAccountingFiles()).willReturn(Mono.just(listAccountingFiles))
        given(reactiveP7mZipService.processZipFile(any())).willReturn(Mono.just(Unit))
        given(zipRepository.findByFilenameIn(any())).willReturn(Flux.empty())
        given(zipRepository.save(any())).willAnswer { invocation ->
            val entityToSave = invocation.getArgument<AccountingZipDocument>(0)
            Mono.just(entityToSave)
        }

        // test
        StepVerifier.create(dataIngestionScheduledJob.accountingDataIngestion())
            .expectSubscription()
            .verifyComplete()

        // verifications
        verify(bdiClient, times(1)).getAvailableAccountingFiles()
        verify(zipRepository, times(1)).findByFilenameIn(any())
        verify(reactiveP7mZipService).processZipFile(captor.capture())

        val accountingZipDocuments = captor.allValues
        assertTrue { accountingZipDocuments.size == 1 }
        assertTrue { accountingZipDocuments[0].filename == fileMetadataDtoSane.fileName }
    }

    @Test
    fun `Should filter the file because size is 0`() {
        // pre-requisites
        val fileMetadataDtoSane = TestUtils.fileMetadataDto()
        val fileMetadataDtoNotSane = TestUtils.fileMetadataDto(size = 0)

        val listAccountingFiles = ListAccountingFiles200ResponseDto()
        listAccountingFiles.addFilesItem(fileMetadataDtoSane)
        listAccountingFiles.addFilesItem(fileMetadataDtoNotSane)

        val captor = argumentCaptor<AccountingZipDocument>()

        given(bdiClient.getAvailableAccountingFiles()).willReturn(Mono.just(listAccountingFiles))
        given(reactiveP7mZipService.processZipFile(any())).willReturn(Mono.just(Unit))
        given(zipRepository.findByFilenameIn(any())).willReturn(Flux.empty())
        given(zipRepository.save(any())).willAnswer { invocation ->
            val entityToSave = invocation.getArgument<AccountingZipDocument>(0)
            Mono.just(entityToSave)
        }

        // test
        StepVerifier.create(dataIngestionScheduledJob.accountingDataIngestion())
            .expectSubscription()
            .verifyComplete()

        // verifications
        verify(bdiClient, times(1)).getAvailableAccountingFiles()
        verify(zipRepository, times(1)).findByFilenameIn(any())
        verify(reactiveP7mZipService).processZipFile(captor.capture())

        val accountingZipDocuments = captor.allValues
        assertTrue { accountingZipDocuments.size == 1 }
        assertTrue { accountingZipDocuments[0].filename == fileMetadataDtoSane.fileName }
    }

    @Test
    fun `Should filter the file because size is 0 and is not regular`() {
        // pre-requisites
        val fileMetadataDtoSane = TestUtils.fileMetadataDto()
        val fileMetadataDtoNotSane = TestUtils.fileMetadataDto(isRegularFile = false, size = 0)

        val listAccountingFiles = ListAccountingFiles200ResponseDto()
        listAccountingFiles.addFilesItem(fileMetadataDtoSane)
        listAccountingFiles.addFilesItem(fileMetadataDtoNotSane)

        val captor = argumentCaptor<AccountingZipDocument>()

        given(bdiClient.getAvailableAccountingFiles()).willReturn(Mono.just(listAccountingFiles))
        given(reactiveP7mZipService.processZipFile(any())).willReturn(Mono.just(Unit))
        given(zipRepository.findByFilenameIn(any())).willReturn(Flux.empty())
        given(zipRepository.save(any())).willAnswer { invocation ->
            val entityToSave = invocation.getArgument<AccountingZipDocument>(0)
            Mono.just(entityToSave)
        }

        // test
        StepVerifier.create(dataIngestionScheduledJob.accountingDataIngestion())
            .expectSubscription()
            .verifyComplete()

        // verifications
        verify(bdiClient, times(1)).getAvailableAccountingFiles()
        verify(zipRepository, times(1)).findByFilenameIn(any())
        verify(reactiveP7mZipService).processZipFile(captor.capture())

        val accountingZipDocuments = captor.allValues
        assertTrue { accountingZipDocuments.size == 1 }
        assertTrue { accountingZipDocuments[0].filename == fileMetadataDtoSane.fileName }
    }

    @Test
    fun `Should filter the file because size is 0 and is a directory`() {
        // pre-requisites
        val fileMetadataDtoSane = TestUtils.fileMetadataDto()
        val fileMetadataDtoNotSane = TestUtils.fileMetadataDto(isDirectory = true, size = 0)

        val listAccountingFiles = ListAccountingFiles200ResponseDto()
        listAccountingFiles.addFilesItem(fileMetadataDtoSane)
        listAccountingFiles.addFilesItem(fileMetadataDtoNotSane)

        val captor = argumentCaptor<AccountingZipDocument>()

        given(bdiClient.getAvailableAccountingFiles()).willReturn(Mono.just(listAccountingFiles))
        given(reactiveP7mZipService.processZipFile(any())).willReturn(Mono.just(Unit))
        given(zipRepository.findByFilenameIn(any())).willReturn(Flux.empty())
        given(zipRepository.save(any())).willAnswer { invocation ->
            val entityToSave = invocation.getArgument<AccountingZipDocument>(0)
            Mono.just(entityToSave)
        }

        // test
        StepVerifier.create(dataIngestionScheduledJob.accountingDataIngestion())
            .expectSubscription()
            .verifyComplete()

        // verifications
        verify(bdiClient, times(1)).getAvailableAccountingFiles()
        verify(zipRepository, times(1)).findByFilenameIn(any())
        verify(reactiveP7mZipService).processZipFile(captor.capture())

        val accountingZipDocuments = captor.allValues
        assertTrue { accountingZipDocuments.size == 1 }
        assertTrue { accountingZipDocuments[0].filename == fileMetadataDtoSane.fileName }
    }

    @Test
    fun `Should filter the file because is a directory and is not regular`() {
        // pre-requisites
        val fileMetadataDtoSane = TestUtils.fileMetadataDto()
        val fileMetadataDtoNotSane =
            TestUtils.fileMetadataDto(isRegularFile = false, isDirectory = true)

        val listAccountingFiles = ListAccountingFiles200ResponseDto()
        listAccountingFiles.addFilesItem(fileMetadataDtoSane)
        listAccountingFiles.addFilesItem(fileMetadataDtoNotSane)

        val captor = argumentCaptor<AccountingZipDocument>()

        // pre-requisites
        given(bdiClient.getAvailableAccountingFiles()).willReturn(Mono.just(listAccountingFiles))
        given(reactiveP7mZipService.processZipFile(any())).willReturn(Mono.just(Unit))
        given(zipRepository.findByFilenameIn(any())).willReturn(Flux.empty())
        given(zipRepository.save(any())).willAnswer { invocation ->
            val entityToSave = invocation.getArgument<AccountingZipDocument>(0)
            Mono.just(entityToSave)
        }

        // test
        StepVerifier.create(dataIngestionScheduledJob.accountingDataIngestion())
            .expectSubscription()
            .verifyComplete()

        // verifications
        verify(bdiClient, times(1)).getAvailableAccountingFiles()
        verify(zipRepository, times(1)).findByFilenameIn(any())
        verify(reactiveP7mZipService).processZipFile(captor.capture())

        val accountingZipDocuments = captor.allValues
        assertTrue { accountingZipDocuments.size == 1 }
        assertTrue { accountingZipDocuments[0].filename == fileMetadataDtoSane.fileName }
    }

    @Test
    fun `Should filter the file because is already present in the DB`() {
        // pre-requisites
        val fileMetadataDto = TestUtils.fileMetadataDto()
        val listAccountingFiles = ListAccountingFiles200ResponseDto()
        val accountingZipDocumentFilenameDto =
            AccountingZipDocumentFilenameDto(fileMetadataDto.fileName)
        listAccountingFiles.addFilesItem(fileMetadataDto)

        // pre-requisites
        given(bdiClient.getAvailableAccountingFiles()).willReturn(Mono.just(listAccountingFiles))
        given(zipRepository.findByFilenameIn(any()))
            .willReturn(Flux.just(accountingZipDocumentFilenameDto))

        // test
        StepVerifier.create(dataIngestionScheduledJob.accountingDataIngestion())
            .expectSubscription()
            .verifyComplete()

        // verifications
        verify(bdiClient, times(1)).getAvailableAccountingFiles()
        verify(zipRepository, times(1)).findByFilenameIn(any())
        verify(reactiveP7mZipService, times(0)).processZipFile(any())
    }

    @Test
    fun `Should return empty Flux if the download candidate list is empty`() {
        // pre-requisites
        val fileMetadataDto = TestUtils.fileMetadataDto(fileName = "NO_MATCH")
        val listAccountingFiles = ListAccountingFiles200ResponseDto()
        listAccountingFiles.addFilesItem(fileMetadataDto)

        // pre-requisites
        given(bdiClient.getAvailableAccountingFiles()).willReturn(Mono.just(listAccountingFiles))

        // test
        StepVerifier.create(dataIngestionScheduledJob.accountingDataIngestion())
            .expectSubscription()
            .verifyComplete()

        // verifications
        verify(bdiClient, times(1)).getAvailableAccountingFiles()
        verify(zipRepository, times(0)).findByFilenameIn(any())
        verify(reactiveP7mZipService, times(0)).processZipFile(any())
    }

    @Test
    fun `Should keep processing ZIPs if one throws an exception`() {
        // pre-requisites
        val listAccountingFiles = ListAccountingFiles200ResponseDto()
        val fileMetadataDto = TestUtils.fileMetadataDto()
        val accountingZipDocument =
            TestUtils.accountingZipDocument()
                .copy(
                    id = null,
                    filename = fileMetadataDto.fileName,
                    createdAt = null,
                    updatedAt = null,
                )

        val fileMetadataDtoError =
            TestUtils.fileMetadataDto(fileName = fileMetadataDto.fileName + "ERROR")
        val accountingZipDocumentError =
            TestUtils.accountingZipDocument()
                .copy(
                    id = null,
                    filename = fileMetadataDtoError.fileName,
                    createdAt = null,
                    updatedAt = null,
                )

        listAccountingFiles.addFilesItem(fileMetadataDtoError)
        listAccountingFiles.addFilesItem(fileMetadataDto)

        given(bdiClient.getAvailableAccountingFiles()).willReturn(Mono.just(listAccountingFiles))
        given(reactiveP7mZipService.processZipFile(accountingZipDocumentError))
            .willReturn(Mono.error(RuntimeException("Fail")))
        given(reactiveP7mZipService.processZipFile(accountingZipDocument))
            .willReturn(Mono.just(Unit))
        given(zipRepository.findByFilenameIn(any())).willReturn(Flux.empty())
        given(zipRepository.save(accountingZipDocument))
            .willReturn(Mono.just(accountingZipDocument))
        given(zipRepository.save(accountingZipDocumentError))
            .willReturn(Mono.just(accountingZipDocumentError))

        // test
        StepVerifier.create(dataIngestionScheduledJob.accountingDataIngestion())
            .expectSubscription()
            .verifyComplete()

        // verifications
        verify(bdiClient, times(1)).getAvailableAccountingFiles()
        verify(zipRepository, times(1)).findByFilenameIn(any())
        verify(reactiveP7mZipService, times(2)).processZipFile(any())
    }
}

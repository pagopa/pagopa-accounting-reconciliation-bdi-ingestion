package it.pagopa.accounting.reconciliation.bdi.ingestion.scheduledjob

import it.pagopa.accounting.reconciliation.bdi.ingestion.clients.BdiClient
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingZipDocument
import it.pagopa.accounting.reconciliation.bdi.ingestion.exceptions.AccountingFilesNotRetrievedException
import it.pagopa.accounting.reconciliation.bdi.ingestion.service.ReactiveP7mZipService
import it.pagopa.generated.bdi.model.FileMetadataDto
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
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class DataIngestionScheduledJobTest {

    private val bdiClient: BdiClient = mock()
    private val reactiveP7mZipService: ReactiveP7mZipService = mock()
    private val retries: Long = 2
    private val minBackoffSeconds: Long = 5
    private val dataIngestionScheduledJob =
        DataIngestionScheduledJob(bdiClient, reactiveP7mZipService, retries, minBackoffSeconds)

    @Test
    fun `Should get files from bdiClient and pass to the reactiveP7mZipService`() {

        val listAccountingFiles = ListAccountingFiles200ResponseDto()
        val fileMetaDataDTO = FileMetadataDto()
        fileMetaDataDTO.fileName = "test"
        fileMetaDataDTO.isRegularFile = true
        fileMetaDataDTO.isDirectory = false
        fileMetaDataDTO.size = 20000
        listAccountingFiles.addFilesItem(fileMetaDataDTO)
        // pre-requisites
        given(bdiClient.getAvailableAccountingFiles()).willReturn(Mono.just(listAccountingFiles))
        given(reactiveP7mZipService.processZipFile(any())).willReturn(Mono.just(Unit))

        // test
        StepVerifier.create(dataIngestionScheduledJob.accountingDataIngestion())
            .expectSubscription()
            .verifyComplete()

        // verifications
        verify(bdiClient, times(1)).getAvailableAccountingFiles()
        verify(reactiveP7mZipService, times(1)).processZipFile(any())
    }

    @Test
    fun `Should receive error from bdiClient call and should retry 3 times`() {

        val exception = Exception()

        // pre-requisites
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

        val fileMetaDataDTOSane = FileMetadataDto()
        fileMetaDataDTOSane.fileName = "test_sane"
        fileMetaDataDTOSane.isRegularFile = true
        fileMetaDataDTOSane.isDirectory = false
        fileMetaDataDTOSane.size = 20000

        val fileMetaDataDTONotSane = FileMetadataDto()
        fileMetaDataDTONotSane.fileName = "test_not_sane"
        fileMetaDataDTONotSane.isRegularFile = false
        fileMetaDataDTONotSane.isDirectory = false
        fileMetaDataDTONotSane.size = 20000

        val listAccountingFiles = ListAccountingFiles200ResponseDto()
        listAccountingFiles.addFilesItem(fileMetaDataDTOSane)
        listAccountingFiles.addFilesItem(fileMetaDataDTONotSane)

        val captor = argumentCaptor<AccountingZipDocument>()

        // pre-requisites
        given(bdiClient.getAvailableAccountingFiles()).willReturn(Mono.just(listAccountingFiles))
        given(reactiveP7mZipService.processZipFile(any())).willReturn(Mono.just(Unit))

        // test
        StepVerifier.create(dataIngestionScheduledJob.accountingDataIngestion())
            .expectSubscription()
            .verifyComplete()

        // verifications
        verify(bdiClient, times(1)).getAvailableAccountingFiles()
        verify(reactiveP7mZipService).processZipFile(captor.capture())

        val accountingZipDocuments = captor.allValues
        assertTrue { accountingZipDocuments.size == 1 }
        assertTrue { accountingZipDocuments[0].filename == "test_sane" }
    }

    @Test
    fun `Should filter the file because is a directory`() {

        val fileMetaDataDTOSane = FileMetadataDto()
        fileMetaDataDTOSane.fileName = "test_sane"
        fileMetaDataDTOSane.isRegularFile = true
        fileMetaDataDTOSane.isDirectory = false
        fileMetaDataDTOSane.size = 20000

        val fileMetaDataDTONotSane = FileMetadataDto()
        fileMetaDataDTONotSane.fileName = "test_not_sane"
        fileMetaDataDTONotSane.isRegularFile = true
        fileMetaDataDTONotSane.isDirectory = true
        fileMetaDataDTONotSane.size = 20000

        val listAccountingFiles = ListAccountingFiles200ResponseDto()
        listAccountingFiles.addFilesItem(fileMetaDataDTOSane)
        listAccountingFiles.addFilesItem(fileMetaDataDTONotSane)

        val captor = argumentCaptor<AccountingZipDocument>()

        // pre-requisites
        given(bdiClient.getAvailableAccountingFiles()).willReturn(Mono.just(listAccountingFiles))
        given(reactiveP7mZipService.processZipFile(any())).willReturn(Mono.just(Unit))

        // test
        StepVerifier.create(dataIngestionScheduledJob.accountingDataIngestion())
            .expectSubscription()
            .verifyComplete()

        // verifications
        verify(bdiClient, times(1)).getAvailableAccountingFiles()
        verify(reactiveP7mZipService).processZipFile(captor.capture())

        val accountingZipDocuments = captor.allValues
        assertTrue { accountingZipDocuments.size == 1 }
        assertTrue { accountingZipDocuments[0].filename == "test_sane" }
    }

    @Test
    fun `Should filter the file because size is 0`() {

        val fileMetaDataDTOSane = FileMetadataDto()
        fileMetaDataDTOSane.fileName = "test_sane"
        fileMetaDataDTOSane.isRegularFile = true
        fileMetaDataDTOSane.isDirectory = false
        fileMetaDataDTOSane.size = 20000

        val fileMetaDataDTONotSane = FileMetadataDto()
        fileMetaDataDTONotSane.fileName = "test_not_sane"
        fileMetaDataDTONotSane.isRegularFile = true
        fileMetaDataDTONotSane.isDirectory = false
        fileMetaDataDTONotSane.size = 0

        val listAccountingFiles = ListAccountingFiles200ResponseDto()
        listAccountingFiles.addFilesItem(fileMetaDataDTOSane)
        listAccountingFiles.addFilesItem(fileMetaDataDTONotSane)

        val captor = argumentCaptor<AccountingZipDocument>()

        // pre-requisites
        given(bdiClient.getAvailableAccountingFiles()).willReturn(Mono.just(listAccountingFiles))
        given(reactiveP7mZipService.processZipFile(any())).willReturn(Mono.just(Unit))

        // test
        StepVerifier.create(dataIngestionScheduledJob.accountingDataIngestion())
            .expectSubscription()
            .verifyComplete()

        // verifications
        verify(bdiClient, times(1)).getAvailableAccountingFiles()
        verify(reactiveP7mZipService).processZipFile(captor.capture())

        val accountingZipDocuments = captor.allValues
        assertTrue { accountingZipDocuments.size == 1 }
        assertTrue { accountingZipDocuments[0].filename == "test_sane" }
    }

    @Test
    fun `Should filter the file because size is 0 and is not regular`() {

        val fileMetaDataDTOSane = FileMetadataDto()
        fileMetaDataDTOSane.fileName = "test_sane"
        fileMetaDataDTOSane.isRegularFile = true
        fileMetaDataDTOSane.isDirectory = false
        fileMetaDataDTOSane.size = 20000

        val fileMetaDataDTONotSane = FileMetadataDto()
        fileMetaDataDTONotSane.fileName = "test_not_sane"
        fileMetaDataDTONotSane.isRegularFile = false
        fileMetaDataDTONotSane.isDirectory = false
        fileMetaDataDTONotSane.size = 0

        val listAccountingFiles = ListAccountingFiles200ResponseDto()
        listAccountingFiles.addFilesItem(fileMetaDataDTOSane)
        listAccountingFiles.addFilesItem(fileMetaDataDTONotSane)

        val captor = argumentCaptor<AccountingZipDocument>()

        // pre-requisites
        given(bdiClient.getAvailableAccountingFiles()).willReturn(Mono.just(listAccountingFiles))
        given(reactiveP7mZipService.processZipFile(any())).willReturn(Mono.just(Unit))

        // test
        StepVerifier.create(dataIngestionScheduledJob.accountingDataIngestion())
            .expectSubscription()
            .verifyComplete()

        // verifications
        verify(bdiClient, times(1)).getAvailableAccountingFiles()
        verify(reactiveP7mZipService).processZipFile(captor.capture())

        val accountingZipDocuments = captor.allValues
        assertTrue { accountingZipDocuments.size == 1 }
        assertTrue { accountingZipDocuments[0].filename == "test_sane" }
    }

    @Test
    fun `Should filter the file because size is 0 and is a directory`() {

        val fileMetaDataDTOSane = FileMetadataDto()
        fileMetaDataDTOSane.fileName = "test_sane"
        fileMetaDataDTOSane.isRegularFile = true
        fileMetaDataDTOSane.isDirectory = false
        fileMetaDataDTOSane.size = 20000

        val fileMetaDataDTONotSane = FileMetadataDto()
        fileMetaDataDTONotSane.fileName = "test_not_sane"
        fileMetaDataDTONotSane.isRegularFile = true
        fileMetaDataDTONotSane.isDirectory = true
        fileMetaDataDTONotSane.size = 0

        val listAccountingFiles = ListAccountingFiles200ResponseDto()
        listAccountingFiles.addFilesItem(fileMetaDataDTOSane)
        listAccountingFiles.addFilesItem(fileMetaDataDTONotSane)

        val captor = argumentCaptor<AccountingZipDocument>()

        // pre-requisites
        given(bdiClient.getAvailableAccountingFiles()).willReturn(Mono.just(listAccountingFiles))
        given(reactiveP7mZipService.processZipFile(any())).willReturn(Mono.just(Unit))

        // test
        StepVerifier.create(dataIngestionScheduledJob.accountingDataIngestion())
            .expectSubscription()
            .verifyComplete()

        // verifications
        verify(bdiClient, times(1)).getAvailableAccountingFiles()
        verify(reactiveP7mZipService).processZipFile(captor.capture())

        val accountingZipDocuments = captor.allValues
        assertTrue { accountingZipDocuments.size == 1 }
        assertTrue { accountingZipDocuments[0].filename == "test_sane" }
    }

    @Test
    fun `Should filter the file because is a directory and is not regular`() {

        val fileMetaDataDTOSane = FileMetadataDto()
        fileMetaDataDTOSane.fileName = "test_sane"
        fileMetaDataDTOSane.isRegularFile = true
        fileMetaDataDTOSane.isDirectory = false
        fileMetaDataDTOSane.size = 20000

        val fileMetaDataDTONotSane = FileMetadataDto()
        fileMetaDataDTONotSane.fileName = "test_not_sane"
        fileMetaDataDTONotSane.isRegularFile = false
        fileMetaDataDTONotSane.isDirectory = true
        fileMetaDataDTONotSane.size = 2000

        val listAccountingFiles = ListAccountingFiles200ResponseDto()
        listAccountingFiles.addFilesItem(fileMetaDataDTOSane)
        listAccountingFiles.addFilesItem(fileMetaDataDTONotSane)

        val captor = argumentCaptor<AccountingZipDocument>()

        // pre-requisites
        given(bdiClient.getAvailableAccountingFiles()).willReturn(Mono.just(listAccountingFiles))
        given(reactiveP7mZipService.processZipFile(any())).willReturn(Mono.just(Unit))

        // test
        StepVerifier.create(dataIngestionScheduledJob.accountingDataIngestion())
            .expectSubscription()
            .verifyComplete()

        // verifications
        verify(bdiClient, times(1)).getAvailableAccountingFiles()
        verify(reactiveP7mZipService).processZipFile(captor.capture())

        val accountingZipDocuments = captor.allValues
        assertTrue { accountingZipDocuments.size == 1 }
        assertTrue { accountingZipDocuments[0].filename == "test_sane" }
    }
}

package it.pagopa.accounting.reconciliation.bdi.ingestion

import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingXmlDocument
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingXmlStatus
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingZipDocument
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingZipStatus
import it.pagopa.generated.bdi.model.FileMetadataDto
import java.time.Instant

object TestUtils {
    val NOW: Instant = Instant.now()

    fun accountingZipDocument(
        id: String = "12345",
        fileName: String = "test.zip",
        createdAt: Instant = NOW,
        updatedAt: Instant = NOW,
        status: AccountingZipStatus = AccountingZipStatus.TO_DOWNLOAD,
    ) =
        AccountingZipDocument(
            id = id,
            filename = fileName,
            createdAt = createdAt,
            updatedAt = updatedAt,
            status = status,
        )

    fun accountingXmlDocument(
        id: String = "12345",
        zipFilename: String = "test.zip",
        filename: String = "test.xml",
        createdAt: Instant = NOW,
        updatedAt: Instant = NOW,
        xmlContent: String = "<root></root>",
        status: AccountingXmlStatus = AccountingXmlStatus.TO_PARSE,
    ) =
        AccountingXmlDocument(
            id = id,
            zipFilename = zipFilename,
            filename = filename,
            createdAt = createdAt,
            updatedAt = updatedAt,
            xmlContent = xmlContent,
            status = status,
        )

    fun fileMetadataDto(
        fileName: String = "TEST-OPI-REND-ANALITICO-BE-",
        isRegularFile: Boolean = true,
        isDirectory: Boolean = false,
        size: Long = 1000,
    ): FileMetadataDto {
        val fileMetaDataDto = FileMetadataDto()
        fileMetaDataDto.fileName = fileName
        fileMetaDataDto.isRegularFile = isRegularFile
        fileMetaDataDto.isDirectory = isDirectory
        fileMetaDataDto.size = size
        return fileMetaDataDto
    }
}

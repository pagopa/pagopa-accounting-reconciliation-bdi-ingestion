package it.pagopa.accounting.reconciliation.bdi.ingestion

import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingXmlDocument
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingXmlStatus
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingZipDocument
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingZipStatus
import java.time.Instant

object TestUtils {
    val NOW: Instant = Instant.now()

    fun accountingZipDocument(): AccountingZipDocument {
        return accountingZipDocumentWithFilename("test.zip")
    }

    fun accountingZipDocumentWithFilename(filename: String): AccountingZipDocument {
        return AccountingZipDocument(
            id = "12345",
            filename = filename,
            createdAt = NOW,
            updatedAt = NOW,
            status = AccountingZipStatus.TO_DOWNLOAD,
        )
    }

    fun accountingXmlDocument(): AccountingXmlDocument {
        return accountingXmlDocumentWithContent("<root></root>")
    }

    fun accountingXmlDocumentWithContent(xmlContent: String): AccountingXmlDocument {
        return AccountingXmlDocument(
            id = "12345",
            zipFilename = "test.zip",
            filename = "test.xml",
            createdAt = NOW,
            updatedAt = NOW,
            xmlContent = xmlContent,
            status = AccountingXmlStatus.TO_PARSE,
        )
    }
}

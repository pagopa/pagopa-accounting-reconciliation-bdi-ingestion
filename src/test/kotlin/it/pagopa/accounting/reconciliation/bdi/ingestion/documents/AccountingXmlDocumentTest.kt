package it.pagopa.accounting.reconciliation.bdi.ingestion.documents

import java.time.Instant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AccountingXmlDocumentTest {

    @Test
    fun `should create instance with all fields populated`() {
        val now = Instant.now()
        val doc =
            AccountingXmlDocument(
                id = "123",
                zipFilename = "data.zip",
                filename = "file.xml",
                uploadedAt = now,
                xmlContent = "<root></root>",
                status = "PENDING",
            )

        assertThat(doc.id).isEqualTo("123")
        assertThat(doc.zipFilename).isEqualTo("data.zip")
        assertThat(doc.filename).isEqualTo("file.xml")
        assertThat(doc.uploadedAt).isEqualTo(now)
        assertThat(doc.xmlContent).isEqualTo("<root></root>")
        assertThat(doc.status).isEqualTo("PENDING")
    }

    @Test
    fun `should use default values when creating instance`() {
        val doc =
            AccountingXmlDocument(
                zipFilename = "data.zip",
                filename = "file.xml",
                xmlContent = "<root></root>",
                status = "PENDING",
            )

        assertThat(doc.id).isNull()
        assertThat(doc.uploadedAt).isNotNull()
        assertThat(doc.uploadedAt).isBeforeOrEqualTo(Instant.now())
    }

    @Test
    fun `should verify equality and hashcode`() {

        val now = Instant.now()
        val doc1 =
            AccountingXmlDocument(
                id = "1",
                zipFilename = "A",
                filename = "B",
                uploadedAt = now,
                xmlContent = "C",
                status = "OK",
            )
        val doc2 =
            AccountingXmlDocument(
                id = "1",
                zipFilename = "A",
                filename = "B",
                uploadedAt = now,
                xmlContent = "C",
                status = "OK",
            )

        assertThat(doc1).isEqualTo(doc2)
        assertThat(doc1.hashCode()).isEqualTo(doc2.hashCode())
    }

    @Test
    fun `should support copy mechanism`() {
        val doc1 =
            AccountingXmlDocument(
                zipFilename = "original.zip",
                filename = "original.xml",
                xmlContent = "content",
                status = "NEW",
            )

        val doc2 = doc1.copy(status = "PROCESSED")

        assertThat(doc2.status).isEqualTo("PROCESSED")
        assertThat(doc2.zipFilename).isEqualTo("original.zip") // Gli altri campi restano uguali
        assertThat(doc2).isNotEqualTo(doc1)
    }
}

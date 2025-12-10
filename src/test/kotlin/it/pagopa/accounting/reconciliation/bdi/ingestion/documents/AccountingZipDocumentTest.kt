package it.pagopa.accounting.reconciliation.bdi.ingestion.documents

import java.time.Instant
import java.time.temporal.ChronoUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AccountingZipDocumentTest {

    @Test
    fun `should create instance with all fields populated`() {
        val now = Instant.now()
        val id = "12345"
        val filename = "accounting_2024.zip"
        val status = "DOWNLOADED"

        val document =
            AccountingZipDocument(id = id, filename = filename, uploadedAt = now, status = status)

        assertThat(document.id).isEqualTo(id)
        assertThat(document.filename).isEqualTo(filename)
        assertThat(document.uploadedAt).isEqualTo(now)
        assertThat(document.status).isEqualTo(status)
    }

    @Test
    fun `should check default values`() {

        val document = AccountingZipDocument(filename = "test.zip", status = "PENDING")

        assertThat(document.id).isNull() // Il default è null
        assertThat(document.filename).isEqualTo("test.zip")
        assertThat(document.status).isEqualTo("PENDING")

        assertThat(document.uploadedAt).isNotNull()
        assertThat(document.uploadedAt)
            .isCloseTo(
                Instant.now(),
                org.assertj.core.data.TemporalUnitWithinOffset(1, ChronoUnit.SECONDS),
            )
    }

    @Test
    fun `should verify equals and hashCode contract`() {
        val now = Instant.now()
        val doc1 =
            AccountingZipDocument(id = "1", filename = "file.zip", uploadedAt = now, status = "OK")
        val doc2 =
            AccountingZipDocument(id = "1", filename = "file.zip", uploadedAt = now, status = "OK")
        val doc3 =
            AccountingZipDocument(
                id = "2", // ID diverso
                filename = "file.zip",
                uploadedAt = now,
                status = "OK",
            )

        assertThat(doc1).isEqualTo(doc2) // Sono uguali per valore
        assertThat(doc1.hashCode()).isEqualTo(doc2.hashCode())
        assertThat(doc1).isNotEqualTo(doc3) // Sono diversi
    }

    @Test
    fun `should verify copy mechanism`() {

        val original = AccountingZipDocument(id = "1", filename = "original.zip", status = "NEW")

        val modified = original.copy(status = "PROCESSED")

        assertThat(modified.id).isEqualTo(original.id)
        assertThat(modified.filename).isEqualTo(original.filename)
        assertThat(modified.uploadedAt).isEqualTo(original.uploadedAt)
        assertThat(modified.status).isEqualTo("PROCESSED")

        assertThat(original.status).isEqualTo("NEW")
    }

    @Test
    fun `toString should contain field values`() {

        val document =
            AccountingZipDocument(id = "test-id", filename = "my-file.zip", status = "ERROR")

        val stringRepresentation = document.toString()

        assertThat(stringRepresentation)
            .contains("test-id")
            .contains("my-file.zip")
            .contains("ERROR")
            .contains("AccountingZipDocument")
    }
}

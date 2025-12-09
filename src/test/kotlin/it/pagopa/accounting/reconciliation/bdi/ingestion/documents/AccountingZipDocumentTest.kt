package it.pagopa.accounting.reconciliation.bdi.ingestion.documents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class AccountingZipDocumentTest {

    @Test
    fun `should create instance with all fields populated`() {
        // GIVEN
        val now = Instant.now()
        val id = "12345"
        val filename = "accounting_2024.zip"
        val status = "DOWNLOADED"

        // WHEN
        val document = AccountingZipDocument(
            id = id,
            filename = filename,
            uploadedAt = now,
            status = status
        )

        // THEN
        assertThat(document.id).isEqualTo(id)
        assertThat(document.filename).isEqualTo(filename)
        assertThat(document.uploadedAt).isEqualTo(now)
        assertThat(document.status).isEqualTo(status)
    }

    @Test
    fun `should check default values`() {
        // WHEN
        // Istanziamo omettendo 'id' e 'uploadedAt' che hanno valori di default
        val document = AccountingZipDocument(
            filename = "test.zip",
            status = "PENDING"
        )

        // THEN
        assertThat(document.id).isNull() // Il default è null
        assertThat(document.filename).isEqualTo("test.zip")
        assertThat(document.status).isEqualTo("PENDING")

        // Verifichiamo che uploadedAt sia stato valorizzato con un Instant recente
        assertThat(document.uploadedAt).isNotNull()
        assertThat(document.uploadedAt).isCloseTo(Instant.now(), org.assertj.core.data.TemporalUnitWithinOffset(1, ChronoUnit.SECONDS))
    }

    @Test
    fun `should verify equals and hashCode contract`() {
        // GIVEN
        val now = Instant.now()
        val doc1 = AccountingZipDocument(
            id = "1",
            filename = "file.zip",
            uploadedAt = now,
            status = "OK"
        )
        val doc2 = AccountingZipDocument(
            id = "1",
            filename = "file.zip",
            uploadedAt = now,
            status = "OK"
        )
        val doc3 = AccountingZipDocument(
            id = "2", // ID diverso
            filename = "file.zip",
            uploadedAt = now,
            status = "OK"
        )

        // THEN
        assertThat(doc1).isEqualTo(doc2)      // Sono uguali per valore
        assertThat(doc1.hashCode()).isEqualTo(doc2.hashCode())
        assertThat(doc1).isNotEqualTo(doc3)   // Sono diversi
    }

    @Test
    fun `should verify copy mechanism`() {
        // GIVEN
        val original = AccountingZipDocument(
            id = "1",
            filename = "original.zip",
            status = "NEW"
        )

        // WHEN
        // Copiamo cambiando solo lo status
        val modified = original.copy(status = "PROCESSED")

        // THEN
        assertThat(modified.id).isEqualTo(original.id)         // Invariato
        assertThat(modified.filename).isEqualTo(original.filename) // Invariato
        assertThat(modified.uploadedAt).isEqualTo(original.uploadedAt) // Invariato
        assertThat(modified.status).isEqualTo("PROCESSED")     // Modificato

        // Assicuriamoci che l'originale non sia stato toccato (immutabilità)
        assertThat(original.status).isEqualTo("NEW")
    }

    @Test
    fun `toString should contain field values`() {
        // Questo è utile per il debugging nei log
        val document = AccountingZipDocument(
            id = "test-id",
            filename = "my-file.zip",
            status = "ERROR"
        )

        val stringRepresentation = document.toString()

        assertThat(stringRepresentation)
            .contains("test-id")
            .contains("my-file.zip")
            .contains("ERROR")
            .contains("AccountingZipDocument")
    }
}
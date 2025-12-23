package it.pagopa.accounting.reconciliation.bdi.ingestion.documents

import it.pagopa.accounting.reconciliation.bdi.ingestion.TestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class AccountingZipDocumentTest {

    @Test
    fun `should create instance with all fields populated`() {
        val document = TestUtils.accountingZipDocument()

        assertNotNull(document.id)
        assertNotNull(document.filename)
        assertNotNull(document.createdAt)
        assertNotNull(document.updatedAt)
        assertNotNull(document.status)
    }

    @Test
    fun `should verify copy mechanism`() {
        val original = TestUtils.accountingZipDocument()

        val modified = original.copy(status = AccountingZipStatus.DOWNLOADED)

        assertThat(modified.id).isEqualTo(original.id)
        assertThat(modified.filename).isEqualTo(original.filename)
        assertThat(modified.createdAt).isEqualTo(original.createdAt)
        assertThat(modified.updatedAt).isEqualTo(original.updatedAt)
        assertThat(modified.status.name).isEqualTo(AccountingZipStatus.DOWNLOADED.name)
        assertThat(original.status.name).isEqualTo(AccountingZipStatus.TO_DOWNLOAD.name)
    }

    @Test
    fun `toString should contain field values`() {
        val document = TestUtils.accountingZipDocument()

        val stringRepresentation = document.toString()

        assertThat(stringRepresentation)
            .contains(document.id)
            .contains(document.filename)
            .contains(document.createdAt.toString())
            .contains(document.updatedAt.toString())
            .contains(document.status.name)
    }
}

package it.pagopa.accounting.reconciliation.bdi.ingestion.documents

import it.pagopa.accounting.reconciliation.bdi.ingestion.TestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class AccountingXmlDocumentTest {

    @Test
    fun `should create instance with all fields populated`() {
        val document = TestUtils.accountingXmlDocument()

        assertNotNull(document.id)
        assertNotNull(document.zipFilename)
        assertNotNull(document.filename)
        assertNotNull(document.createdAt)
        assertNotNull(document.updatedAt)
        assertNotNull(document.xmlContent)
        assertNotNull(document.status)
    }

    @Test
    fun `should verify copy mechanism`() {
        val original = TestUtils.accountingXmlDocument()

        val modified = original.copy(status = AccountingXmlStatus.PARSED)

        assertThat(modified.id).isEqualTo(original.id)
        assertThat(modified.zipFilename).isEqualTo(original.zipFilename)
        assertThat(modified.filename).isEqualTo(original.filename)
        assertThat(modified.createdAt).isEqualTo(original.createdAt)
        assertThat(modified.updatedAt).isEqualTo(original.updatedAt)
        assertThat(modified.status.name).isEqualTo(AccountingXmlStatus.PARSED.name)
        assertThat(original.status.name).isEqualTo(AccountingXmlStatus.TO_PARSE.name)
    }

    @Test
    fun `toString should contain field values`() {
        val document = TestUtils.accountingXmlDocument()

        val stringRepresentation = document.toString()

        assertThat(stringRepresentation)
            .contains(document.id)
            .contains(document.zipFilename)
            .contains(document.filename)
            .contains(document.createdAt.toString())
            .contains(document.updatedAt.toString())
            .contains(document.xmlContent)
            .contains(document.status.name)
    }
}

package it.pagopa.accounting.reconciliation.bdi.ingestion.documents

import java.math.BigDecimal
import java.time.Instant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BdiAccountingDataTest {

    @Test
    fun `should create instance with all values populated`() {

        val e2eId = "E2E123456789"
        val causale = "Bonifico Stipendio"
        val importo = BigDecimal("1250.50")
        val banca = "Intesa Sanpaolo"

        val data =
            BdiAccountingData(
                end2endId = e2eId,
                causale = causale,
                importo = importo,
                bancaOrdinante = banca,
                insertedTimestamp = Instant.now(),
            )

        assertThat(data.end2endId).isEqualTo(e2eId)
        assertThat(data.causale).isEqualTo(causale)
        assertThat(data.importo).isEqualTo(importo)
        assertThat(data.bancaOrdinante).isEqualTo(banca)
    }

    @Test
    fun `should handle null values correctly`() {

        val data =
            BdiAccountingData(
                end2endId = null,
                causale = null,
                importo = null,
                bancaOrdinante = null,
                insertedTimestamp = Instant.now(),
            )

        assertThat(data.end2endId).isNull()
        assertThat(data.causale).isNull()
        assertThat(data.importo).isNull()
        assertThat(data.bancaOrdinante).isNull()
    }

    @Test
    fun `should verify equality and hashcode`() {

        val importo = BigDecimal("100.00")
        val data1 =
            BdiAccountingData(
                "ID1",
                "Causale A",
                importo,
                "Banca A",
                insertedTimestamp = Instant.now(),
            )
        val data2 =
            BdiAccountingData(
                "ID1",
                "Causale A",
                importo,
                "Banca A",
                insertedTimestamp = Instant.now(),
            )
        val data3 =
            BdiAccountingData(
                "ID2",
                "Causale B",
                BigDecimal("50.00"),
                "Banca B",
                insertedTimestamp = Instant.now(),
            )

        assertThat(data1.end2endId).isEqualTo(data2.end2endId)
        assertThat(data1).isNotEqualTo(data3)
    }

    @Test
    fun `should verify BigDecimal equality edge case`() {

        val dataScale0 =
            BdiAccountingData("ID", "C", BigDecimal("10"), "B", insertedTimestamp = Instant.now())
        val dataScale2 =
            BdiAccountingData(
                "ID",
                "C",
                BigDecimal("10.00"),
                "B",
                insertedTimestamp = Instant.now(),
            )

        assertThat(dataScale0).isNotEqualTo(dataScale2)
    }

    @Test
    fun `should support copy with modification`() {

        val original =
            BdiAccountingData(
                end2endId = "OLD_ID",
                causale = "Old Causale",
                importo = BigDecimal("10.00"),
                bancaOrdinante = "Old Bank",
                insertedTimestamp = Instant.now(),
            )

        val modified = original.copy(importo = BigDecimal("99.99"))

        assertThat(modified.importo).isEqualTo(BigDecimal("99.99"))

        assertThat(modified.end2endId).isEqualTo("OLD_ID")
        assertThat(modified.causale).isEqualTo("Old Causale")
        assertThat(modified.bancaOrdinante).isEqualTo("Old Bank")
    }

    @Test
    fun `toString should be readable`() {
        val data =
            BdiAccountingData(
                "ID_123",
                "Test",
                BigDecimal("1.0"),
                "MyBank",
                insertedTimestamp = Instant.now(),
            )

        assertThat(data.toString())
            .contains("BdiAccountingData")
            .contains("ID_123")
            .contains("Test")
            .contains("MyBank")
    }
}

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
                END2END_ID = e2eId,
                CAUSALE = causale,
                IMPORTO = importo,
                BANCA_ORDINANTE = banca,
                INSERT_TIMESTAMP = Instant.now(),
            )

        assertThat(data.END2END_ID).isEqualTo(e2eId)
        assertThat(data.CAUSALE).isEqualTo(causale)
        assertThat(data.IMPORTO).isEqualTo(importo)
        assertThat(data.BANCA_ORDINANTE).isEqualTo(banca)
    }

    @Test
    fun `should handle null values correctly`() {

        val data =
            BdiAccountingData(
                END2END_ID = null,
                CAUSALE = null,
                IMPORTO = null,
                BANCA_ORDINANTE = null,
                INSERT_TIMESTAMP = Instant.now(),
            )

        assertThat(data.END2END_ID).isNull()
        assertThat(data.CAUSALE).isNull()
        assertThat(data.IMPORTO).isNull()
        assertThat(data.BANCA_ORDINANTE).isNull()
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
                INSERT_TIMESTAMP = Instant.now(),
            )
        val data2 =
            BdiAccountingData(
                "ID1",
                "Causale A",
                importo,
                "Banca A",
                INSERT_TIMESTAMP = Instant.now(),
            )
        val data3 =
            BdiAccountingData(
                "ID2",
                "Causale B",
                BigDecimal("50.00"),
                "Banca B",
                INSERT_TIMESTAMP = Instant.now(),
            )

        assertThat(data1.END2END_ID).isEqualTo(data2.END2END_ID)
        assertThat(data1).isNotEqualTo(data3)
    }

    @Test
    fun `should verify BigDecimal equality edge case`() {

        val dataScale0 =
            BdiAccountingData("ID", "C", BigDecimal("10"), "B", INSERT_TIMESTAMP = Instant.now())
        val dataScale2 =
            BdiAccountingData("ID", "C", BigDecimal("10.00"), "B", INSERT_TIMESTAMP = Instant.now())

        assertThat(dataScale0).isNotEqualTo(dataScale2)
    }

    @Test
    fun `should support copy with modification`() {

        val original =
            BdiAccountingData(
                END2END_ID = "OLD_ID",
                CAUSALE = "Old Causale",
                IMPORTO = BigDecimal("10.00"),
                BANCA_ORDINANTE = "Old Bank",
                INSERT_TIMESTAMP = Instant.now(),
            )

        val modified = original.copy(IMPORTO = BigDecimal("99.99"))

        assertThat(modified.IMPORTO).isEqualTo(BigDecimal("99.99"))

        assertThat(modified.END2END_ID).isEqualTo("OLD_ID")
        assertThat(modified.CAUSALE).isEqualTo("Old Causale")
        assertThat(modified.BANCA_ORDINANTE).isEqualTo("Old Bank")
    }

    @Test
    fun `toString should be readable`() {
        val data =
            BdiAccountingData(
                "ID_123",
                "Test",
                BigDecimal("1.0"),
                "MyBank",
                INSERT_TIMESTAMP = Instant.now(),
            )

        assertThat(data.toString())
            .contains("BdiAccountingData")
            .contains("ID_123")
            .contains("Test")
            .contains("MyBank")
    }
}

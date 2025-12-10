package it.pagopa.accounting.reconciliation.bdi.ingestion.documents

import java.math.BigDecimal
import java.time.Instant

data class BdiAccountingData(
    val END2END_ID: String?,
    val CAUSALE: String?,
    val IMPORTO: BigDecimal?,
    val BANCA_ORDINANTE: String?,
    val INSERT_TIMESTAMP: Instant,
)

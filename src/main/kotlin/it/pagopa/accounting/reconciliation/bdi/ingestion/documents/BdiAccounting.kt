package it.pagopa.accounting.reconciliation.bdi.ingestion.documents

import java.math.BigDecimal

data class BdiAccounting(
    val zipFileName: String,
    val xmlFileName: String,
    val end2endId: String?,
    val causale: String?,
    val importo: BigDecimal?,
    val bancaOrdinante: String?,
)

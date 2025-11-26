package it.pagopa.accounting.reconciliation.bdi.ingestion.documents

import java.math.BigDecimal

data class BdiAccountingData(
    val end2endId: String?,
    val causale: String?,
    val importo: BigDecimal?,
    val bancaOrdinante: String?,
)

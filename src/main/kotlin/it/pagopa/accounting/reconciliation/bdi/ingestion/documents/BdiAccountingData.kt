package it.pagopa.accounting.reconciliation.bdi.ingestion.documents

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.math.BigDecimal
import java.time.Instant

@JsonNaming(PropertyNamingStrategies.UpperSnakeCaseStrategy::class)
data class BdiAccountingData(
    val end2endId: String?,
    val causale: String?,
    val importo: BigDecimal?,
    val bancaOrdinante: String?,
    val insertedTimestamp: Instant,
)

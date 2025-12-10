package it.pagopa.accounting.reconciliation.bdi.ingestion.documents

import java.time.Instant
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class AccountingZipDocument(
    @Id val id: String? = null,
    val filename: String,
    val uploadedAt: Instant = Instant.now(),
    val status: String,
)

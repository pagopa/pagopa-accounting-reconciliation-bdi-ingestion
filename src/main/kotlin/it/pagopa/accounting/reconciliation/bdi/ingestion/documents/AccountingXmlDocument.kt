package it.pagopa.accounting.reconciliation.bdi.ingestion.documents

import java.time.Instant
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class AccountingXmlDocument(
    @Id val id: String? = null,
    val zipFilename: String,
    val filename: String,
    val uploadedAt: Instant = Instant.now(),
    val xmlContent: String,
    val status: String,
)

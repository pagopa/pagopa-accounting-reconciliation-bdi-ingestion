package it.pagopa.accounting.reconciliation.bdi.ingestion.documents

import java.time.Instant
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType

@Document("accounting-zip")
data class AccountingZipDocument(
    @Id val id: String = UUID.randomUUID().toString(),
    val filename: String,
    val uploadedAt: Instant = Instant.now(),
    @Field(targetType = FieldType.STRING) val status: AccountingZipStatus,
)

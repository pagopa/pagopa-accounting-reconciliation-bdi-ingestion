package it.pagopa.accounting.reconciliation.bdi.ingestion.documents

import java.time.Instant
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType

@Document("accounting-xml")
data class AccountingXmlDocument(
    @Id val id: String = UUID.randomUUID().toString(),
    val zipFilename: String,
    val filename: String,
    val uploadedAt: Instant = Instant.now(),
    val xmlContent: String,
    @Field(targetType = FieldType.STRING) val status: AccountingXmlStatus,
)

package it.pagopa.accounting.reconciliation.bdi.ingestion.documents

import java.time.Instant
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType

@Document("accounting-xml")
data class AccountingXmlDocument(
    @Id val id: String? = null,
    val zipFilename: String,
    val filename: String,
    @CreatedDate val createdAt: Instant? = null,
    @LastModifiedDate val updatedAt: Instant? = null,
    val xmlContent: String,
    @Field(targetType = FieldType.STRING) val status: AccountingXmlStatus,
)

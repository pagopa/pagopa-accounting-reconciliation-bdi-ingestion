package it.pagopa.accounting.reconciliation.bdi.ingestion.repositories

import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingZipDocument
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingZipDocumentFilenameDto
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface AccountingZipRepository : ReactiveCrudRepository<AccountingZipDocument, String> {

    fun existsByFilename(filename: String): Mono<Boolean>

    @Query(value = "{ 'filename' : { \$in : ?0 } }", fields = "{ 'filename' : 1, '_id': 0 }")
    fun findByFilenameIn(filenames: Collection<String>): Flux<AccountingZipDocumentFilenameDto>
}

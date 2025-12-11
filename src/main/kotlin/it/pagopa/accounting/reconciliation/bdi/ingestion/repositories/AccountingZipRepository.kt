package it.pagopa.accounting.reconciliation.bdi.ingestion.repositories

import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingZipDocument
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
interface AccountingZipRepository : ReactiveCrudRepository<AccountingZipDocument, String> {

    fun existsByFilename(filename: String): Mono<Boolean>
}

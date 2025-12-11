package it.pagopa.accounting.reconciliation.bdi.ingestion.repositories

import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingXmlDocument
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountingXmlRepository : ReactiveCrudRepository<AccountingXmlDocument, String>

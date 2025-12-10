package it.pagopa.accounting.reconciliation.bdi.ingestion.exceptions

class AccountingFilesNotRetrievedException(throwable: Throwable? = null) :
    RuntimeException("Cannot retrieve BDI accounting file list", throwable)

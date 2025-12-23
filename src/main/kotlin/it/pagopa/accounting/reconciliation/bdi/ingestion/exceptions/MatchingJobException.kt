package it.pagopa.accounting.reconciliation.bdi.ingestion.exceptions

class MatchingJobException(throwable: Throwable? = null) :
    RuntimeException("Error executing matching job!", throwable)

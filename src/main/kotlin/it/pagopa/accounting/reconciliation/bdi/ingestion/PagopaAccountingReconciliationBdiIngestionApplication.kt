package it.pagopa.accounting.reconciliation.bdi.ingestion

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PagopaAccountingReconciliationBdiIngestionApplication

fun main(args: Array<String>) {
	runApplication<PagopaAccountingReconciliationBdiIngestionApplication>(*args)
}

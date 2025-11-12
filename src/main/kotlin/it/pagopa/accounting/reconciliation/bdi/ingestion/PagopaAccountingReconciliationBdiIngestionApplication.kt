package it.pagopa.accounting.reconciliation.bdi.ingestion

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class PagopaAccountingReconciliationBdiIngestionApplication

fun main(args: Array<String>) {
	runApplication<PagopaAccountingReconciliationBdiIngestionApplication>(*args)
}

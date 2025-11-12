package it.pagopa.accounting.reconciliation.bdi.ingestion.scheduledjob

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class DataIngestionScheduledJob {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron ="\${data-ingestion-job.execution.cron}")
    fun dataIngestion() {
        logger.info("RUN dataIngestion()")
    }
}
package it.pagopa.accounting.reconciliation.bdi.ingestion.scheduledjob

import it.pagopa.accounting.reconciliation.bdi.ingestion.jobs.AccountingDataIngestionJob
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import reactor.core.publisher.Mono

class DataIngestionScheduledJobTest {
    private val accountingDataIngestionJob: AccountingDataIngestionJob = mock()

    private val dataIngestionScheduledJob = DataIngestionScheduledJob(accountingDataIngestionJob)

    @Test
    fun `Should trigger the inner job process`() {
        // pre-requisites
        given(accountingDataIngestionJob.process(null)).willReturn(Mono.just(10L))

        // test
        dataIngestionScheduledJob.accountingDataIngestion()

        // verifications
        verify(accountingDataIngestionJob, times(1)).process(null)
    }

    @Test
    fun `Should handle processing exception`() {
        // pre-requisites
        given(accountingDataIngestionJob.process(null)).willReturn(Mono.error(RuntimeException("")))

        // test
        assertDoesNotThrow { dataIngestionScheduledJob.accountingDataIngestion() }

        // verifications
        verify(accountingDataIngestionJob, times(1)).process(null)
    }
}

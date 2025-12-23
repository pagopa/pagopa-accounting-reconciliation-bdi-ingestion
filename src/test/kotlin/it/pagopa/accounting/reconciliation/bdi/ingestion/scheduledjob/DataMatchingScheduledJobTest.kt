package it.pagopa.accounting.reconciliation.bdi.ingestion.scheduledjob

import com.microsoft.azure.kusto.data.Client
import com.microsoft.azure.kusto.data.KustoOperationResult
import com.microsoft.azure.kusto.data.KustoResultSetTable
import it.pagopa.accounting.reconciliation.bdi.ingestion.exceptions.MatchingJobException
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertTrue
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class DataMatchingScheduledJobTest {

    private val kustoClient: Client = mock()
    private val timeout: Long = 10
    private val database: String = "test"
    private val bdiTimeshift: String = "7d"
    private val fdrTimeshift: String = "7d"
    private val bdiTable: String = "bdiTable"
    private val fdrTable: String = "fdrTable"
    private val matchingTable: String = "matchingTable"
    private val matchingTableTimeshift: String = "25d"
    private val regexCausaleQuery: String = "regexCausaleQuery"
    private val retries: Long = 2
    private val minBackoffSeconds: Long = 15

    private val dataMatchingScheduledJob =
        DataMatchingScheduledJob(
            kustoClient,
            timeout,
            database,
            bdiTimeshift,
            fdrTimeshift,
            bdiTable,
            fdrTable,
            matchingTable,
            regexCausaleQuery,
            matchingTableTimeshift,
            retries,
            minBackoffSeconds,
        )

    @Test
    fun `should create the new table using the matching query`() {

        var responseObj: KustoOperationResult = mock()
        var table: KustoResultSetTable = mock()
        var rowAffected: Long = 100

        given(kustoClient.executeMgmtAsync(any(), any(), any())).willReturn(Mono.just(responseObj))
        given(responseObj.primaryResults).willReturn(table)
        given(table.hasNext()).willReturn(true)
        given(table.next()).willReturn(true)
        given(table.getLong(any<String>())).willReturn(rowAffected)

        StepVerifier.create(dataMatchingScheduledJob.matchingQuery())
            .expectSubscription()
            .verifyComplete()

        // Verify that the kusto client is called only 1 time
        verify(kustoClient, times(1)).executeMgmtAsync(any(), any(), any())
    }

    @Test
    fun `should fail to call the client for execute the query, must retry and then call throw an Error`() {

        val exception = Exception()

        given(kustoClient.executeMgmtAsync(any(), any(), any())).willReturn(Mono.error(exception))

        StepVerifier.create(dataMatchingScheduledJob.matchingQuery())
            .expectSubscription()
            .thenAwait(Duration.ofSeconds(30))
            .expectErrorSatisfies { error ->
                assertTrue { error.javaClass == MatchingJobException::class.java }
                assertTrue { error.cause == exception }
            }
            .verify()
    }
}

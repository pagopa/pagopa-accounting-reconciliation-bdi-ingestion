package it.pagopa.accounting.reconciliation.bdi.ingestion.clients

import it.pagopa.generated.bdi.api.AccountingApi
import it.pagopa.generated.bdi.model.ListAccountingFiles200ResponseDto
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class BdiClient(private val bdiAccountingApi: AccountingApi) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Calls the listAccountingFiles API
     *
     * @return A [Mono] emitting a [ListAccountingFiles200ResponseDto] containing the list of files
     *   or an error
     */
    fun getAvailableAccountingFiles(): Mono<ListAccountingFiles200ResponseDto> {
        return bdiAccountingApi.listAccountingFiles().doOnError {
            logger.error("Error calling listAccountingFiles API", it)
        }
    }

    /**
     * Calls the getAccountingFile API with a file name as a filter
     *
     * @param fileName The name of the file to download
     * @return A [Mono] emitting a [Resource] or an error
     */
    fun getAccountingFile(fileName: String): Mono<Resource> {
        return bdiAccountingApi.getAccountingFile(fileName).doOnError {
            logger.error("Error calling getAccountingFile API with fileName: $fileName", it)
        }
    }
}

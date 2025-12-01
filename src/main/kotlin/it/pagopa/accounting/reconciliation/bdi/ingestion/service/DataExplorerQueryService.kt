package it.pagopa.accounting.reconciliation.bdi.ingestion.service

import com.microsoft.azure.kusto.data.Client
import com.microsoft.azure.kusto.data.ClientRequestProperties
import com.microsoft.azure.kusto.data.KustoOperationResult
import it.pagopa.generated.bdi.model.FileMetadataDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class DataExplorerQueryService(
    private val dataExplorerClient: Client,
    @Value("\${azuredataexplorer.re.database}") private val database: String,
    @Value("\${azuredataexplorer.re.table}") private val table: String,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun getAllNotSavedFile(filesToCheck: List<FileMetadataDto>): Flux<FileMetadataDto> {
        logger.info("Call getAllNotSavedFile")

        return Mono.fromCallable {
            // 2. Costruiamo la parte "datatable" della query
            // Trasformiamo la lista ["A", "B"] nella stringa: "A", "B"
            // Attenzione: bisogna fare l'escape delle virgolette se i nomi file ne contengono
            val formattedList = filesToCheck.map { it.fileName }.joinToString(", ") { fileName ->
                "\"${fileName.replace("\"", "\\\"")}\""
            }

            // 3. Costruiamo la query completa
            // Usiamo 'datatable' per creare una tabella temporanea con i tuoi input
            // Usiamo 'join kind=leftanti' per trovare quelli che NON matchano
            val kqlQuery = """
                let inputList = datatable(FileName:string) [
                    $formattedList
                ];
                inputList
                | join kind=leftanti (
                    $table 
                    | distinct FileName
                ) on FileName
            """.trimIndent()

            // 4. Eseguiamo la query
            dataExplorerClient.executeQuery(database, kqlQuery, ClientRequestProperties())
        }
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany { kustoResult ->
            extractStringColumn(kustoResult, filesToCheck)
        }
    }

    private fun extractStringColumn(result: KustoOperationResult, fileList: List<FileMetadataDto>): Flux<FileMetadataDto> {
        val resultSet = result.primaryResults
        val values = mutableListOf<String>()

        while (resultSet.next()) {
            val missingFile = resultSet.getString(0)
            if (missingFile != null) {
                values.add(missingFile)
            }
        }
        val filterFileList = fileList.filter { it.fileName in values }
        return Flux.fromIterable(filterFileList)
    }


}
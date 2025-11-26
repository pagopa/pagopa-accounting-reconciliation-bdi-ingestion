package it.pagopa.accounting.reconciliation.bdi.ingestion.config

import io.netty.channel.ChannelOption
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.timeout.ReadTimeoutHandler
import it.pagopa.generated.bdi.ApiClient as BdiApiClient
import it.pagopa.generated.bdi.api.AccountingApi
import java.util.concurrent.TimeUnit
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ssl.SslBundles
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.Connection
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.NameResolverProvider

@Configuration
class WebClientsConfig {

    @Bean
    fun bdiWebClient(
        @Value("\${bdi.server.uri}") serverUri: String,
        @Value("\${bdi.server.readTimeoutMillis}") readTimeoutMillis: Int,
        @Value("\${bdi.server.connectionTimeoutMillis}") connectionTimeoutMillis: Int,
        sslBundles: SslBundles,
    ): WebClient {
        // Create the bundle spring from the name defined into the application properties for the
        // bdi service
        val sslBundle = sslBundles.getBundle("bdi-service")

        // Configure the SSLcontext of Netty using the KeyManager
        val sslContext =
            SslContextBuilder.forClient()
                .keyManager(sslBundle.managers.keyManagerFactory)
                .build()

        val httpClient =
            HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMillis)
                .doOnConnected { connection: Connection ->
                    connection.addHandlerLast(
                        ReadTimeoutHandler(readTimeoutMillis.toLong(), TimeUnit.MILLISECONDS)
                    )
                }
                // SSL configuration
                .secure { t -> t.sslContext(sslContext) }
                .resolver { nameResolverSpec: NameResolverProvider.NameResolverSpec ->
                    nameResolverSpec.ndots(1)
                }

        return BdiApiClient.buildWebClientBuilder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .baseUrl(serverUri)
            .build()
    }

    @Bean
    fun bdiAccountingApi(
        @Value("\${bdi.server.uri}") serverUri: String,
        bdiWebClient: WebClient,
    ): AccountingApi {
        val apiClient = BdiApiClient(bdiWebClient)
        apiClient.setBasePath(serverUri)
        return AccountingApi(apiClient)
    }
}

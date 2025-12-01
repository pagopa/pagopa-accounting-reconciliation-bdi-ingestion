package it.pagopa.accounting.reconciliation.bdi.ingestion.documents

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import java.math.BigDecimal

/**
 * Class that maps a BDI OPI_REND_ANALITICO XML element. It only contains the XML properties that
 * needs to be extracted
 */
@JacksonXmlRootElement(
    localName = "OPI_REND_ANALITICO",
    namespace = "http://tesoreria.bancaditalia.it/rendicontazione/",
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class OpiRendAnalitico(
    @field:JacksonXmlProperty(localName = "movimento") val movimento: Movimento? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Movimento(
    @field:JacksonXmlProperty(localName = "end2endID") val end2endId: String? = null,
    @field:JacksonXmlProperty(localName = "causale") val causale: String? = null,
    @field:JacksonXmlProperty(localName = "importo") val importo: BigDecimal? = null,
    @field:JacksonXmlProperty(localName = "dettaglioMovimento")
    val dettaglioMovimento: DettaglioMovimento? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DettaglioMovimento(
    @field:JacksonXmlProperty(localName = "entrata") val entrata: Entrata? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Entrata(
    @field:JacksonXmlProperty(localName = "bancaOrdinante") val bancaOrdinante: String? = null
)

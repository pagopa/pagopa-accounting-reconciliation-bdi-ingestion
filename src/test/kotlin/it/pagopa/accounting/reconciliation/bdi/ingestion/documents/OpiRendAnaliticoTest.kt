package it.pagopa.accounting.reconciliation.bdi.ingestion.documents

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import java.math.BigDecimal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OpiRendAnaliticoXmlTest {

    private val xmlMapper =
        XmlMapper.builder()
            .defaultUseWrapper(false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build()

    @Test
    fun `should deserialize full XML correctly`() {
        val xmlContent =
            """
                <OPI_REND_ANALITICO xmlns="http://tesoreria.bancaditalia.it/rendicontazione/">
                    <movimento>
                        <end2endID>E2E123456789</end2endID>
                        <causale>Bonifico Stipendio</causale>
                        <importo>1500.50</importo>
                        <dettaglioMovimento>
                            <entrata>
                                <bancaOrdinante>Banca Intesa</bancaOrdinante>
                            </entrata>
                        </dettaglioMovimento>
                    </movimento>
                </OPI_REND_ANALITICO>
            """
                .trimIndent()

        val result = xmlMapper.readValue(xmlContent, OpiRendAnalitico::class.java)

        assertThat(result).isNotNull

        val mov = result.movimento
        assertThat(mov).isNotNull
        assertThat(mov?.end2endId).isEqualTo("E2E123456789")
        assertThat(mov?.causale).isEqualTo("Bonifico Stipendio")
        assertThat(mov?.importo).isEqualByComparingTo(BigDecimal("1500.50"))

        assertThat(mov?.dettaglioMovimento).isNotNull

        val entrata = mov?.dettaglioMovimento?.entrata
        assertThat(entrata).isNotNull
        assertThat(entrata?.bancaOrdinante).isEqualTo("Banca Intesa")
    }

    @Test
    fun `should ignore unknown properties in XML`() {

        val xmlWithExtraTags =
            """
                <OPI_REND_ANALITICO xmlns="http://tesoreria.bancaditalia.it/rendicontazione/">
                    <campoInutile>Valore da ignorare</campoInutile>
                    <movimento>
                        <end2endID>123</end2endID>
                        <campoSconosciuto>Ignorami</campoSconosciuto>
                    </movimento>
                </OPI_REND_ANALITICO>
            """
                .trimIndent()

        val result = xmlMapper.readValue(xmlWithExtraTags, OpiRendAnalitico::class.java)

        assertThat(result.movimento).isNotNull
        assertThat(result.movimento?.end2endId).isEqualTo("123")
    }

    @Test
    fun `should handle missing optional fields (nullability)`() {
        val minimalXml =
            """
                <OPI_REND_ANALITICO xmlns="http://tesoreria.bancaditalia.it/rendicontazione/">
                    <movimento>
                        <end2endID>SOLO_ID</end2endID>
                        </movimento>
                </OPI_REND_ANALITICO>
            """
                .trimIndent()

        val result = xmlMapper.readValue(minimalXml, OpiRendAnalitico::class.java)

        assertThat(result.movimento).isNotNull
        assertThat(result.movimento?.end2endId).isEqualTo("SOLO_ID")

        assertThat(result.movimento?.causale).isNull()
        assertThat(result.movimento?.importo).isNull()
        assertThat(result.movimento?.dettaglioMovimento).isNull()
    }

    @Test
    fun `should deserialize empty tag as null or empty object depending on mapper config`() {
        val xml =
            """
                <OPI_REND_ANALITICO xmlns="http://tesoreria.bancaditalia.it/rendicontazione/">
                </OPI_REND_ANALITICO>
            """
                .trimIndent()

        val result = xmlMapper.readValue(xml, OpiRendAnalitico::class.java)

        assertThat(result).isNotNull
        assertThat(result.movimento).isNull()
    }
}

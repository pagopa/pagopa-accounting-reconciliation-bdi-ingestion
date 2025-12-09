package it.pagopa.accounting.reconciliation.bdi.ingestion.documents

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class OpiRendAnaliticoXmlTest {

    // Creiamo un vero XmlMapper per testare le annotazioni
    private val xmlMapper = XmlMapper.builder()
        .defaultUseWrapper(false)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build()

    @Test
    fun `should deserialize full XML correctly`() {
        // GIVEN - Un XML completo che simula la struttura BDI
        val xmlContent = """
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
        """.trimIndent()

        // WHEN
        val result = xmlMapper.readValue(xmlContent, OpiRendAnalitico::class.java)

        // THEN
        assertThat(result).isNotNull

        // Verifica livello 1: Movimento
        val mov = result.movimento
        assertThat(mov).isNotNull
        assertThat(mov?.end2endId).isEqualTo("E2E123456789")
        assertThat(mov?.causale).isEqualTo("Bonifico Stipendio")
        assertThat(mov?.importo).isEqualByComparingTo(BigDecimal("1500.50"))

        // Verifica livello 2: DettaglioMovimento
        assertThat(mov?.dettaglioMovimento).isNotNull

        // Verifica livello 3: Entrata
        val entrata = mov?.dettaglioMovimento?.entrata
        assertThat(entrata).isNotNull
        assertThat(entrata?.bancaOrdinante).isEqualTo("Banca Intesa")
    }

    @Test
    fun `should ignore unknown properties in XML`() {
        // GIVEN - XML con campi "spazzatura" non mappati nella classe
        // Serve a testare @JsonIgnoreProperties(ignoreUnknown = true)
        val xmlWithExtraTags = """
            <OPI_REND_ANALITICO xmlns="http://tesoreria.bancaditalia.it/rendicontazione/">
                <campoInutile>Valore da ignorare</campoInutile>
                <movimento>
                    <end2endID>123</end2endID>
                    <campoSconosciuto>Ignorami</campoSconosciuto>
                </movimento>
            </OPI_REND_ANALITICO>
        """.trimIndent()

        // WHEN
        val result = xmlMapper.readValue(xmlWithExtraTags, OpiRendAnalitico::class.java)

        // THEN - Non deve esplodere e deve mappare quello che conosce
        assertThat(result.movimento).isNotNull
        assertThat(result.movimento?.end2endId).isEqualTo("123")
    }

    @Test
    fun `should handle missing optional fields (nullability)`() {
        // GIVEN - XML minimale senza i campi opzionali annidati
        val minimalXml = """
            <OPI_REND_ANALITICO xmlns="http://tesoreria.bancaditalia.it/rendicontazione/">
                <movimento>
                    <end2endID>SOLO_ID</end2endID>
                    </movimento>
            </OPI_REND_ANALITICO>
        """.trimIndent()

        // WHEN
        val result = xmlMapper.readValue(minimalXml, OpiRendAnalitico::class.java)

        // THEN
        assertThat(result.movimento).isNotNull
        assertThat(result.movimento?.end2endId).isEqualTo("SOLO_ID")

        // I campi mancanti devono essere null
        assertThat(result.movimento?.causale).isNull()
        assertThat(result.movimento?.importo).isNull()
        assertThat(result.movimento?.dettaglioMovimento).isNull()
    }

    @Test
    fun `should deserialize empty tag as null or empty object depending on mapper config`() {
        val xml = """
            <OPI_REND_ANALITICO xmlns="http://tesoreria.bancaditalia.it/rendicontazione/">
            </OPI_REND_ANALITICO>
        """.trimIndent()

        val result = xmlMapper.readValue(xml, OpiRendAnalitico::class.java)

        assertThat(result).isNotNull
        assertThat(result.movimento).isNull()
    }
}
package it.pagopa.accounting.reconciliation.bdi.ingestion.service

import it.pagopa.accounting.reconciliation.bdi.ingestion.clients.BdiClient
import it.pagopa.accounting.reconciliation.bdi.ingestion.documents.AccountingZipDocument
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.Security
import java.time.Instant
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.test.assertTrue
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.willReturn
import org.springframework.core.io.InputStreamResource
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class ReactiveP7mZipServiceTest {
    private val bdiClient: BdiClient = mock()
    private val xmlParserService: XmlParserService = mock()
    private val reactiveP7mZipService = ReactiveP7mZipService(bdiClient, xmlParserService)

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            if (Security.getProvider("BC") == null) {
                Security.addProvider(BouncyCastleProvider())
            }
        }
    }

    @Test
    fun `should process the zip file and call the xmlParserService`() {

        val files =
            mapOf(
                "test_1.xml" to "Test 1",
                "test_2.xml" to "",
                "test.txt" to "This should be ignored",
                "directory/" to "",
            )
        val zipFile = P7mTestGenerator.createP7mWithZip(files)
        val resource = InputStreamResource(zipFile)

        val accountingZipDocument =
            AccountingZipDocument("test_id", "test_file", Instant.now(), "test_status")

        given(bdiClient.getAccountingFile(any())).willReturn { Mono.just(resource) }
        given(xmlParserService.processXmlFile(any())).willReturn { Mono.just(Unit) }

        StepVerifier.create(reactiveP7mZipService.processZipFile(accountingZipDocument))
            .expectNext(Unit)
            .verifyComplete()

        verify(bdiClient, times(1)).getAccountingFile("test_file")
        verify(xmlParserService, times(2)).processXmlFile(any())
    }

    @Test
    fun `should process the zip file and return an error if P7M is a Detached Signature (no content inside)`() {

        val files = mapOf("test.xml" to "<root>content</root>")
        val zipFile = P7mTestGenerator.createP7mWithZip(files, encapsulate = false)

        val resource = InputStreamResource(zipFile)

        val accountingZipDocument =
            AccountingZipDocument("test_id", "test_file", Instant.now(), "test_status")

        given(bdiClient.getAccountingFile(any())).willReturn { Mono.just(resource) }

        StepVerifier.create(reactiveP7mZipService.processZipFile(accountingZipDocument))
            .expectErrorSatisfies { error ->
                assertTrue { error.javaClass == IllegalArgumentException::class.java }
                assertTrue { error.message == "No content found in P7M" }
            }
            .verify()

        verify(bdiClient, times(1)).getAccountingFile("test_file")
    }

    @Test
    fun `extractAndMap should fail if input is not a P7M file`() {
        // pre-requisites
        val inputStream = ByteArrayInputStream("test".toByteArray())
        val resource = InputStreamResource(inputStream)

        val accountingZipDocument =
            AccountingZipDocument("test_id", "test_file", Instant.now(), "test_status")

        given(bdiClient.getAccountingFile(any())).willReturn { Mono.just(resource) }

        StepVerifier.create(reactiveP7mZipService.processZipFile(accountingZipDocument))
            .expectError()
            .verify()

        verify(bdiClient, times(1)).getAccountingFile("test_file")
    }

    @Test
    fun `processZipEntries should close the zip if the file is corrupted and continue without error`() {
        val files =
            mapOf(
                "test_1.xml" to "Test 1",
                "test_2.xml" to "",
                "test.txt" to "This should be ignored",
                "directory/" to "",
            )
        val zipFile = P7mTestGenerator.createP7mWithCorruptedZip(files)
        val resource = InputStreamResource(zipFile)

        val accountingZipDocument =
            AccountingZipDocument("test_id", "test_file", Instant.now(), "test_status")

        given(bdiClient.getAccountingFile(any())).willReturn { Mono.just(resource) }
        given(xmlParserService.processXmlFile(any())).willReturn { Mono.just(Unit) }

        StepVerifier.create(reactiveP7mZipService.processZipFile(accountingZipDocument))
            .expectNext(Unit)
            .verifyComplete()

        verify(bdiClient, times(1)).getAccountingFile("test_file")
    }

    @Test
    fun `processZipEntries should stop processing zip if the sink is cancelled`() {
        val files =
            mapOf(
                "test_1.xml" to "Test 1",
                "test_2.xml" to "",
                "test.txt" to "This should be ignored",
                "directory/" to "",
            )
        val zipFile = P7mTestGenerator.createP7mWithCorruptedZip(files)
        val spyStream = spy(zipFile)
        val resource = InputStreamResource(spyStream)

        val accountingZipDocument =
            AccountingZipDocument("test_id", "test_file", Instant.now(), "test_status")

        given(bdiClient.getAccountingFile(any())).willReturn { Mono.just(resource) }

        StepVerifier.create(reactiveP7mZipService.processZipFile(accountingZipDocument))
            .thenCancel()
            .verify()

        assert(spyStream.available() > 0)
        verify(bdiClient, times(1)).getAccountingFile("test_file")
    }
}

/** Utility object to generate P7M files in memory */
object P7mTestGenerator {

    fun createP7mWithZip(files: Map<String, String>, encapsulate: Boolean = true): InputStream {
        val zipBytes = createZip(files)
        val p7mBytes = signData(zipBytes, encapsulate)
        return ByteArrayInputStream(p7mBytes)
    }

    fun createP7mWithCorruptedZip(
        files: Map<String, String>,
        encapsulate: Boolean = true,
    ): InputStream {
        val zipBytes = createTrunkedZip(files)
        val p7mBytes = signData(zipBytes, encapsulate)
        return ByteArrayInputStream(p7mBytes)
    }

    fun createZip(files: Map<String, String>): ByteArray {
        val stream = ByteArrayOutputStream()
        ZipOutputStream(stream).use { zos ->
            files.forEach { (name, content) ->
                val entry = ZipEntry(name)
                zos.putNextEntry(entry)

                if (!name.endsWith("/")) {
                    zos.write(content.toByteArray(StandardCharsets.UTF_8))
                }

                zos.closeEntry()
            }
        }
        return stream.toByteArray()
    }

    fun createTrunkedZip(files: Map<String, String>): ByteArray {
        val stream = ByteArrayOutputStream()
        ZipOutputStream(stream).use { zos ->
            files.forEach { (name, content) ->
                val entry = ZipEntry(name)
                zos.putNextEntry(entry)

                if (!name.endsWith("/")) {
                    zos.write(content.toByteArray(StandardCharsets.UTF_8))
                }

                zos.closeEntry()
            }
        }
        stream.size()
        val zipByteArray = stream.toByteArray()
        val brokenZipByteArray = zipByteArray.copyOfRange(0, zipByteArray.lastIndex - 10)

        return brokenZipByteArray
    }

    fun signData(data: ByteArray, encapsulate: Boolean): ByteArray {
        val keyPairGen = KeyPairGenerator.getInstance("RSA")
        keyPairGen.initialize(2048)
        val keyPair = keyPairGen.generateKeyPair()

        val certBuilder =
            JcaX509v3CertificateBuilder(
                X500Name("CN=Test"),
                BigInteger.ONE,
                Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24),
                Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24),
                X500Name("CN=Test"),
                keyPair.public,
            )
        val contentSigner =
            JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC").build(keyPair.private)
        val cert = certBuilder.build(contentSigner)
        val gen = CMSSignedDataGenerator()
        val digestProvider = JcaDigestCalculatorProviderBuilder().setProvider("BC").build()
        val signerInfoGenerator =
            JcaSignerInfoGeneratorBuilder(digestProvider).build(contentSigner, cert)

        gen.addSignerInfoGenerator(signerInfoGenerator)
        gen.addCertificate(cert)

        val processable = CMSProcessableByteArray(data)
        val signedData = gen.generate(processable, encapsulate)

        return signedData.encoded
    }
}

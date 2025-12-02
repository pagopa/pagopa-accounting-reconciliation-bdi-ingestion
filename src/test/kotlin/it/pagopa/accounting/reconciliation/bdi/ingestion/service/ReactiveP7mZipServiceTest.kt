package it.pagopa.accounting.reconciliation.bdi.ingestion.service

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.Security
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
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
import reactor.test.StepVerifier

class ReactiveP7mZipServiceTest {
    private val reactiveP7mZipService = ReactiveP7mZipService()

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
    fun `extractAndMap should decrypt P7M, unzip, filter files and map content`() {
        // pre-requisites
        val files =
            mapOf(
                "test_1.test" to "Test 1",
                "test_2.test" to "Test 2",
                "test.txt" to "This should be ignored",
                "directory/" to "",
            )

        val resultFlux =
            reactiveP7mZipService.extractAndMap(
                p7mZipInputStream = P7mTestGenerator.createP7mWithZip(files),
                entryNameFilter = { it.endsWith(".test") },
                mapper = { stream -> String(stream.readAllBytes(), StandardCharsets.UTF_8) },
            )

        // test
        StepVerifier.create(resultFlux)
            .expectNextMatches { it.contains("Test 1") || it.contains("Test 2") }
            .expectNextMatches { it.contains("Test 1") || it.contains("Test 2") }
            .expectComplete()
            .verify()
    }

    @Test
    fun `extractAndMap should skip files where mapper throws exception`() {
        // pre-requisites
        val files = mapOf("valid.xml" to "Valid Content", "invalid.xml" to "Error")

        val resultFlux =
            reactiveP7mZipService.extractAndMap(
                p7mZipInputStream = P7mTestGenerator.createP7mWithZip(files),
                entryNameFilter = { it.endsWith(".xml") },
                mapper = { stream ->
                    val content = String(stream.readAllBytes())
                    if (content == "Error") {
                        throw RuntimeException("Mapper failed!")
                    }
                    content
                },
            )

        // test
        StepVerifier.create(resultFlux).expectNext("Valid Content").expectComplete().verify()
    }

    @Test
    fun `extractAndMap should fail if input is not a P7M file`() {
        // pre-requisites
        val inputStream = ByteArrayInputStream("test".toByteArray())

        val resultFlux =
            reactiveP7mZipService.extractAndMap(
                p7mZipInputStream = inputStream,
                entryNameFilter = { true },
                mapper = { it.toString() },
            )

        // test
        StepVerifier.create(resultFlux).expectError().verify()
    }

    @Test
    fun `extractAndMap should fail if P7M is a Detached Signature (no content inside)`() {
        // pre-requisites
        val files = mapOf("test.xml" to "<root>content</root>")

        val resultFlux =
            reactiveP7mZipService.extractAndMap(
                // encapsulate = false creates a detached signature
                p7mZipInputStream = P7mTestGenerator.createP7mWithZip(files, encapsulate = false),
                entryNameFilter = { true },
                mapper = { it.toString() },
            )

        // test
        StepVerifier.create(resultFlux)
            .expectErrorMatches { error ->
                error is IllegalArgumentException && error.message == "No content found in P7M"
            }
            .verify()
    }
}

/** Utility object to generate P7M files in memory */
object P7mTestGenerator {

    fun createP7mWithZip(files: Map<String, String>, encapsulate: Boolean = true): InputStream {
        val zipBytes = createZip(files)
        val p7mBytes = signData(zipBytes, encapsulate)
        return ByteArrayInputStream(p7mBytes)
    }

    private fun createZip(files: Map<String, String>): ByteArray {
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

    private fun signData(data: ByteArray, encapsulate: Boolean): ByteArray {
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

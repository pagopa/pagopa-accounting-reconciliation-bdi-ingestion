package it.pagopa.accounting.reconciliation.bdi.ingestion.service

import java.io.BufferedInputStream
import java.io.InputStream
import java.security.Security
import java.util.zip.ZipInputStream
import org.bouncycastle.cms.CMSSignedDataParser
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StreamUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.scheduler.Schedulers

@Service
class ReactiveP7mZipService {
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        if (Security.getProvider("BC") == null) Security.addProvider(BouncyCastleProvider())
    }

    /**
     * Unwraps a P7M-signed input stream, extracts the contained ZIP entries, and maps them to a
     * domain object of type [T].
     *
     * @param T The type of the object to be produced by the mapper.
     * @param p7mZipInputStream The source [InputStream] containing the `.zip.p7m` data.
     * @param entryNameFilter A predicate that returns true if the ZIP entry should be processed
     *   (e.g. check extension).
     * @param mapper A lambda that accepts an [InputStream] (the unzipped entry content) and returns
     *   an instance of [T]. The provided stream is a "non-closing" wrapper; the mapper should read
     *   from it but does not need to close it.
     * @return A [Flux] emitting the mapped objects extracted from the stream.
     * @throws IllegalArgumentException (Emitted as error signal) If the P7M structure contains no
     *   signed content.
     */
    fun <T : Any> extractAndMap(
        p7mZipInputStream: InputStream,
        entryNameFilter: (String) -> Boolean,
        mapper: (String, InputStream) -> T,
    ): Flux<T> {
        return Flux.create<T> { sink ->
                try {
                    // P7M Unwrapping
                    BufferedInputStream(p7mZipInputStream).use { bufferedIn ->
                        val digestProvider =
                            JcaDigestCalculatorProviderBuilder().setProvider("BC").build()
                        val cmsParser = CMSSignedDataParser(digestProvider, bufferedIn)

                        val signedContent =
                            cmsParser.signedContent
                                ?: throw IllegalArgumentException("No content found in P7M")

                        signedContent.contentStream.use { rawCmsStream ->
                            processZipEntries(rawCmsStream, sink, entryNameFilter, mapper)
                        }
                        // Drain the parser (Required to prevent EOFException)
                        cmsParser.signerInfos
                    }
                    sink.complete()
                } catch (e: Exception) {
                    sink.error(e)
                }
            }
            .subscribeOn(Schedulers.boundedElastic())
    }

    private fun <T : Any> processZipEntries(
        decryptedStream: InputStream,
        sink: FluxSink<T>,
        entryNameFilter: (String) -> Boolean,
        mapper: (String, InputStream) -> T,
    ) {
        BufferedInputStream(decryptedStream).use { bufferedCmsStream ->
            ZipInputStream(bufferedCmsStream).use { zipStream ->
                var entry = zipStream.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entryNameFilter(entry.name)) {
                        try {
                            // protect the stream to avoid it to being closed by the mapper function
                            val protectedStream = StreamUtils.nonClosing(zipStream)
                            val result = mapper(entry.name, protectedStream)
                            sink.next(result)
                        } catch (e: Exception) {
                            logger.warn(
                                "Skipping corrupted file entry: ${entry.name}. Reason: ${e.message}"
                            )
                        } finally {
                            zipStream.closeEntry()
                        }
                    }
                    entry = zipStream.nextEntry
                }
            }
        }
    }
}

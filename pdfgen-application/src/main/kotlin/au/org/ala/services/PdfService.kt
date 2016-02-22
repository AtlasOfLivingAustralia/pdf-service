package au.org.ala.services

import com.google.common.hash.Hashing
import com.google.common.hash.HashingOutputStream
import com.google.common.io.Files
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*
import javax.ws.rs.WebApplicationException

class PdfService(val exec: String, val storageDir: File) {

    companion object {
        private val log = LoggerFactory.getLogger(PdfService::class.java)
    }

    fun fileForSha(sha: String): File = File(storageDir, "$sha.pdf")

    fun hashAndConvert(stream: InputStream): String {
        val outDir = Files.createTempDir()
        try {
            log.debug("Created temp dir $outDir")
            val tempFile = File(outDir, UUID.randomUUID().toString())
            log.debug("Using temp file $tempFile")

            val hash = HashingOutputStream(Hashing.sha256(), FileOutputStream(tempFile)).use {
                stream.copyTo(it)
                it.flush()
                it.hash()
            }
            val hashString = hash.toString()
            log.debug("Hash is $hashString")

            val pdfFile = fileForSha(hashString)
            log.debug("PDF file is $pdfFile")
            if (!pdfFile.exists()) {
                log.debug("PDF file does not exist, generating...")
                val pb = ProcessBuilder(exec, "--nologo", "--headless", "--nofirststartwizard", "--convert-to", "pdf", "--outdir", outDir.toString(), tempFile.toString())
                log.debug("Running ${pb.command()}")
                val p = pb.start()

                val stdout = p.inputStream.reader().readText()
                val stderr = p.errorStream.reader().readText()
                val exit = p.waitFor()
                if (exit > 0) {
                    log.error("Exit code $exit converting $tempFile")
                    log.error("$tempFile stdout:\n$stdout")
                    log.error("$tempFile stderr:\n$stderr")
                    throw WebApplicationException(500)
                }
                val tmpPdf = File(outDir, "${tempFile.name}.pdf")
                log.debug("Temp PDF generated at $tmpPdf")
                tmpPdf.copyTo(pdfFile)
            }
            return hashString
        } finally {
            outDir.deleteRecursively()
        }
    }
}
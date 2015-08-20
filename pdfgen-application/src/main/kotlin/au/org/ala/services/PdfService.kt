package au.org.ala.services

import com.google.common.hash.Hashing
import com.google.common.hash.HashingOutputStream
import com.google.common.io.ByteStreams
import com.google.common.io.Files
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*
import javax.ws.rs.WebApplicationException

public class PdfService(val exec: String, val storageDir: String) {

    companion object {
        private val log = LoggerFactory.getLogger(PdfService.javaClass)
    }

    fun fileForSha(sha: String): File = File(storageDir, "$sha.pdf")

    fun hashAndConvert(stream: InputStream): String {
        val outDir = Files.createTempDir()
        log.debug("Created temp dir $outDir")
        val tempFile = File(outDir, UUID.randomUUID().toString())
        log.debug("Using temp file $tempFile")

        val hash = HashingOutputStream(Hashing.sha256(), FileOutputStream(tempFile)).use {
            ByteStreams.copy(stream, it)
            it.flush()
            it.hash()
        }
        val hashString = hash.toString()
        log.debug("Hash is $hashString")

        val pdfFile = File(storageDir, "$hashString.pdf")
        log.debug("PDF file is $pdfFile")
        if (!pdfFile.exists()) {
            log.debug("PDF file does not exist, generating...")
            //unoconv --connection 'socket,host=127.0.0.1,port=2220,tcpNoDelay=1;urp;StarOffice.ComponentContext' -f pdf test.html
            val p = ProcessBuilder(exec, "--connection", "'socket,host=127.0.0.1,port=2220,tcpNoDelay=1;urp;StarOffice.ComponentContext'", "-f", "pdf", tempFile.toString()).start()
            //val p = ProcessBuilder(exec, "--convert-to", "pdf", "--headless", "--outdir", outDir.toString(), tempFile.toString()).start()
            val stdout = p.getInputStream().reader().readText()
            val stderr = p.getErrorStream().reader().readText()
            val exit = p.waitFor()
            if (exit > 0) {
                log.error("Non zero error code converting $tempFile")
                log.error("$tempFile stdout:\n$stdout")
                log.error("$tempFile stderr:\n$stderr")
                throw WebApplicationException(500)
            }
            val tmpPdf = File(outDir, "${tempFile.name}.pdf")
            log.debug("Temp PDF generated at $tmpPdf")
            Files.copy(tmpPdf, pdfFile)
        }
        outDir.deleteRecursively()

        return hashString
    }
}
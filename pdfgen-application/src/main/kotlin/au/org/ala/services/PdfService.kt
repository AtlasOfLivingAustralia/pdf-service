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

class PdfService(val loExec: String, val htmltopdfExec: String, val storageDir: File) {

    companion object {
        private val log = LoggerFactory.getLogger(PdfService::class.java)
    }

    fun fileForSha(sha: String): File = File(storageDir, "$sha.pdf")

    private fun hash(stream: InputStream, tempFile: File): String {
        val hash = HashingOutputStream(Hashing.sha256(), FileOutputStream(tempFile)).use {
            stream.copyTo(it)
            it.flush()
            it.hash()
        }
        return hash.toString()
    }

    private fun convertToPDF(contentHash: String, inputFile: File, outputFile : File, conversionProcess: ProcessBuilder) {

        val pdfFile = fileForSha(contentHash)
        log.debug("PDF file is $pdfFile")
        if (!pdfFile.exists()) {
            log.debug("PDF file does not exist, generating...")
            log.debug("Running ${conversionProcess.command()}")
            val p = conversionProcess.start()

            val stdout = p.inputStream.reader().readText()
            val stderr = p.errorStream.reader().readText()
            val exit = p.waitFor()
            if (exit > 0) {
                log.error("Exit code $exit converting $inputFile")
                log.error("$inputFile stdout:\n$stdout")
                log.error("$inputFile stderr:\n$stderr")
                throw WebApplicationException(500)
            }
            log.debug("Temp PDF generated at $outputFile")
            outputFile.copyTo(pdfFile)
        }
    }

    fun hashAndConvertDocument(stream: InputStream): String {

        val outDir = Files.createTempDir()
        try {
            log.debug("Created temp dir $outDir")
            val tempFile = File(outDir, UUID.randomUUID().toString())
            log.debug("Using temp file $tempFile")

            val hashString = hash(stream, tempFile)
            val pdfFile = fileForSha(hashString)
            log.debug("PDF file is $pdfFile")

            val pb = ProcessBuilder(loExec, "--nologo", "--headless", "--nofirststartwizard", "--convert-to", "pdf", "--outdir", outDir.toString(), tempFile.toString())
            val outputFile = File(outDir, "${tempFile.name}.pdf")
            convertToPDF(hashString, tempFile, outputFile, pb)
            return hashString
        } finally {
            //outDir.deleteRecursively()
        }
    }

    fun hashAndConvertHtml(url: String, stream: InputStream): String {
        val outDir = Files.createTempDir()
        try {
            log.debug("Created temp dir $outDir")
            val tempFile = File(outDir, UUID.randomUUID().toString())
            log.debug("Using temp file $tempFile")

            val hashString = hash(stream, tempFile)

            val outputFile = File(outDir, "${tempFile.name}.pdf")

            val conversionProcessCommand = ProcessBuilder(htmltopdfExec, "--javascript-delay", "10000", url, outputFile.toString())
            convertToPDF(hashString, tempFile, outputFile, conversionProcessCommand)
            return hashString
        } finally {
            outDir.deleteRecursively()
        }

    }
}
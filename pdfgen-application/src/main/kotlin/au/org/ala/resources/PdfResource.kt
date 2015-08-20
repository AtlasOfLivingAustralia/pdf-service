package au.org.ala.resources

import com.google.common.hash.Hashing
import com.google.common.hash.HashingOutputStream
import com.google.common.io.Files
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.client.Client
import javax.ws.rs.core.Response

Path("pdf")
public class PdfResource(val client: HttpClient, val exec: String, val storageDir: String) {

    GET fun pdf(QueryParam("docUrl") docUrl: String): File {
        val response = client.execute(HttpGet(docUrl))
        if (response.getStatusLine().getStatusCode() == 200) {
            val outDir = Files.createTempDir()
            val tempFile = File(outDir, UUID.randomUUID().toString())
            val hash = HashingOutputStream(Hashing.sha256(), FileOutputStream(tempFile)).use {
                response.getEntity().writeTo(it)
                it.flush()
                it.hash()
            }

            val pdfFile = File(storageDir, "${hash.toString()}.pdf")
            if (!pdfFile.exists()) {

                val exit = ProcessBuilder(exec, "--convert-to", "pdf", "--headless", "--outdir", outDir.toString(), tempFile.toString()).start().waitFor()
                if (exit > 0) throw WebApplicationException(500)
                val tmpPdf = File(outDir, "${tempFile.name}.pdf")
                Files.copy(tmpPdf, pdfFile)
            }
            outDir.deleteRecursively()

            return pdfFile
        } else {
            throw WebApplicationException(400)
        }
    }
}
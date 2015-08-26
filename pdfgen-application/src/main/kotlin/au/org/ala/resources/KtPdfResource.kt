package au.org.ala.resources

import au.org.ala.services.PdfService
import com.codahale.metrics.annotation.Timed
import com.google.common.hash.Hashing
import com.google.common.hash.HashingOutputStream
import com.google.common.io.ByteStreams
import com.google.common.io.Files
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.glassfish.jersey.media.multipart.FormDataParam
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*
import javax.inject.Singleton
import javax.ws.rs.*
import javax.ws.rs.client.Client
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

/**
 * Disabled because Kotlin adds extra annotations to method parameters, which the Jersey FormDataParam extractor
 * can't deal with.
 */
Path("pdf")
public class KtPdfResource(val client: HttpClient, val service: PdfService) {

    companion object {
        private val log = LoggerFactory.getLogger(KtPdfResource.javaClass)
    }

    Timed GET
    public fun generate(QueryParam("docUrl") docUrl: String?,
            Context info: UriInfo): Response {

        if (docUrl == null) throw WebApplicationException(400)

        val response = client.execute(HttpGet(docUrl))
        if (response.getStatusLine().getStatusCode() == 200) {
            val hash = response.getEntity().getContent().use {
                service.hashAndConvert(it)
            }

            return Response.status(Response.Status.MOVED_PERMANENTLY).location(info.getBaseUriBuilder().path(hash).build()).build()
        } else {
            throw WebApplicationException(400)
        }
    }

    Timed POST Consumes(MediaType.MULTIPART_FORM_DATA)
    public fun upload(FormDataParam("file") file: InputStream,
               FormDataParam("file") contentDispositionHeader: FormDataContentDisposition,
               Context info: UriInfo ): Response {
        //if (file == null) throw WebApplicationException(400)

        val hash = file.use {
            service.hashAndConvert(it)
        }

        return Response.status(Response.Status.SEE_OTHER).location(info.getBaseUriBuilder().path(KtPdfResource.javaClass).path(hash).build()).build()
    }

    GET Path("{sha}")
    public fun pdf(PathParam("sha") sha: String): File = service.fileForSha(sha)

}
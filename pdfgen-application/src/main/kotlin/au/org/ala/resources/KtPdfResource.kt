package au.org.ala.resources

import au.org.ala.services.PdfService
import com.codahale.metrics.annotation.Timed
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.util.concurrent.UncheckedExecutionException
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpRequestBase
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.glassfish.jersey.media.multipart.FormDataParam
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.net.URI
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

/**
 * Disabled because Kotlin adds extra annotations to method parameters, which the Jersey FormDataParam extractor
 * can't deal with.
 */
@Path("pdf") class KtPdfResource(private val client: HttpClient, private val service: PdfService, urlCacheSpec: String) {

    companion object {
        private val log = LoggerFactory.getLogger(KtPdfResource::class.java)

        internal fun buildPdfURI(info: UriInfo, hash: String): URI {
            return info.baseUriBuilder.path(PdfResource::class.java).path(PdfResource::class.java, "pdf").build(hash)
        }
    }

    private val cache = CacheBuilder.from(urlCacheSpec).build(object: CacheLoader<String,String>() {
        override fun load(key: String): String = downloadAndHash(key)
    })

    @Timed @GET fun generate(@QueryParam("docUrl") docUrl: String?,
                             @Context info: UriInfo): Response {

        if (docUrl == null) throw WebApplicationException(400)

        try {
            return Response.status(Response.Status.MOVED_PERMANENTLY).location(buildPdfURI(info, cache.getUnchecked(docUrl))).build()
        } catch (e: UncheckedExecutionException) {
            val c = e.cause
            if (c is WebApplicationException)
                throw c
            else {
                log.warn("Caught exception while trying to generate pdf for {}", docUrl, e)
                throw WebApplicationException(500)
            }
        }

    }

    @Timed @POST @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun upload(@FormDataParam("file") file: InputStream,
               @FormDataParam("file") contentDispositionHeader: FormDataContentDisposition,
               @Context info: UriInfo ): Response {
        //if (file == null) throw WebApplicationException(400)

        try {
            return file.use { Response.status(Response.Status.SEE_OTHER).location(buildPdfURI(info, service.hashAndConvertDocument(it))).build() }
        } catch (e: IOException) {
            log.error("Error converting file upload: {}", contentDispositionHeader.name, e)
            throw WebApplicationException(500)
        }

    }

    @GET @Path("{sha}") @Produces("application/pdf") fun pdf(@PathParam("sha") sha: String): Response {
        val file = service.fileForSha(sha)
        log.debug("Sending file ${file.absolutePath}")
        return Response.ok(file).header("Content-Length", file.length()).build();
    }


    internal fun downloadAndHash(docUrl: String): String {
        return HttpGet(docUrl).use {
            val response = client.execute(it)
            val status = response.statusLine.statusCode
            if (status != 200) {
                log.warn("HTTP error $status retrieving $docUrl")
                throw WebApplicationException(400)
            }
            response.entity.content.use { service.hashAndConvertDocument(it) }
        }
    }
}

fun <T> HttpRequestBase.use(f: (HttpRequestBase) -> T): T {
    try {
        return f(this)
    } finally {
        this.reset()
    }
}
package au.org.ala.resources;

import au.org.ala.services.PdfService;
import com.codahale.metrics.annotation.Timed;
import com.google.common.cache.*;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;

@Path("pdf")
public class PdfResource implements RemovalListener<String, String> {

    private static final Logger log = LoggerFactory.getLogger(PdfResource.class);

    private final HttpClient client;
    private final PdfService service;

    // TODO inject
    private final LoadingCache<String, String> cache;

    private final LoadingCache<String, String> tempCache;

    public PdfResource(HttpClient client, PdfService service, String urlCacheSpec) {
        this.client = client;
        this.service = service;
        this.cache = CacheBuilder.from(urlCacheSpec).removalListener(this).build(new CacheLoader<String, String>() {
            @Override
            public String load(@NotNull String key) {
                return downloadAndHash(key);
            }
        });
        this.tempCache = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).removalListener(this).build(new CacheLoader<String, String>() {
            @Override
            public String load(@NotNull String key) {
                return downloadAndHash(key);
            }
        });
    }

    @Override
    public void onRemoval(RemovalNotification<String, String> notification) {
        log.debug("onRemoval for: "+notification.toString()+", cause: "+notification.getCause());
        if (notification.wasEvicted()) {
            File pdf = service.fileForSha(notification.getValue());
            if (pdf.exists()) {
                log.info("Deleting file: "+pdf.getPath());
                pdf.delete();
            }
        }
    }

    @Timed
    @GET
    public Response generate(@QueryParam("docUrl") String docUrl, @Context UriInfo info, @QueryParam("cacheable") String cacheable) {

        if (docUrl == null) throw new WebApplicationException(400);
        boolean canCache = cacheable != null ? Boolean.valueOf(cacheable) : true;

        try {
            if (canCache) {
                return Response.status(Response.Status.TEMPORARY_REDIRECT).location(buildPdfURI(info, this.cache.getUnchecked(docUrl))).build();
            }
            else {
                // The main reason for using this cache is to allow time for the file to be
                // downloaded by the client - it will be deleted when the entry expires.
                String sha = this.tempCache.getUnchecked(docUrl);
                tempCache.put(docUrl, sha);
                return respondWithPDF(sha);
            }
        } catch (UncheckedExecutionException e) {
            if (e.getCause() instanceof WebApplicationException) throw (WebApplicationException) e.getCause();
            else {
                log.warn("Caught unexpected exception while trying to generate pdf for {}", docUrl, e);
                throw new WebApplicationException(500);
            }
        }
    }

    @Timed
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response upload(@FormDataParam("file") InputStream file,
                           @FormDataParam("file") FormDataContentDisposition contentDispositionHeader,
                           @Context UriInfo info) {
        //if (file == null) throw WebApplicationException(400)

        try (InputStream it = file) {

            return Response.status(Response.Status.SEE_OTHER).location(buildPdfURI(info, service.hashAndConvertDocument(it))).build();
        } catch (IOException e) {
            log.error("Error converting file upload: {}", contentDispositionHeader.getName(), e);
            throw new WebApplicationException(500);
        }
    }

    String downloadAndHash(String docUrl) {

        HttpGet get = null;

        try {
            get = new HttpGet(docUrl);

            HttpResponse response = client.execute(get);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String hash;
                HttpEntity responseEntity = response.getEntity();
                ContentType contentType = ContentType.get(responseEntity);

                try (InputStream it = responseEntity.getContent()) {
                    if (ContentType.TEXT_HTML.getMimeType().equals(contentType.getMimeType())) {
                        hash = service.hashAndConvertHtml(docUrl, it);
                    } else {
                        hash = service.hashAndConvertDocument(it);
                    }
                }

                return hash;
            } else {
                log.warn("HTTP error {} retrieving {}", response.getStatusLine().getStatusCode(), docUrl);
                throw new WebApplicationException(400);
            }
        } catch (Exception e) {
            log.error("Error calling {}", docUrl, e);
            throw new WebApplicationException(500);
        } finally {
            if (get != null) {
                get.reset();
            }

        }
    }

    static URI buildPdfURI(UriInfo info, String hash) {
        return info.getBaseUriBuilder().path(PdfResource.class).path(PdfResource.class, "pdf").build(hash);
    }

    @GET
    @Path("{sha}")
    @Produces("application/pdf")
    public Response pdf(@PathParam("sha") String sha) {
        return respondWithPDF(sha);
    }

    private Response respondWithPDF(String sha) {
        final File file = service.fileForSha(sha);
        log.debug("Sending file {}", file.getAbsolutePath());
        return Response.ok(file).type("application/pdf").header("Content-Length", file.length()).build();
    }
}

package au.org.ala.resources;

import au.org.ala.services.PdfService;
import com.codahale.metrics.annotation.Timed;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Path("pdf")
public class PdfResource {

    private static final Logger log = LoggerFactory.getLogger(PdfResource.class);

    private final HttpClient client;
    private final PdfService service;

    // TODO inject
    private final LoadingCache<String, String> cache;

    public PdfResource(HttpClient client, PdfService service) {
        this.client = client;
        this.service = service;
        this.cache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build(new CacheLoader<String, String>() {
            @Override
            public String load(String key) {
                return downloadAndHash(key);
            }
        });
    }

    @Timed
    @GET
    public Response generate(@QueryParam("docUrl") String docUrl, @Context UriInfo info) {

        if (docUrl == null) throw new WebApplicationException(400);

        try {
            return Response.status(Response.Status.SEE_OTHER).location(buildPdfURI(info, this.cache.getUnchecked(docUrl))).build();
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

        String hash;
        try (InputStream it = file) {
            hash = service.hashAndConvert(it);
        } catch (IOException e) {
            log.error("Error converting {}", contentDispositionHeader.getName(), e);
            throw new WebApplicationException(500);
        }

        return Response.status(Response.Status.SEE_OTHER).location(buildPdfURI(info, hash)).build();
    }

    String downloadAndHash(String docUrl) {
        HttpGet get = new HttpGet(docUrl);
        try {
            HttpResponse response = client.execute(get);
            if (response.getStatusLine().getStatusCode() == 200) {
                String hash;
                try (InputStream it = response.getEntity().getContent()) {
                    hash = service.hashAndConvert(it);
                }
                return hash;
            } else {
                log.warn("HTTP error {} retrieving {}", response.getStatusLine().getStatusCode(), docUrl);
                throw new WebApplicationException(400);
            }
        } catch (IOException e) {
            log.error("Error calling {}", docUrl, e);
            throw new WebApplicationException(500);
        } finally {
            get.releaseConnection();
        }
    }

    static URI buildPdfURI(UriInfo info, String hash) {
        return info.getBaseUriBuilder().path(PdfResource.class).path(hash).build();
    }

    @GET
    @Path("{sha}")
    @Produces("application/pdf")
    public Response pdf(@PathParam("sha") String sha) {
        final File file = service.fileForSha(sha);
        log.debug("Sending file {}", file.getAbsolutePath());
        return Response.ok(file).header("Content-Length", file.length()).build();
    }
}

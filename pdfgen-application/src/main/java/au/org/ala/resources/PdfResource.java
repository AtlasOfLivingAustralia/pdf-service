package au.org.ala.resources;

import au.org.ala.services.PdfService;
import com.codahale.metrics.annotation.Timed;
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

@Path("pdf")
public class PdfResource {

    private static final Logger log = LoggerFactory.getLogger(PdfResource.class);

    private final HttpClient client;
    private final PdfService service;

    public PdfResource(HttpClient client, PdfService service) {
        this.client = client;
        this.service = service;
    }

    @Timed
    @GET
    public Response generate(@QueryParam("docUrl") String docUrl, @Context UriInfo info) {

        if (docUrl == null) throw new WebApplicationException(400);

        HttpGet get = new HttpGet(docUrl);
        try {
            HttpResponse response = client.execute(get);
            if (response.getStatusLine().getStatusCode() == 200) {
                String hash;
                try (InputStream it = response.getEntity().getContent()) {
                    hash = service.hashAndConvert(it);
                }

                return Response.status(Response.Status.MOVED_PERMANENTLY).location(info.getBaseUriBuilder().path(hash).build()).build();
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

        return Response.status(Response.Status.SEE_OTHER).location(info.getBaseUriBuilder().path(KtPdfResource.class).path(hash).build()).build();
    }

    @GET
    @Path("{sha}")
    public File pdf(@PathParam("sha") String sha) {
        return service.fileForSha(sha);
    }
}

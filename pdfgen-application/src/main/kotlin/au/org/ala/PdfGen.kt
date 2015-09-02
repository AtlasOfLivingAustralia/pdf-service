package au.org.ala

import io.dropwizard.Application
import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.client.JerseyClientBuilder
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.apache.http.client.HttpClient
import org.eclipse.jetty.servlets.CrossOriginFilter
import org.slf4j.LoggerFactory
import au.org.ala.resources.KtPdfResource
import au.org.ala.resources.PdfResource
import au.org.ala.services.PdfService
import io.dropwizard.forms.MultiPartBundle
import java.util.*
import javax.servlet.DispatcherType
import javax.servlet.FilterRegistration
import javax.ws.rs.client.Client
import kotlin.platform.platformStatic
import kotlin.properties.Delegates


import org.eclipse.jetty.servlets.CrossOriginFilter.*
import org.glassfish.jersey.media.multipart.MultiPartFeature
import org.glassfish.jersey.media.multipart.internal.FormDataParamInjectionFeature
import org.glassfish.jersey.media.multipart.internal.MultiPartReaderClientSide
import org.glassfish.jersey.media.multipart.internal.MultiPartReaderServerSide
import org.glassfish.jersey.media.multipart.internal.MultiPartWriter
import java.io.File
import java.io.IOException
import java.nio.file.Files

public class PdfGen : Application<PdfGenConfiguration>() {

    companion object {

        val log = LoggerFactory.getLogger(javaClass<PdfGen>())

        val ALLOWED_ORIGINS = "*"

        val pdfgen = PdfGen()

        //throws(Exception::class)
        platformStatic public fun main(args: Array<String>) {
            pdfgen.run(*args)
        }

        //throws(Exception::class)
        platformStatic public fun stop(args: Array<String>) {
            pdfgen.shutdown(*args)
        }
    }

    var environment: Environment by Delegates.notNull()
    //var dao: DAO by Delegates.notNull()

    override public fun initialize(bootstrap: Bootstrap<PdfGenConfiguration>) {
        log.info("Initialising")
        super.initialize(bootstrap)
        bootstrap.addBundle(MultiPartBundle());
    }

    override fun run(config: PdfGenConfiguration, environment: Environment) {
        this.environment = environment

        val filter = environment.servlets().addFilter("CORSFilter", javaClass<CrossOriginFilter>())

        filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, environment.getApplicationContext().getContextPath() + "*");
        filter.setInitParameter(ALLOWED_METHODS_PARAM, "HEAD,GET,PUT,POST,DELETE,OPTIONS");
        filter.setInitParameter(ALLOWED_ORIGINS_PARAM, ALLOWED_ORIGINS);
        filter.setInitParameter(ALLOWED_HEADERS_PARAM, "Origin, Content-Type, Accept");
        filter.setInitParameter(ALLOW_CREDENTIALS_PARAM, "true");

        val storageDir = ensureStorageDir(config.storageDir)
        log.info("Using ${storageDir.getAbsolutePath()} for PDF storage")

        val httpClient: HttpClient = HttpClientBuilder(environment).using(config.httpClientConfiguration).build("httpClient")
        val service = PdfService(config.sofficePath, storageDir)
        //environment.jersey().register(KtPdfResource(httpClient, service))
        environment.jersey().register(PdfResource(httpClient, service, config.urlCacheSpec))

    }

    private fun ensureStorageDir(storageDir: String): File {
        val file = File(storageDir)
        return if (file.exists()) {
            if (!file.isDirectory()) throw IOException("Storage dir is not a directory: ${file.getAbsolutePath()}")
            if (!file.canWrite()) throw IOException("Storage dir is not writable: ${file.getAbsolutePath()}")
            file
        } else {
            if (!file.mkdirs()) throw IOException("Could not create storage dir: ${file.getAbsolutePath()}")
            file
        }
    }

    fun shutdown(vararg args: String) {
        log.info("Stopping pdfgen dropwizard")
        log.debug("Got args ${args.joinToString(",")}")

        log.info("Stopping admin context")
        environment.getAdminContext().stop()
        log.info("Stopping application context")
        environment.getApplicationContext().stop()
        log.info("Stopping health checks")
        environment.getHealthCheckExecutorService().shutdownNow()
        log.info("Stopping lifecycle managed objects")
        for (lc in environment.lifecycle().getManagedObjects().reverse()) {
            log.info("Stopping ${lc}")
            lc.stop()
        }

        log.info("Quitting VM")
        System.exit(0)
    }
}

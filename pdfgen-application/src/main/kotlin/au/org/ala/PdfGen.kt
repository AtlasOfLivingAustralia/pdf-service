package au.org.ala

import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.dropwizard.Application
import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.client.JerseyClientBuilder
import io.dropwizard.db.DataSourceFactory
import io.dropwizard.flyway.FlywayBundle
import io.dropwizard.flyway.FlywayFactory
import io.dropwizard.java8.Java8Bundle
import io.dropwizard.jdbi.bundles.DBIExceptionsBundle
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.apache.http.client.HttpClient
import org.eclipse.jetty.servlets.CrossOriginFilter
import org.slf4j.LoggerFactory
import au.org.ala.resources.PdfResource
import java.util.*
import javax.servlet.DispatcherType
import javax.servlet.FilterRegistration
import javax.ws.rs.client.Client
import kotlin.platform.platformStatic
import kotlin.properties.Delegates


import org.eclipse.jetty.servlets.CrossOriginFilter.*

public class PdfGen : Application<PdfGenConfiguration>() {

    companion object {

        val log = LoggerFactory.getLogger(PdfGen.javaClass)

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
        super.initialize(bootstrap)
//        bootstrap.getObjectMapper().registerModule(KotlinModule())
//        bootstrap.addBundle(Java8Bundle())
//        bootstrap.addBundle(DBIExceptionsBundle())
//        bootstrap.addBundle(object : FlywayBundle<PdfGenConfiguration>() {
//            override public fun getDataSourceFactory(configuration: PdfGenConfiguration) = configuration.dataSourceFactory
//            override public fun getFlywayFactory(configuration: PdfGenConfiguration) = configuration.flywayFactory
//        });
    }

    override fun run(config: PdfGenConfiguration, environment: Environment) {
        this.environment = environment

        val filter = environment.servlets().addFilter("CORSFilter", javaClass<CrossOriginFilter>())

        filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, environment.getApplicationContext().getContextPath() + "*");
        filter.setInitParameter(ALLOWED_METHODS_PARAM, "GET,PUT,POST,OPTIONS");
        filter.setInitParameter(ALLOWED_ORIGINS_PARAM, ALLOWED_ORIGINS);
        filter.setInitParameter(ALLOWED_HEADERS_PARAM, "Origin, Content-Type, Accept");
        filter.setInitParameter(ALLOW_CREDENTIALS_PARAM, "true");

        val httpClient: HttpClient = HttpClientBuilder(environment).using(config.getHttpClientConfiguration()).build("httpClient")
        environment.jersey().register(PdfResource(httpClient, config.sofficePath, config.storageDir));

    }

    fun shutdown(vararg args: String) {
        log.info("Stopping walter dropwizard")
        log.debug("Got args ${args.joinToString(",")}")
//        log.info("Closing database")
//        dao.close()
//        log.info("Closed database")

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
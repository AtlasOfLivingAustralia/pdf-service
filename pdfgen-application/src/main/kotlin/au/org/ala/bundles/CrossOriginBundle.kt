package au.org.ala.bundles

import io.dropwizard.Bundle
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.eclipse.jetty.servlets.CrossOriginFilter
import java.util.*
import javax.servlet.DispatcherType


class CrossOriginBundle(val urlPattern: String,
                        val allowedMethods: String = DEFAULT_ALLOWED_METHODS,
                        val allowedOrigins: String = DEFAULT_ALLOWED_ORIGINS,
                        val allowedHeaders: String = DEFAULT_ALLOWED_HEADERS,
                        val allowCredentials: String = DEFAULT_ALLOW_CREDENTIALS) : Bundle {
    companion object {
        val DEFAULT_ALLOWED_METHODS = "HEAD,GET,PUT,POST,DELETE,OPTIONS"
        val DEFAULT_ALLOWED_ORIGINS = "*"
        val DEFAULT_ALLOWED_HEADERS = "Origin, Content-Type, Accept"
        val DEFAULT_ALLOW_CREDENTIALS = "true"
    }

    override fun initialize(bootstrap: Bootstrap<*>) {}

    override fun run(environment: Environment) {
        val filter = environment.servlets().addFilter("CORSFilter", CrossOriginFilter::class.java)

        filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, environment.applicationContext.contextPath + urlPattern)
        filter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, allowedMethods)
        filter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, allowedOrigins)
        filter.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, allowedHeaders)
        filter.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, allowCredentials)
    }

}
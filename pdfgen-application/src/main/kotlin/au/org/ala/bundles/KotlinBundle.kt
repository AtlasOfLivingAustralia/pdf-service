package au.org.ala.bundles

import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.dropwizard.Bundle
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment

class KotlinBundle: Bundle {
    override fun initialize(bootstrap: Bootstrap<*>?) { }

    override fun run(environment: Environment) {
        environment.objectMapper.registerModule(KotlinModule())
    }
}
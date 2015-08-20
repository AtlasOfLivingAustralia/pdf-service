package au.org.ala

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Configuration
import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.client.HttpClientConfiguration
import io.dropwizard.client.JerseyClientConfiguration
import io.dropwizard.db.DataSourceFactory
import io.dropwizard.flyway.FlywayFactory
import org.hibernate.validator.constraints.NotEmpty
import javax.validation.Valid
import javax.validation.constraints.NotNull
import kotlin.properties.Delegates

public class PdfGenConfiguration : Configuration() {

    NotEmpty JsonProperty
    public var sofficePath: String = ""

    NotEmpty JsonProperty
    public var storageDir: String = ""

//    NotNull Valid JsonProperty("database")
//    public val dataSourceFactory: DataSourceFactory = DataSourceFactory()
//
//    NotNull Valid JsonProperty("flyway")
//    public val flywayFactory: FlywayFactory = FlywayFactory()

    @Valid
    @NotNull
    private var httpClient: HttpClientConfiguration = HttpClientConfiguration();

    @JsonProperty("httpClient")
    public fun getHttpClientConfiguration(): HttpClientConfiguration {
        return httpClient;
    }
}
package au.org.ala

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Configuration
import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.client.HttpClientConfiguration
import io.dropwizard.client.JerseyClientConfiguration
import org.hibernate.validator.constraints.NotEmpty
import org.hibernate.validator.constraints.Range
import javax.validation.Valid
import javax.validation.constraints.NotNull
import kotlin.properties.Delegates

public class PdfGenConfiguration : Configuration() {

    NotEmpty JsonProperty
    public var unoconvPath: String = ""

    NotEmpty JsonProperty
    public var storageDir: String = ""

    NotEmpty JsonProperty
    public var urlCacheSpec: String = "expireAfterAccess=7d"

    NotEmpty JsonProperty
    public var sofficeHost: String = "localhost"

    Range(min=1, max=65535) JsonProperty
    public var sofficePort: Int = 2220

    Valid NotNull JsonProperty("httpClient")
    public val httpClientConfiguration: HttpClientConfiguration = HttpClientConfiguration();

}
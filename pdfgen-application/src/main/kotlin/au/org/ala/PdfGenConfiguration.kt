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

class PdfGenConfiguration : Configuration() {

    @NotEmpty @JsonProperty var sofficePath: String = ""

    @NotEmpty @JsonProperty var htmltopdfPath: String = ""

    @NotEmpty @JsonProperty var storageDir: String = ""

    @NotEmpty @JsonProperty var urlCacheSpec: String = "expireAfterAccess=7d"

    @Valid @NotNull @JsonProperty("httpClient") val httpClientConfiguration: HttpClientConfiguration = HttpClientConfiguration()

}
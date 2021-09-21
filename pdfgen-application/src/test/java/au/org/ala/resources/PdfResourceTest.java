package au.org.ala.resources;

import au.org.ala.services.PdfService;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.ws.rs.WebApplicationException;
import java.io.File;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PdfResourceTest {

    private PdfService pdfService;
    private HttpClient httpClient;
    private HttpGet httpGet;
    private HttpResponse httpResponse;
    private StatusLine statusLine;

    @BeforeEach
    void setup() {
        pdfService = new PdfService("", "", new File(System.getProperty("java.io.tmpdir")));
        httpClient = mock(HttpClient.class);

        httpGet = mock(HttpGet.class);
        httpResponse = mock(HttpResponse.class);
        statusLine = mock(StatusLine.class);
    }

    @ParameterizedTest
    @CsvSource({
            "https://www.ala.org.au, true",
            "http://devt.ala.org.au, true",
            "https://www.google.com, false",
            ", false",
            "not a url, false"
    })
    public void theSuppliedUrlIsValidated(String docUrl, String expectedValidationPassed) throws Exception {
        PdfResource pdfResource = new PdfResource(httpClient, pdfService, "", ".*\\.ala\\.org\\.au");


        when(httpClient.execute((HttpGet)any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_GATEWAY_TIMEOUT);

        try {
            pdfResource.generate(docUrl, null, null, null);
        }
        catch (WebApplicationException e) {
            // This will always be thrown as if the validation passes, the return code in the stub will
            // cause an exception to be thrown anyway.
        }

        if (Boolean.valueOf(expectedValidationPassed)) {
            // If the validation passes, we expect the URL to be used.
            verify(httpClient).execute((HttpGet)any());
        }
        else {
            // If the validation fails we should have exited immeditately.
            verifyNoInteractions(httpClient);
        }

    }
}

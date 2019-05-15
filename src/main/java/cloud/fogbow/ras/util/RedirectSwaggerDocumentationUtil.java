package cloud.fogbow.ras.util;

import cloud.fogbow.common.constants.Messages;
import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import org.apache.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;

public class RedirectSwaggerDocumentationUtil {

    private static final Logger LOGGER = Logger.getLogger(RedirectSwaggerDocumentationUtil.class);

    public static <T> ResponseEntity<T> redirectRequest(String body, HttpMethod method, HttpServletRequest request,
                                     Class<T> responseType) throws FatalErrorException, FogbowException {
        int servicePort = request.getLocalPort();

        URI uri = null;
        try {
            uri = new URI("http://localhost");
        } catch (URISyntaxException e) {
            throw new ConfigurationErrorException(String.format(Messages.Exception.UNEXPECTED));
        }
        uri = UriComponentsBuilder.fromUri(uri).port(servicePort).path("swagger-ui.html")
                .query(request.getQueryString()).build(true).toUri();

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.set(headerName, request.getHeader(headerName));
        }

        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new NoOpErrorHandler());
        try {
            ResponseEntity<T> response = restTemplate.exchange(uri, method, httpEntity, responseType);
            return response;
        } catch (Exception e) {
            LOGGER.info(String.format(Messages.Exception.WRONG_SYNTAX_FOR_ENDPOINT_S, e.getMessage()));
            throw e;
        }
    }

    private static class NoOpErrorHandler implements ResponseErrorHandler {

        @Override
        public boolean hasError(ClientHttpResponse clientHttpResponse) throws IOException {
            return clientHttpResponse.getRawStatusCode() >= 300;
        }

        @Override
        public void handleError(ClientHttpResponse clientHttpResponse) {
        }
    }
}

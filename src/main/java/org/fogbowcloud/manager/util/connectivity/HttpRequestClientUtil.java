package org.fogbowcloud.manager.util.connectivity;

import java.nio.charset.StandardCharsets;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.plugins.cloud.models.ErrorType;
import org.fogbowcloud.manager.core.plugins.cloud.models.RequestHeaders;
import org.fogbowcloud.manager.core.plugins.cloud.models.ResponseConstants;
import org.fogbowcloud.manager.core.plugins.cloud.models.ErrorResponse;
import org.fogbowcloud.manager.core.plugins.cloud.models.ErrorResponseMap;
import org.fogbowcloud.manager.core.models.tokens.Token;

public class HttpRequestClientUtil {
	
	private static final Logger LOGGER = Logger.getLogger(HttpRequestClientUtil.class);
	private HttpClient client;
	
	public HttpRequestClientUtil() throws FatalErrorException {
        HttpRequestUtil.init();
        this.client = HttpRequestUtil.createHttpClient();
	}
	
    public String doGetRequest(String endpoint, Token localToken) throws FogbowManagerException {
        LOGGER.debug("Doing GET request to endpoint <" + endpoint + ">");

        HttpResponse response = null;
        String responseStr;
        try {
            HttpGet request = new HttpGet(endpoint);
            
            request.addHeader(RequestHeaders.CONTENT_TYPE.getValue(), RequestHeaders.JSON_CONTENT_TYPE.getValue());
            request.addHeader(RequestHeaders.ACCEPT.getValue(), RequestHeaders.JSON_CONTENT_TYPE.getValue());
            request.addHeader(RequestHeaders.X_AUTH_TOKEN.getValue(), localToken.getAccessId());
            
            response = this.client.execute(request);
            responseStr = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error("Could not make GET request.", e);
            throw new FogbowManagerException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
        } finally {
            try {
                EntityUtils.consume(response.getEntity());
            } catch (Throwable t) {
                LOGGER.error("Error while consuming the response: " + t);
            }
        }
        checkStatusResponse(response, responseStr);
        return responseStr;
    }
    
    private void checkStatusResponse(HttpResponse response, String message) throws FogbowManagerException {
        LOGGER.debug("Checking status response...");

        ErrorResponseMap errorResponseMap = new ErrorResponseMap(response, message);
        Integer statusCode = response.getStatusLine().getStatusCode();
        ErrorResponse errorResponse = errorResponseMap.getStatusResponse(statusCode);

        if (errorResponse != null) {
            throw new FogbowManagerException(errorResponse.getErrorType(), errorResponse.getResponseConstants());
        }
    }
	
}

package org.fogbowcloud.manager.core.manager.plugins.cloud.quota.compute;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.QuotaException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.manager.constants.OpenStackConfigurationConstants;
import org.fogbowcloud.manager.core.manager.plugins.cloud.quota.ComputeQuotaPlugin;
import org.fogbowcloud.manager.core.models.ErrorType;
import org.fogbowcloud.manager.core.models.RequestHeaders;
import org.fogbowcloud.manager.core.models.ResponseConstants;
import org.fogbowcloud.manager.core.models.StatusResponse;
import org.fogbowcloud.manager.core.models.StatusResponseMap;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.utils.HttpRequestUtil;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenStackComputeQuotaPlugin implements ComputeQuotaPlugin {
	
	public static final String MAX_TOTAL_CORES = "maxTotalCores";
	public static final String TOTAL_CORES_USED = "totalCoresUsed";
	public static final String MAX_TOTAL_RAM_SIZE = "maxTotalRAMSize";
	public static final String TOTAL_RAM_USED = "totalRAMUsed";
	public static final String MAX_TOTAL_INSTANCES = "maxTotalInstances";
	public static final String TOTAL_INSTANCES_USED = "totalInstancesUsed";
	
	private static final String SUFFIX = "limits";
	private static final String TENANT_ID = "tenantId";
	private static final String COMPUTE_V2_API_ENDPOINT = "/v2/";
	
	private static final Logger LOGGER = Logger.getLogger(OpenStackComputeQuotaPlugin.class);
	
	private HttpClient client;
	
	private Properties properties;
	
	public OpenStackComputeQuotaPlugin(Properties properties) {
		this.properties = properties;
		initClient();
	}
	
	@Override
	public ComputeQuota getComputeQuota(Token localToken) throws QuotaException {
		
		String jsonLimits = getLimitsJson(localToken);
		
//		new ComputeQuotaInfo(
//				getAttFromLimitsJson(TOTAL_CORES_USED, jsonLimits), 
//				getAttFromLimitsJson(TOTAL_RAM_USED, jsonLimits),
//				getAttFromLimitsJson(TOTAL_INSTANCES_USED, jsonLimits));
		return null; 
	}

	String getLimitsJson(Token localToken) throws QuotaException {
		String endpoint = 
				this.properties.getProperty(OpenStackConfigurationConstants.COMPUTE_NOVAV2_URL_KEY)
                + COMPUTE_V2_API_ENDPOINT
                + localToken.getAttributes().get(TENANT_ID)
                + SUFFIX;
		try {
			String jsonResponse = doGetRequest(endpoint, localToken);
			return jsonResponse;
		} catch (RequestException e) {
			throw new QuotaException("Could not make GET request.", e);
		}
	}
	
    protected String doGetRequest(String endpoint, Token localToken) throws RequestException {
        LOGGER.debug("Doing GET request to OpenStack on endpoint <" + endpoint + ">");

        HttpResponse response = null;
        String responseStr;

        try {
            HttpGet request = new HttpGet(endpoint);
            request.addHeader(
                    RequestHeaders.CONTENT_TYPE.getValue(),
                    RequestHeaders.JSON_CONTENT_TYPE.getValue());
            request.addHeader(
                    RequestHeaders.ACCEPT.getValue(), RequestHeaders.JSON_CONTENT_TYPE.getValue());
            request.addHeader(RequestHeaders.X_AUTH_TOKEN.getValue(), localToken.getAccessId());

            response = this.client.execute(request);
            responseStr = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error("Could not make GET request.", e);
            throw new RequestException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
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
    
    private void checkStatusResponse(HttpResponse response, String message)
            throws RequestException {
        LOGGER.debug("Checking status response...");

        StatusResponseMap statusResponseMap = new StatusResponseMap(response, message);
        Integer statusCode = response.getStatusLine().getStatusCode();
        StatusResponse statusResponse = statusResponseMap.getStatusResponse(statusCode);

        if (statusResponse != null) {
            throw new RequestException(
                    statusResponse.getErrorType(), statusResponse.getResponseConstants());
        }
    }
    
    private void initClient() {
        HttpRequestUtil.init(this.properties);
        this.client = HttpRequestUtil.createHttpClient();
    }
    
	private int getAttFromLimitsJson(String attName, String responseStr) 
			throws QuotaException {
		try {
			JSONObject jsonObject = new JSONObject(responseStr);
			return jsonObject.getJSONObject("limits").getJSONObject("absolute").getInt(attName);
		} catch (JSONException e) {
			throw new QuotaException("Error while reading Json", e);
		}
	}
	
}

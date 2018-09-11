package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineResponse;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;

import java.io.File;
import java.util.Map;
import java.util.Properties;

public class CloudStackTokenGenerator implements TokenGeneratorPlugin {
    private static final Logger LOGGER = Logger.getLogger(CloudStackTokenGenerator.class);

    public static final String API_KEY = "apiKey";
    public static final String SECRET_KEY = "secretKey";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String DOMAIN = "secretKey";
    public static final String CLOUDSTACK_URL = "cloudstack_api_url";
    public static final String TOKEN_VALUE_SEPARATOR = "!#!";

    private String endpoint;
    private String tokenProviderId;
    private HttpRequestClientUtil client;

    public CloudStackTokenGenerator() {
        this.tokenProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        Properties properties = PropertiesUtil.readProperties(HomeDir.getPath() + File.separator
                + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME);

        this.endpoint = properties.getProperty(CLOUDSTACK_URL);
        this.client = new HttpRequestClientUtil();
    }

    @Override
    public String createTokenValue(Map<String, String> credentials) throws FogbowRasException {
        if ((credentials == null) || (credentials.get(USERNAME) == null) || (credentials.get(PASSWORD) == null) ||
             credentials.get(DOMAIN) == null) {
            String errorMsg = "User credentials can't be null";
            throw new InvalidParameterException(errorMsg);
        }

        LoginRequest request = createLoginRequest(credentials);
        HttpRequestClientUtil.Response jsonResponse = null;
        try {
            // NOTE(pauloewerton): since all cloudstack requests params are passed via url args, we do not need to
            // send a valid json body in the post request
            jsonResponse = this.client.doPostRequest(request.getUriBuilder().toString(), null);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }

        LoginResponse response = LoginResponse.fromJson(jsonResponse.getContent());
        String tokenValue = getTokenValue(response.getSessionKey());

        return tokenValue;
    }

    private LoginRequest createLoginRequest(Map<String, String> credentials) throws InvalidParameterException {
        String userId = credentials.get(USERNAME);
        String password = credentials.get(PASSWORD);
        String domain = credentials.get(DOMAIN);

        LoginRequest loginRequest = new LoginRequest.Builder()
                .username(userId)
                .password(password)
                .domain(domain)
                .build();

        return loginRequest;
    }

    private String getTokenValue(String sessionKey) throws FogbowRasException {
        ListAccountsRequest request = new ListAccountsRequest.Builder()
                .sessionKey(sessionKey)
                .build();

        String jsonResponse = null;
        try {
            // NOTE(pauloewerton): no need to pass a token
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), null);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }

        ListAccountsResponse response = ListAccountsResponse.fromJson(jsonResponse);

        return null;
    }

    private ListAccountsRequest createListAccountsRequest(String sessionKey) {
        String password = credentials.get(PASSWORD);
        String domain = credentials.get(DOMAIN);

        LoginRequest loginRequest = new LoginRequest.Builder()
                .username(userId)
                .password(password)
                .domain(domain)
                .build();
    }
}

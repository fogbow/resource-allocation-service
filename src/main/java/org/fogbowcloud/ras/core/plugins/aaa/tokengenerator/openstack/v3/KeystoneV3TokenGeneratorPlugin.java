package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.openstack.v3;

import org.apache.http.Header;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;

import java.util.Map;
import java.util.Properties;

public class KeystoneV3TokenGeneratorPlugin implements TokenGeneratorPlugin {
    private static final Logger LOGGER = Logger.getLogger(KeystoneV3TokenGeneratorPlugin.class);

    public static final String OPENSTACK_KEYSTONE_V3_URL = "openstack_keystone_v3_url";
    public static final String V3_TOKENS_ENDPOINT_PATH = "/auth/tokens";
    public static final String X_SUBJECT_TOKEN = "X-Subject-Token";
    public static final String TOKEN_VALUE_SEPARATOR = "!#!";
    public static final String PROJECT_ID = "projectId";
    public static final String PASSWORD = "password";
    public static final String USER_ID = "userId";
//    public static final String AUTH_URL = "authUrl";
    private String v3TokensEndpoint;
    private HttpRequestClientUtil client;
    private String tokenProviderId;

    public KeystoneV3TokenGeneratorPlugin() throws FatalErrorException {
        this.tokenProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        Properties properties = PropertiesUtil.readProperties(HomeDir.getPath() +
                DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);

        String identityUrl = properties.getProperty(OPENSTACK_KEYSTONE_V3_URL);
        if (isUrlValid(identityUrl)) {
            this.v3TokensEndpoint = identityUrl + V3_TOKENS_ENDPOINT_PATH;
        }
        this.client = new HttpRequestClientUtil();
    }

    private boolean isUrlValid(String url) throws FatalErrorException {
        if (url == null || url.trim().isEmpty()) {
            throw new FatalErrorException("Invalid Keystone_V3_URL " + OPENSTACK_KEYSTONE_V3_URL);
        }
        return true;
    }

    @Override
    public String createTokenValue(Map<String, String> credentials) throws FogbowRasException,
            UnexpectedException {

        String jsonBody = mountJsonBody(credentials);
//        String authUrl = credentials.get(AUTH_URL);
//        String currentTokenEndpoint = this.v3TokensEndpoint;
//        if (authUrl != null && !authUrl.isEmpty()) {
//            currentTokenEndpoint = authUrl + V3_TOKENS_ENDPOINT_PATH;
//        }

        HttpRequestClientUtil.Response response = null;
        try {
//            response = this.client.doPostRequest(currentTokenEndpoint, jsonBody);
            response = this.client.doPostRequest(this.v3TokensEndpoint, jsonBody);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }
        String tokenString = getTokenFromJson(response);
        return tokenString;
    }

    private String getTokenFromJson(HttpRequestClientUtil.Response response) throws UnexpectedException {

        String tokenValue = null;
        Header[] headers = response.getHeaders();
        for (Header header : headers) {
            if (header.getName().equals(X_SUBJECT_TOKEN)) {
                tokenValue = header.getValue();
            }
        }

        try {
            CreateTokenResponse createTokenResponse = CreateTokenResponse.fromJson(response.getContent());
            CreateTokenResponse.User userTokenResponse = createTokenResponse.getUser();
            String userId = userTokenResponse.getId();
            String userName = userTokenResponse.getName();

            CreateTokenResponse.Project projectTokenResponse = createTokenResponse.getProject();
            String projectId = projectTokenResponse.getId();
            String projectName = projectTokenResponse.getName();

            String tokenString = this.tokenProviderId + TOKEN_VALUE_SEPARATOR + tokenValue + TOKEN_VALUE_SEPARATOR +
                    userId + TOKEN_VALUE_SEPARATOR + userName + TOKEN_VALUE_SEPARATOR + projectId +
                    TOKEN_VALUE_SEPARATOR + projectName;

            return tokenString;
        } catch (Exception e) {
            LOGGER.error("Exception while getting tokens from json", e);
            throw new UnexpectedException("Exception while getting tokens from json", e);
        }
    }

    private String mountJsonBody(Map<String, String> credentials) {
        String projectId = credentials.get(PROJECT_ID);
        String userId = credentials.get(USER_ID);
        String password = credentials.get(PASSWORD);

        CreateTokenRequest createTokenRequest = new CreateTokenRequest.Builder()
                .projectId(projectId)
                .userId(userId)
                .password(password)
                .build();

        return createTokenRequest.toJson();
    }

    // Used in testing
    public void setClient(HttpRequestClientUtil client) {
        this.client = client;
    }
}

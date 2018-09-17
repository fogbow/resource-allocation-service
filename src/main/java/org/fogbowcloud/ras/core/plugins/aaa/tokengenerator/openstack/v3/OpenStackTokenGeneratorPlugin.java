package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.openstack.v3;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.RSAUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;
import java.util.Map;
import java.util.Properties;

public class OpenStackTokenGeneratorPlugin implements TokenGeneratorPlugin {
    private static final Logger LOGGER = Logger.getLogger(OpenStackTokenGeneratorPlugin.class);

    public static final String OPENSTACK_KEYSTONE_V3_URL = "openstack_keystone_v3_url";
    public static final String V3_TOKENS_ENDPOINT_PATH = "/auth/tokens";
    public static final String X_SUBJECT_TOKEN = "X-Subject-Token";
    public static final String TOKEN_VALUE_SEPARATOR = "!#!";
    public static final String PROJECT_ID = "projectName";
    public static final String PASSWORD = "password";
    public static final String USER_ID = "userId";

    private String v3TokensEndpoint;
    private HttpRequestClientUtil client;
    private String tokenProviderId;
	private RSAPrivateKey privateKey;

    public OpenStackTokenGeneratorPlugin() throws FatalErrorException {
        this.tokenProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        Properties properties = PropertiesUtil.readProperties(HomeDir.getPath() +
                DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);

        String identityUrl = properties.getProperty(OPENSTACK_KEYSTONE_V3_URL);
        if (isUrlValid(identityUrl)) {
            this.v3TokensEndpoint = identityUrl + V3_TOKENS_ENDPOINT_PATH;
        }
        
        try {
            this.privateKey = RSAUtil.getPrivateKey();
        } catch (IOException | GeneralSecurityException e) {
            throw new FatalErrorException(String.format(Messages.Fatal.ERROR_READING_PRIVATE_KEY_FILE, e.getMessage()));
        }        
        
        this.client = new HttpRequestClientUtil();
    }

    private boolean isUrlValid(String url) throws FatalErrorException {
        if (url == null || url.trim().isEmpty()) {
            throw new FatalErrorException(String.format(Messages.Fatal.INVALID_SERVICE_URL, OPENSTACK_KEYSTONE_V3_URL));
        }
        return true;
    }

    @Override
    public String createTokenValue(Map<String, String> credentials) throws FogbowRasException,
            UnexpectedException {

        String jsonBody = mountJsonBody(credentials);

        HttpRequestClientUtil.Response response = null;
        try {
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

            String tokenString = this.tokenProviderId + TOKEN_VALUE_SEPARATOR + tokenValue + TOKEN_VALUE_SEPARATOR +
                    userId + TOKEN_VALUE_SEPARATOR + userName + TOKEN_VALUE_SEPARATOR + projectId;
            
            String signature = createSignature(tokenString);
            return tokenString + TOKEN_VALUE_SEPARATOR + signature;
        } catch (Exception e) {
            LOGGER.error(Messages.Error.UNABLE_TO_GET_TOKEN_FROM_JSON, e);
            throw new UnexpectedException(Messages.Error.UNABLE_TO_GET_TOKEN_FROM_JSON, e);
        }
    }

    private String createSignature(String message) throws IOException, GeneralSecurityException {
        return RSAUtil.sign(this.privateKey, message);
    }
    
    private String mountJsonBody(Map<String, String> credentials) {
        String projectId = credentials.get(PROJECT_ID);
        String userId = credentials.get(USER_ID);
        String password = credentials.get(PASSWORD);

        CreateTokenRequest createTokenRequest = new CreateTokenRequest.Builder()
                .projectName(projectId)
                .password(password)
                .build();

        return createTokenRequest.toJson();
    }

    // Used in testing
    public void setClient(HttpRequestClientUtil client) {
        this.client = client;
    }
}

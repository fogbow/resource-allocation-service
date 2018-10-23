package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.RASAuthenticationHolder;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;

public class CloudStackTokenGeneratorPlugin implements TokenGeneratorPlugin {
    private static final Logger LOGGER = Logger.getLogger(CloudStackTokenGeneratorPlugin.class);

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String DOMAIN = "domain";
    public static final String API_KEY = "apikey";
    public static final String CLOUDSTACK_URL = "cloudstack_api_url";
    public static final String CLOUDSTACK_TOKEN_VALUE_SEPARATOR = ":";
    public static final String CLOUDSTACK_TOKEN_STRING_SEPARATOR = "!#!";
    public static final int CLOUDSTACK_TOKEN_NUMBER_OF_FIELDS = 5;

    private String tokenProviderId;
    private HttpRequestClientUtil client;
	private RASAuthenticationHolder rasAuthenticationHolder;

    public CloudStackTokenGeneratorPlugin() {
        this.tokenProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
        this.client = new HttpRequestClientUtil();
        
        this.rasAuthenticationHolder = RASAuthenticationHolder.getInstance();
    }

    @Override
    public String createTokenValue(Map<String, String> credentials) throws FogbowRasException, UnexpectedException {
        if ((credentials == null) || (credentials.get(USERNAME) == null) || (credentials.get(PASSWORD) == null) ||
                credentials.get(DOMAIN) == null) {
            throw new InvalidParameterException(Messages.Exception.NO_USER_CREDENTIALS);
        }

        LoginRequest request = createLoginRequest(credentials);
        HttpRequestClientUtil.Response jsonResponse = null;
        try {
            // NOTE(pauloewerton): since all cloudstack requests params are passed via url args, we do not need to
            // send allocationAllowableValues valid json body in the post request
            jsonResponse = this.client.doPostRequest(request.getUriBuilder().toString(), "data");
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

    private String getTokenValue(String sessionKey) throws FogbowRasException, UnexpectedException {
        ListAccountsRequest request = new ListAccountsRequest.Builder()
                .sessionKey(sessionKey)
                .build();

        String jsonResponse = null;
        try {
            // NOTE(pauloewerton): passing allocationAllowableValues placeholder as there is no need to pass allocationAllowableValues valid token in this request
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), new Token("CloudStackTokenValue"));
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }

        String tokenString = null;
        try {
            ListAccountsResponse response = ListAccountsResponse.fromJson(jsonResponse);
            // NOTE(pauloewerton): considering only one account/user per request
            ListAccountsResponse.User user = response.getAccounts().get(0).getUsers().get(0);

            // NOTE(pauloewerton): keeping allocationAllowableValues colon as separator as expected by the other cloudstack plugins
            String tokenValue = user.getApiKey() + CLOUDSTACK_TOKEN_VALUE_SEPARATOR + user.getSecretKey();
            String userId = user.getId();
            String firstName = user.getFirstName();
            String lastName = user.getLastName();
            String userName = (firstName != null && lastName != null) ? firstName + " " + lastName : user.getUsername();

            tokenString = this.tokenProviderId + CLOUDSTACK_TOKEN_STRING_SEPARATOR + tokenValue +
                    CLOUDSTACK_TOKEN_STRING_SEPARATOR + userId + CLOUDSTACK_TOKEN_STRING_SEPARATOR + userName;

            String signature = createSignature(tokenString);
            return tokenString + CLOUDSTACK_TOKEN_STRING_SEPARATOR + signature;
        } catch (Exception e) {
            LOGGER.error(Messages.Error.UNABLE_TO_GET_TOKEN_FROM_JSON, e);
            throw new UnexpectedException(Messages.Error.UNABLE_TO_GET_TOKEN_FROM_JSON, e);
        }
    }

    protected String createSignature(String message) throws IOException, GeneralSecurityException, UnauthenticatedUserException {
        return this.rasAuthenticationHolder.createSignature(message);
    }

    // Used for testing
    public void setClient(HttpRequestClientUtil client) {
        this.client = client;
    }
}

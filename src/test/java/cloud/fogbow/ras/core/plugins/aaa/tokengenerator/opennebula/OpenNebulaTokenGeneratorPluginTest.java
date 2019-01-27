package cloud.fogbow.ras.core.plugins.aaa.tokengenerator.opennebula;

import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.constants.ConfigurationConstants;
import cloud.fogbow.ras.core.constants.SystemConstants;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.user.UserPool;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class OpenNebulaTokenGeneratorPluginTest {

    private static final String CLOUD_NAME = "opennebula";
    private String openenbulaConfFilePath;

    private OpenNebulaTokenGeneratorPlugin openNenbulaTokenGenerator;
    private OpenNebulaClientFactory factory;
    private String memberId;

    @Before
    public void setUp() {
        this.openenbulaConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME +
                File.separator + CLOUD_NAME + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.openNenbulaTokenGenerator = Mockito.spy(new OpenNebulaTokenGeneratorPlugin(openenbulaConfFilePath));

        this.factory = Mockito.mock(OpenNebulaClientFactory.class);
        this.openNenbulaTokenGenerator.setFactory(this.factory);

        this.memberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
    }

    // test case: success case
    @Test
    public void testCreateTokenValue() throws FogbowRasException {
        // set up
        String usernameExpected = "username";
        String userIdExpected = usernameExpected;
        String passwordExpected = "password";
        Map<String, String> credentials = new HashMap<>();
        credentials.put(OpenNebulaTokenGeneratorPlugin.USERNAME, usernameExpected);
        credentials.put(OpenNebulaTokenGeneratorPlugin.PASSWORD, passwordExpected);

        String openNebulaTokenValue = usernameExpected + OpenNebulaTokenGeneratorPlugin.OPENNEBULA_TOKEN_VALUE_SEPARATOR + passwordExpected;;
        Mockito.doReturn(Boolean.TRUE)
                .when(this.openNenbulaTokenGenerator).isAuthenticated(Mockito.eq(openNebulaTokenValue));

        // exercise
        String tokenValue = this.openNenbulaTokenGenerator.createTokenValue(credentials);

        // verify
        String[] tokenValueSlices = tokenValue.split(OpenNebulaTokenGeneratorPlugin.OPENNEBULA_FIELD_SEPARATOR);
        Assert.assertEquals(OpenNebulaTokenGeneratorPlugin.FEDERATION_TOKEN_PARAMETER_SIZE, tokenValueSlices.length);
        Assert.assertEquals(usernameExpected, tokenValueSlices[OpenNebulaTokenGeneratorPlugin.USERNAME_TOKEN_VALUE_PARAMETER]);
        Assert.assertEquals(userIdExpected, tokenValueSlices[OpenNebulaTokenGeneratorPlugin.USER_ID_TOKEN_VALUE_PARAMETER]);
        Assert.assertEquals(openNebulaTokenValue, tokenValueSlices[OpenNebulaTokenGeneratorPlugin.ONE_TOKEN_VALUE_PARAMETER]);
        Assert.assertEquals(this.memberId, tokenValueSlices[OpenNebulaTokenGeneratorPlugin.PROVIDER_ID_TOKEN_VALUE_PARAMETER]);
        Assert.assertNotNull(tokenValueSlices[OpenNebulaTokenGeneratorPlugin.SIGNATURE_TOKEN_VALUE_PARAMETER]);
    }

    // test case: throw exception when is not authenticated
    @Test(expected = UnauthenticatedUserException.class)
    public void testCreateTokenValueUnauthenticated() throws FogbowRasException {
        // set up
        String usernameExpected = "wrong";
        String passwordExpected = "wrong";
        Map<String, String> credentials = new HashMap<>();
        credentials.put(OpenNebulaTokenGeneratorPlugin.USERNAME, usernameExpected);
        credentials.put(OpenNebulaTokenGeneratorPlugin.PASSWORD, passwordExpected);

        String openNebulaTokenValue = usernameExpected + OpenNebulaTokenGeneratorPlugin.OPENNEBULA_TOKEN_VALUE_SEPARATOR + passwordExpected;;
        Mockito.doReturn(Boolean.FALSE)
                .when(this.openNenbulaTokenGenerator).isAuthenticated(Mockito.eq(openNebulaTokenValue));

        // exercise
        this.openNenbulaTokenGenerator.createTokenValue(credentials);
    }

    // test case: is authenticated
    @Test
    public void testIsAuthenticated() throws UnexpectedException {
        // set up
        String openNebulaTokenValue = "anything";
        Client client = Mockito.mock(Client.class);
        Mockito.when(this.factory.createClient(Mockito.eq(openNebulaTokenValue))).thenReturn(client);
        UserPool userPool = Mockito.mock(UserPool.class);
        Mockito.when(this.factory.createUserPool(Mockito.eq(client))).thenReturn(userPool);
        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.when(userPool.info()).thenReturn(oneResponse);
        Mockito.when(oneResponse.isError()).thenReturn(false);

        // exercise
        boolean isAuthenticated = this.openNenbulaTokenGenerator.isAuthenticated(openNebulaTokenValue);
        // verify
        Assert.assertTrue(isAuthenticated);
    }

    // test case: is not authenticated because the Opennebula returned an error
    @Test
    public void testIsAuthenticatedOpennebulaError() throws UnexpectedException {
        // set up
        String openNebulaTokenValue = "anything";
        Client client = Mockito.mock(Client.class);
        Mockito.when(this.factory.createClient(Mockito.eq(openNebulaTokenValue))).thenReturn(client);
        UserPool userPool = Mockito.mock(UserPool.class);
        Mockito.when(this.factory.createUserPool(Mockito.eq(client))).thenReturn(userPool);
        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.when(userPool.info()).thenReturn(oneResponse);
        Mockito.when(oneResponse.isError()).thenReturn(true);

        // exercise
        boolean isAuthenticated = this.openNenbulaTokenGenerator.isAuthenticated(openNebulaTokenValue);
        // verify
        Assert.assertFalse(isAuthenticated);
        Mockito.verify(oneResponse, Mockito.times(1)).isError();
    }

    // test case: is not authenticated because the Opennebula client was created wrong
    @Test
    public void testIsAuthenticatedClientCreationError() throws UnexpectedException {
        // set up
        String openNebulaTokenValue = "anything";
        Client client = Mockito.mock(Client.class);
        Mockito.when(this.factory.createClient(Mockito.eq(openNebulaTokenValue))).thenThrow(UnexpectedException.class);
        UserPool userPool = Mockito.mock(UserPool.class);
        Mockito.when(this.factory.createUserPool(Mockito.eq(client))).thenReturn(userPool);
        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.when(userPool.info()).thenReturn(oneResponse);

        // exercise
        boolean isAuthenticated = this.openNenbulaTokenGenerator.isAuthenticated(openNebulaTokenValue);
        // verify
        Assert.assertFalse(isAuthenticated);
        Mockito.verify(oneResponse, Mockito.never()).isError();
    }

}

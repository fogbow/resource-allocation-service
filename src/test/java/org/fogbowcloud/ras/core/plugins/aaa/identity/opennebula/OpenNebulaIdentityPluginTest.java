package org.fogbowcloud.ras.core.plugins.aaa.identity.opennebula;

import org.apache.commons.lang.StringUtils;
import org.fogbowcloud.ras.core.exceptions.InvalidTokenException;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.opennebula.OpenNebulaTokenGeneratorPlugin;
import org.junit.Assert;
import org.junit.Test;

public class OpenNebulaIdentityPluginTest {

    // test case: success case
    @Test
    public void testCreateToken() throws InvalidTokenException {
        // set up
        OpenNebulaIdentityPlugin openNebulaIdentityPlugin = new OpenNebulaIdentityPlugin();

        String[] federationTokenParameters = new String[OpenNebulaTokenGeneratorPlugin.FEDERATION_TOKEN_PARAMETER_SIZE];
        String providerExpected = "provider";
        String openNebulaTokenValueExpected = "openNebulaTokenValue";
        String usernameExpected = "username";
        String userIdExpected = "userid";
        String signatureExpected = "signature";
        federationTokenParameters[OpenNebulaTokenGeneratorPlugin.PROVIDER_ID_TOKEN_VALUE_PARAMETER] = providerExpected;
        federationTokenParameters[OpenNebulaTokenGeneratorPlugin.ONE_TOKEN_VALUE_PARAMETER] = openNebulaTokenValueExpected;
        federationTokenParameters[OpenNebulaTokenGeneratorPlugin.USERNAME_TOKEN_VALUE_PARAMETER] = usernameExpected;
        federationTokenParameters[OpenNebulaTokenGeneratorPlugin.USER_ID_TOKEN_VALUE_PARAMETER] = userIdExpected;
        federationTokenParameters[OpenNebulaTokenGeneratorPlugin.SIGNATURE_TOKEN_VALUE_PARAMETER] = signatureExpected;
        String tokenValue = StringUtils.join(federationTokenParameters, OpenNebulaTokenGeneratorPlugin.OPENNEBULA_FIELD_SEPARATOR);

        //exercise
        OpenNebulaToken opennebula = openNebulaIdentityPlugin.createToken(tokenValue);

        //verify
        Assert.assertEquals(providerExpected, opennebula.getTokenProvider());
        Assert.assertEquals(usernameExpected, opennebula.getUserName());
        Assert.assertEquals(userIdExpected, opennebula.getUserId());
        Assert.assertEquals(signatureExpected, opennebula.getSignature());
        Assert.assertEquals(openNebulaTokenValueExpected, opennebula.getTokenValue());
    }

    // test case: token value is null
    @Test(expected = InvalidTokenException.class)
    public void testCreateTokenNullTokenValue() throws InvalidTokenException {
        // set up
        OpenNebulaIdentityPlugin openNebulaIdentityPlugin = new OpenNebulaIdentityPlugin();
        // exercise
        openNebulaIdentityPlugin.createToken(null);
    }

    // test case: token value is null
    @Test(expected = InvalidTokenException.class)
    public void testCreateTokenInvalidFormatTokenValue() throws InvalidTokenException {
        // set up
        OpenNebulaIdentityPlugin openNebulaIdentityPlugin = new OpenNebulaIdentityPlugin();
        String tokenValue = "anything";

        // exercise
        openNebulaIdentityPlugin.createToken(tokenValue);
    }

}

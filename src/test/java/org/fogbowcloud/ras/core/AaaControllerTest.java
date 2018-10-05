package org.fogbowcloud.ras.core;

import org.fogbowcloud.ras.core.constants.Operation;
import org.fogbowcloud.ras.core.exceptions.*;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.AuthenticationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.authorization.AuthorizationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.identity.FederationIdentityPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.mapper.FederationToLocalMapperPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

public class AaaControllerTest {

    private AaaController aaaController;
    private AaaPluginsHolder aaaPluginsHolderMock;
    private AuthenticationPlugin authenticationPluginMock;
    private AuthorizationPlugin authorizationPluginMock;
    private FederationToLocalMapperPlugin federationToLocalMapperPluginMock;
    private FederationIdentityPlugin federationIdentityPluginMock;

    @Before
    public void setUp() {
        this.aaaPluginsHolderMock = Mockito.mock(AaaPluginsHolder.class);
        this.authenticationPluginMock = Mockito.mock(AuthenticationPlugin.class);
        this.authorizationPluginMock = Mockito.mock(AuthorizationPlugin.class);
        this.federationToLocalMapperPluginMock = Mockito.mock(FederationToLocalMapperPlugin.class);
        this.federationIdentityPluginMock = Mockito.mock(FederationIdentityPlugin.class);

        Mockito.when(this.aaaPluginsHolderMock.getAuthorizationPlugin()).thenReturn(this.authorizationPluginMock);
        Mockito.when(this.aaaPluginsHolderMock.getAuthenticationPlugin()).thenReturn(this.authenticationPluginMock);
        Mockito.when(this.aaaPluginsHolderMock.getFederationToLocalMapperPlugin()).thenReturn(this.federationToLocalMapperPluginMock);
        Mockito.when(this.aaaPluginsHolderMock.getFederationIdentityPlugin()).thenReturn(this.federationIdentityPluginMock);
        this.aaaController = new AaaController(this.aaaPluginsHolderMock);
    }

    @Test(expected = UnauthorizedRequestException.class)
    public void testGettingOrderWithNonAuthorizedUser() throws UnauthorizedRequestException {
        // TODO implement this
        throw new UnauthorizedRequestException();
    }

    //test case: Check if authenticate method throws no exception when the federation token is valid and federation token
    //id is properly passed as parameter.
    @Test
    public void testAuthenticate() throws UnauthenticatedUserException, UnavailableProviderException {
        //set up
        FederationUserToken federationToken = new FederationUserToken("fake-provider",
                "fake=federation-user", "fake-user-id", "fake-name");
        Mockito.when(this.authenticationPluginMock.isAuthentic(Mockito.anyString(), Mockito.any(FederationUserToken.class))).thenReturn(true);

        //exercise/verify
        this.aaaController.authenticate("fake-member-id", federationToken);
        Mockito.verify(this.authenticationPluginMock, Mockito.times(1)).isAuthentic("fake-member-id", federationToken);
    }

    //test case: Check if authenticate method throws Unauthenticated exception when the federation token is invalid. 
    @Test(expected = UnauthenticatedUserException.class)
    public void testAuthenticateWhenUnauthenticatedUserException() throws UnauthenticatedUserException, UnavailableProviderException {
        //set up
        Mockito.when(this.authenticationPluginMock.isAuthentic(Mockito.anyString(), Mockito.any(FederationUserToken.class))).thenReturn(false);

        //exercise/verify
        this.aaaController.authenticate(Mockito.anyString(), Mockito.any(FederationUserToken.class));
    }

    //test case: Check if authorize method throws no exception when the operation is valid.
    @Test
    public void testAuthorizeOnInstanceType() throws UnauthorizedRequestException {
        //set up
        Mockito.when(this.authorizationPluginMock.isAuthorized(
                Mockito.any(FederationUserToken.class),
                Mockito.any(Operation.class),
                Mockito.any(ResourceType.class))).thenReturn(true);

        //exercise/verify
        this.aaaController.authorize(Mockito.any(FederationUserToken.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));
    }

    //test case: Check if authorize method throws no exception when the operation is valid.
    @Test
    public void testAuthorize() throws FogbowRasException {
        //set up
        Mockito.when(this.authorizationPluginMock.isAuthorized(
                Mockito.any(FederationUserToken.class),
                Mockito.any(Operation.class),
                Mockito.any(ResourceType.class))).thenReturn(true);

        //exercise/verify
        this.aaaController.authorize(Mockito.any(FederationUserToken.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));
    }

    //test case: Check if getLocalToken() is returning a valid token.
    @Ignore
    @Test
    public void testGetLocalToken() throws FogbowRasException, UnexpectedException {
        //set up

        //exercise

        //verify
    }

    //test case: Check if getLocalToken is properly forwarding FogbowRasException thrown by FederationIdentityPlugin.
    @Ignore
    @Test(expected = FogbowRasException.class)
    public void testGetLocalTokenWhenFogbowManagerException() throws FogbowRasException, UnexpectedException {
        //set up
        //exercise/verify
    }

    //test case: Check if getLocalToken is properly forwading UnexpectedException thrown by FederationIdentityPlugin.
    @Ignore
    @Test(expected = UnexpectedException.class)
    public void testGetLocalTokenWhenUnexpectedException() throws FogbowRasException, UnexpectedException {
        //set up
        //exercise/verify
    }

    //test case: Check if federation user token is returning a valid token properly.
    @Test
    public void testGetFederationUser() throws InvalidParameterException {
        //set up
        FederationUserToken expectedFederationUserToken = new FederationUserToken("fake-token-provider", "token-value", "id", "fake-name");
        Mockito.when(this.federationIdentityPluginMock.createToken(Mockito.anyString())).thenReturn(expectedFederationUserToken);

        //exercise
        FederationUserToken aaFederationUserToken = this.aaaController.getFederationUser(Mockito.anyString());

        //verify
        Assert.assertEquals(expectedFederationUserToken, aaFederationUserToken);
    }

    //test case: Check if AaaController is properly forwarding InvalidParameterException thrown by FederationIdentityPlugin.
    @Test(expected = InvalidParameterException.class)
    public void testGetFederationUserInvalidParameterException() throws InvalidParameterException {
        //set up
        Mockito.when(this.federationIdentityPluginMock.createToken(Mockito.anyString())).thenThrow(new InvalidParameterException());

        //exercise/verify
        this.aaaController.getFederationUser(Mockito.anyString());
    }
}

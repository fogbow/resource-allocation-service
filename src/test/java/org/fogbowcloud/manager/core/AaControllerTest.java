package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;
import org.fogbowcloud.manager.core.plugins.behavior.authorization.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.authentication.AuthenticationPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.identity.FederationIdentityPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.FederationToLocalMapperPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

public class AaControllerTest {

	private AaController aaController;
	private BehaviorPluginsHolder behaviorPluginsHolderMock;
    private AuthenticationPlugin authenticationPluginMock;
    private AuthorizationPlugin authorizationPluginMock;
    private FederationToLocalMapperPlugin federationToLocalMapperPluginMock;
    private FederationIdentityPlugin federationIdentityPluginMock;
	
    @Before
    public void setUp() {
    	this.behaviorPluginsHolderMock = Mockito.mock(BehaviorPluginsHolder.class);
        this.authenticationPluginMock = Mockito.mock(AuthenticationPlugin.class);
        this.authorizationPluginMock = Mockito.mock(AuthorizationPlugin.class);
        this.federationToLocalMapperPluginMock = Mockito.mock(FederationToLocalMapperPlugin.class);
        this.federationIdentityPluginMock = Mockito.mock(FederationIdentityPlugin.class);

        HomeDir.getInstance().setPath("src/test/resources/private");

        Mockito.when(this.behaviorPluginsHolderMock.getAuthorizationPlugin()).thenReturn(this.authorizationPluginMock);
    	Mockito.when(this.behaviorPluginsHolderMock.getAuthenticationPlugin()).thenReturn(this.authenticationPluginMock);
        Mockito.when(this.behaviorPluginsHolderMock.getFederationToLocalMapperPlugin()).thenReturn(this.federationToLocalMapperPluginMock);
    	Mockito.when(this.behaviorPluginsHolderMock.getFederationIdentityPlugin()).thenReturn(this.federationIdentityPluginMock);
    	this.aaController = new AaController(this.behaviorPluginsHolderMock);
    }
    
    //test case: Check if authenticate method throws no exception when the federation token is valid and federation token
    //id is properly passed as parameter.
    @Test
    public void testAuthenticate() throws UnauthenticatedUserException, UnavailableProviderException {
    	//set up
    	FederationUserToken federationToken = new FederationUserToken("fake-provider",
                "fake=federation-user","fake-user-id", "fake-name");
    	Mockito.when(this.authenticationPluginMock.isAuthentic(Mockito.any(FederationUserToken.class))).thenReturn(true);

    	//exercise/verify
    	this.aaController.authenticate(federationToken);
    	Mockito.verify(this.authenticationPluginMock, Mockito.times(1)).isAuthentic(federationToken);
    }
    
    //test case: Check if authenticate method throws Unauthenticated exception when the federation token is invalid. 
    @Test (expected = UnauthenticatedUserException.class)
    public void testAuthenticateWhenUnauthenticatedUserException() throws UnauthenticatedUserException, UnavailableProviderException {
    	//set up
    	Mockito.when(this.authenticationPluginMock.isAuthentic(Mockito.any(FederationUserToken.class))).thenReturn(false);

    	//exercise/verify
    	this.aaController.authenticate(Mockito.any(FederationUserToken.class));
    }
    
    //test case: Check if authorize method throws no exception when the operation is valid.
    @Test
    public void testAuthorizeOnInstanceType() throws UnauthorizedRequestException {
    	//set up
    	Mockito.when(this.authorizationPluginMock.isAuthorized(
    			Mockito.any(), 
    			Mockito.any(), 
    			Mockito.any(ResourceType.class))).thenReturn(true);
    	
    	//exercise/verify
    	this.aaController.authorize(Mockito.any(), Mockito.any(), Mockito.any(ResourceType.class));
    }
    
    //test case: Check if authorize method throws no exception when the operation is valid.
    @Test
    public void testAuthorize() throws FogbowManagerException {
    	//set up
    	Mockito.when(this.authorizationPluginMock.isAuthorized(
    			Mockito.any(), 
    			Mockito.any(), 
    			Mockito.any(ResourceType.class))).thenReturn(true);
    	
    	//exercise/verify
    	this.aaController.authorize(Mockito.any(), Mockito.any(), Mockito.any(ResourceType.class));
    }
    
    //test case: Check if authorize method throws Unauthenticated exception when the federation token is invalid. 
    @Test (expected = UnauthorizedRequestException.class)
    public void testAuthorizeWhenUnauthorizedRequestExceptionOnInstanceType() throws UnauthorizedRequestException {
    	//set up
    	Mockito.when(this.authorizationPluginMock.isAuthorized(
    			Mockito.any(), 
    			Mockito.any(), 
    			Mockito.any(ResourceType.class))).thenReturn(false);
    	
    	//exercise/verify
    	this.aaController.authorize(Mockito.any(), Mockito.any(), Mockito.any(ResourceType.class));
    }
    
    //test case: Check if authorize method throws Unauthenticated exception when the federation token is invalid. 
    @Test (expected = UnauthorizedRequestException.class)
    public void testAuthorizeWhenUnauthorizedRequestException() throws FogbowManagerException {
    	//set up
    	Mockito.when(this.authorizationPluginMock.isAuthorized(
    			Mockito.any(), 
    			Mockito.any(), 
    			Mockito.any(ResourceType.class))).thenReturn(false);
    	
    	//exercise/verify
    	this.aaController.authorize(Mockito.any(), Mockito.any(), Mockito.any(ResourceType.class));
    }
    
    //test case: Check if getLocalToken() is returning a valid token.
    @Ignore
	@Test
    public void testGetLocalToken() throws FogbowManagerException, UnexpectedException {
    	//set up

		//exercise

    	//verify
    }
    
    //test case: Check if getLocalToken is properly forwarding FogbowManagerException thrown by FederationIdentityPlugin.
	@Ignore
    @Test (expected = FogbowManagerException.class)
    public void testGetLocalTokenWhenFogbowManagerException() throws FogbowManagerException, UnexpectedException {
    	//set up
    	//exercise/verify
    }
    
    //test case: Check if getLocalToken is properly forwading UnexpectedException thrown by FederationIdentityPlugin.
	@Ignore
    @Test (expected = UnexpectedException.class)
    public void testGetLocalTokenWhenUnexpectedException() throws FogbowManagerException, UnexpectedException {
    	//set up
    	//exercise/verify
    }
    
    //test case: Check if federation user token is returning a valid token properly.
    @Test
    public void testGetFederationUser() throws UnauthenticatedUserException, InvalidParameterException {
    	//set up
    	FederationUserToken expectedFederationUserToken = new FederationUserToken("fake-token-provider", "token-value", "id", "fake-name");
    	Mockito.when(this.federationIdentityPluginMock.createToken(Mockito.anyString())).thenReturn(expectedFederationUserToken);
    	
    	//exercise
    	FederationUserToken aaFederationUserToken = this.aaController.getFederationUser(Mockito.anyString());
    	
    	//verify
    	Assert.assertEquals(expectedFederationUserToken, aaFederationUserToken);
    }
    
    //test case: Check if AaController is properly forwarding UnauthenticatedUserException thrown by FederationIdentityPlugin.
    @Test (expected = UnauthenticatedUserException.class)
    public void testGetFederationUserUnauthenticatedUserException() throws UnauthenticatedUserException, InvalidParameterException {
    	//set up
    	Mockito.when(this.federationIdentityPluginMock.createToken(Mockito.anyString())).thenThrow(new UnauthenticatedUserException());
    	
    	//exercise/verify
    	this.aaController.getFederationUser(Mockito.anyString());
    }
}

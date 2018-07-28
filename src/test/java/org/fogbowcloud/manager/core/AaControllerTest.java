package org.fogbowcloud.manager.core;

import java.util.HashMap;
import java.util.Map;

import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.core.plugins.behavior.authorization.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.authentication.AuthenticationPlugin;
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
	
    @Before
    public void setUp() {
    	this.behaviorPluginsHolderMock = Mockito.mock(BehaviorPluginsHolder.class);
        this.authenticationPluginMock = Mockito.mock(AuthenticationPlugin.class);
        this.authorizationPluginMock = Mockito.mock(AuthorizationPlugin.class);
        this.federationToLocalMapperPluginMock = Mockito.mock(FederationToLocalMapperPlugin.class);
    	
    	Mockito.when(this.behaviorPluginsHolderMock.getAuthorizationPlugin()).thenReturn(this.authorizationPluginMock);
    	Mockito.when(this.behaviorPluginsHolderMock.getAuthenticationPlugin()).thenReturn(this.authenticationPluginMock);
    	Mockito.when(this.behaviorPluginsHolderMock.getFederationToLocalMapperPlugin()).thenReturn(this.federationToLocalMapperPluginMock);
    	this.aaController = new AaController(this.behaviorPluginsHolderMock);
    }
    
    //test case: Check if authenticate method throws no exception when the federation token is valid and federation token
    //id is properly passed as parameter.
    @Test
    public void testAuthenticate() throws UnauthenticatedUserException {
    	//set up
    	String federationTokenId = "federation-token-id";
    	Mockito.when(this.authenticationPluginMock.isValid(Mockito.anyString())).thenReturn(true);

    	//exercise/verify
    	this.aaController.authenticate(federationTokenId);
    	Mockito.verify(this.authenticationPluginMock, Mockito.times(1)).isValid(federationTokenId);
    }
    
    //test case: Check if authenticate method throws Unauthenticated exception when the federation token is invalid. 
    @Test (expected = UnauthenticatedUserException.class)
    public void testAuthenticateWhenUnauthenticatedUserException() throws UnauthenticatedUserException {
    	//set up
    	Mockito.when(this.authenticationPluginMock.isValid(Mockito.anyString())).thenReturn(false);

    	//exercise/verify
    	this.aaController.authenticate(Mockito.anyString());
    }
    
    //test case: Check if authorize method throws no exception when the operation is valid.
    @Test
    public void testAuthorizeOnInstanceType() throws UnauthorizedRequestException {
    	//set up
    	Mockito.when(this.authorizationPluginMock.isAuthorized(
    			Mockito.any(), 
    			Mockito.any(), 
    			Mockito.any(InstanceType.class))).thenReturn(true);
    	
    	//exercise/verify
    	this.aaController.authorize(Mockito.any(), Mockito.any(), Mockito.any(InstanceType.class));
    }
    
    //test case: Check if authorize method throws no exception when the operation is valid.
    @Test
    public void testAuthorizeOnOrderType() throws FogbowManagerException {
    	//set up
    	Mockito.when(this.authorizationPluginMock.isAuthorized(
    			Mockito.any(), 
    			Mockito.any(), 
    			Mockito.any(InstanceType.class))).thenReturn(true);
    	
    	//exercise/verify
    	this.aaController.authorize(Mockito.any(), Mockito.any(), Mockito.any(InstanceType.class));
    }
    
    //test case: Check if authorize method throws no exception when the operation is valid.
    @Test
    public void testAuthorize() throws FogbowManagerException {
    	//set up
    	Mockito.when(this.authorizationPluginMock.isAuthorized(
    			Mockito.any(), 
    			Mockito.any())).thenReturn(true);
    	
    	//exercise/verify
    	this.aaController.authorize(Mockito.any(), Mockito.any());
    }
    
    //test case: Check if authorize method throws Unauthenticated exception when the federation token is invalid. 
    @Test (expected = UnauthorizedRequestException.class)
    public void testAuthorizeWhenUnauthorizedRequestExceptionOnInstanceType() throws UnauthorizedRequestException {
    	//set up
    	Mockito.when(this.authorizationPluginMock.isAuthorized(
    			Mockito.any(), 
    			Mockito.any(), 
    			Mockito.any(InstanceType.class))).thenReturn(false);
    	
    	//exercise/verify
    	this.aaController.authorize(Mockito.any(), Mockito.any(), Mockito.any(InstanceType.class));
    }
    
    //test case: Check if authorize method throws Unauthenticated exception when the federation token is invalid. 
    @Test (expected = UnauthorizedRequestException.class)
    public void testAuthorizeWhenUnauthorizedRequestExceptionOnOrderType() throws FogbowManagerException {
    	//set up
    	Mockito.when(this.authorizationPluginMock.isAuthorized(
    			Mockito.any(), 
    			Mockito.any(), 
    			Mockito.any(InstanceType.class))).thenReturn(false);
    	
    	//exercise/verify
    	this.aaController.authorize(Mockito.any(), Mockito.any(), Mockito.any(InstanceType.class));
    }
    
    //test case: Check if authorize method throws Unauthenticated exception when the federation token is invalid. 
    @Test (expected = UnauthorizedRequestException.class)
    public void testAuthorizeWhenUnauthorizedRequestException() throws FogbowManagerException {
    	//set up
    	Mockito.when(this.authorizationPluginMock.isAuthorized(
    			Mockito.any(), 
    			Mockito.any())).thenReturn(false);
    	
    	//exercise/verify
    	this.aaController.authorize(Mockito.any(), Mockito.any());
    }
    
    //test case: Check if getLocalToken() is returning a valid token.
    @Ignore
	@Test
    public void testGetLocalToken() throws FogbowManagerException, UnexpectedException {
    	//set up

		//exercise

    	//verify
    }
    
    //test case: Check if getLocalToken is properly forwarding FogbowManagerException thrown by LocalTokenGenerator.
	@Ignore
    @Test (expected = FogbowManagerException.class)
    public void testGetLocalTokenWhenFogbowManagerException() throws FogbowManagerException, UnexpectedException {
    	//set up
    	//exercise/verify
    }
    
    //test case: Check if getLocalToken is properly forwading UnexpectedException thrown by LocalTokenGenerator.
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
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
    	FederationUser expectedFederationUser = new FederationUser("id", attributes);
    	Mockito.when(this.authenticationPluginMock.getFederationUser(Mockito.anyString())).thenReturn(expectedFederationUser);
    	
    	//exercise
    	FederationUser aaFederationUser = this.aaController.getFederationUser(Mockito.anyString());
    	
    	//verify
    	Assert.assertEquals(expectedFederationUser,aaFederationUser);
    }
    
    //test case: Check if AaController is properly forwarding UnauthenticatedUserException thrown by AuthenticationPlugin.
    @Test (expected = UnauthenticatedUserException.class)
    public void testGetFederationUserUnauthenticatedUserException() throws UnauthenticatedUserException, InvalidParameterException {
    	//set up
    	Mockito.when(this.authenticationPluginMock.getFederationUser(Mockito.anyString())).thenThrow(new UnauthenticatedUserException());
    	
    	//exercise/verify
    	this.aaController.getFederationUser(Mockito.anyString());
    }
    
    //test case: Check if AaController is properly forwarding InvalidParameterException thrown by AuthenticationPlugin.
    @Test (expected = InvalidParameterException.class)
    public void testGetFederationUserUnexpectedException() throws UnauthenticatedUserException, InvalidParameterException {
    	//set up
    	Mockito.when(this.authenticationPluginMock.getFederationUser(Mockito.anyString())).thenThrow(new InvalidParameterException());
    	
    	//exercise/verify
    	this.aaController.getFederationUser(Mockito.anyString());
    }
}

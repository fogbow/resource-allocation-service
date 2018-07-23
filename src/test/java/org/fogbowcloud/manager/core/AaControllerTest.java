package org.fogbowcloud.manager.core;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.manager.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.models.tokens.Token.User;
import org.fogbowcloud.manager.core.plugins.behavior.authorization.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.federationidentity.FederationIdentityPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.LocalUserCredentialsMapperPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.LocalIdentityPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class AaControllerTest {

	private AaController aaController;
	private LocalIdentityPlugin localIdentityPluginMock;
	private BehaviorPluginsHolder behaviorPluginsHolderMock;
    private FederationIdentityPlugin federationIdentityPluginMock;
    private AuthorizationPlugin authorizationPluginMock;
    private LocalUserCredentialsMapperPlugin localUserCredentialsMapperPluginMock;
	
    @Before
    public void setUp() {
    	this.localIdentityPluginMock = Mockito.mock(LocalIdentityPlugin.class);
    	this.behaviorPluginsHolderMock = Mockito.mock(BehaviorPluginsHolder.class);
        this.federationIdentityPluginMock = Mockito.mock(FederationIdentityPlugin.class);
        this.authorizationPluginMock = Mockito.mock(AuthorizationPlugin.class);
        this.localUserCredentialsMapperPluginMock = Mockito.mock(LocalUserCredentialsMapperPlugin.class);
    	
    	Mockito.when(this.behaviorPluginsHolderMock.getAuthorizationPlugin()).thenReturn(this.authorizationPluginMock);
    	Mockito.when(this.behaviorPluginsHolderMock.getFederationIdentityPlugin()).thenReturn(this.federationIdentityPluginMock);
    	Mockito.when(this.behaviorPluginsHolderMock.getLocalUserCredentialsMapperPlugin()).thenReturn(this.localUserCredentialsMapperPluginMock);
    	this.aaController = new AaController(this.localIdentityPluginMock, this.behaviorPluginsHolderMock);
    }
    
    //test case: Check if authenticate method throws no exception when the federation token is valid and federation token
    //id is properly passed as parameter.
    @Test
    public void testAuthenticate() throws UnauthenticatedUserException {
    	//set up
    	String federationTokenId = "federation-token-id";
    	Mockito.when(this.federationIdentityPluginMock.isValid(Mockito.anyString())).thenReturn(true);
    	
    	//exercise/verify
    	this.aaController.authenticate(federationTokenId);
    	Mockito.verify(this.federationIdentityPluginMock, Mockito.times(1)).isValid(federationTokenId);
    }
    
    //test case: Check if authenticate method throws Unauthenticated exception when the federation token is invalid. 
    @Test (expected = UnauthenticatedUserException.class)
    public void testAuthenticateWhenUnauthenticatedUserException() throws UnauthenticatedUserException {
    	//set up
    	Mockito.when(this.federationIdentityPluginMock.isValid(Mockito.anyString())).thenReturn(false);
    	
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
    			Mockito.any(Order.class))).thenReturn(true);
    	
    	//exercise/verify
    	this.aaController.authorize(Mockito.any(), Mockito.any(), Mockito.any(Order.class));
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
    			Mockito.any(Order.class))).thenReturn(false);
    	
    	//exercise/verify
    	this.aaController.authorize(Mockito.any(), Mockito.any(), Mockito.any(Order.class));
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
    
    //test case: Check if create token is returning a valid token properly.
    @Test
    public void testGetLocalToken() throws FogbowManagerException, UnexpectedException {
    	//set up
    	Mockito.doReturn(new HashMap<String, String>()).when(this.localUserCredentialsMapperPluginMock).getCredentials(Mockito.any());
    	String accessId = "accessId";
    	User user = new User("id", "name");
    	Date date = new Date();
    	Map <String, String> attributes = new HashMap<String, String>();
    	Token expectedToken = new Token(accessId, user, date, attributes);
    	Mockito.when(this.localIdentityPluginMock.createToken(Mockito.any())).thenReturn(expectedToken);
    	FederationUser federationUser = Mockito.mock(FederationUser.class);
    	
    	//exercise
    	Token aaControllerToken = this.aaController.getLocalToken(federationUser);
    	
    	//verify
    	Assert.assertEquals(expectedToken, aaControllerToken);
    	Mockito.verify(this.localUserCredentialsMapperPluginMock, Mockito.times(1)).getCredentials(federationUser);
    }
    
    //test case: Check if getLocalToken is properly forwading FogbowManagerException thrown by LocalIdentityPlugin. 
    @Test (expected = FogbowManagerException.class)
    public void testGetLocalTokenWhenFogbowManagerException() throws FogbowManagerException, UnexpectedException {
    	//set up
    	Mockito.when(this.localUserCredentialsMapperPluginMock.getCredentials(Mockito.any())).thenReturn(new HashMap<String, String>());
    	Mockito.when(this.localIdentityPluginMock.createToken(Mockito.any())).thenThrow(new FogbowManagerException());
    	
    	//exercise/verify
    	this.aaController.getLocalToken(Mockito.any());
    }
    
    //test case: Check if getLocalToken is properly forwading UnexpectedException thrown by LocalIdentityPlugin. 
    @Test (expected = UnexpectedException.class)
    public void testGetLocalTokenWhenUnexpectedException() throws FogbowManagerException, UnexpectedException {
    	//set up 
    	Mockito.when(this.localUserCredentialsMapperPluginMock.getCredentials(Mockito.any())).thenReturn(new HashMap<String, String>());
    	Mockito.when(this.localIdentityPluginMock.createToken(Mockito.any())).thenThrow(new UnexpectedException());
    	
    	//exercise/verify
    	this.aaController.getLocalToken(Mockito.any());
    }
    
    //test case: Check if federation user token is returning a valid token properly.
    @Test
    public void testGetFederationUser() throws UnauthenticatedUserException, UnexpectedException {
    	//set up
    	FederationUser expectedFederationUser = new FederationUser("id", new HashMap<String, String>());
    	Mockito.when(this.federationIdentityPluginMock.getFederationUser(Mockito.anyString())).thenReturn(expectedFederationUser);
    	
    	//exercise
    	FederationUser aaFederationUser = this.aaController.getFederationUser(Mockito.anyString());
    	
    	//verify
    	Assert.assertEquals(expectedFederationUser,aaFederationUser);
    }
    
    //test case: Check if AaController is properly forwading UnauthenticatedUserException thrown by FederationIdentityPlugin. 
    @Test (expected = UnauthenticatedUserException.class)
    public void testGetFederationUserUnauthenticatedUserException() throws UnauthenticatedUserException, UnexpectedException {
    	//set up
    	Mockito.when(this.federationIdentityPluginMock.getFederationUser(Mockito.anyString())).thenThrow(new UnauthenticatedUserException());
    	
    	//exercise/verify
    	this.aaController.getFederationUser(Mockito.anyString());
    }
    
    //test case: Check if AaController is properly forwading UnexpectedException thrown by FederationIdentityPlugin. 
    @Test (expected = UnexpectedException.class)
    public void testGetFederationUserUnexpectedException() throws UnauthenticatedUserException, UnexpectedException {
    	//set up
    	Mockito.when(this.federationIdentityPluginMock.getFederationUser(Mockito.anyString())).thenThrow(new UnexpectedException());
    	
    	//exercise/verify
    	this.aaController.getFederationUser(Mockito.anyString());
    }
}

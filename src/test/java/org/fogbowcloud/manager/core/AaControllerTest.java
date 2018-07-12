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
    	this.aaController = new AaController(this.localIdentityPluginMock, this.behaviorPluginsHolderMock);
    	
    	Mockito.when(this.behaviorPluginsHolderMock.getAuthorizationPlugin()).thenReturn(this.authorizationPluginMock);
    	Mockito.when(this.behaviorPluginsHolderMock.getFederationIdentityPlugin()).thenReturn(this.federationIdentityPluginMock);
    	Mockito.when(this.behaviorPluginsHolderMock.getLocalUserCredentialsMapperPlugin()).thenReturn(this.localUserCredentialsMapperPluginMock);
    	
    	this.aaController = new AaController(this.localIdentityPluginMock, this.behaviorPluginsHolderMock);
    	
    }
    
    @Test
    public void testAuthenticate() throws UnauthenticatedUserException {
    	Mockito.when(this.federationIdentityPluginMock.isValid(Mockito.anyString())).thenReturn(true);
    	this.aaController.authenticate(Mockito.anyString());
    }
    
    @Test (expected = UnauthenticatedUserException.class)
    public void testAuthenticateWhenUnauthenticatedUserException() throws UnauthenticatedUserException {
    	Mockito.when(this.federationIdentityPluginMock.isValid(Mockito.anyString())).thenReturn(false);
    	this.aaController.authenticate(Mockito.anyString());
    }
    
    @Test
    public void testAuthorizeOnInstance() throws UnauthorizedRequestException {
    	Mockito.when(this.authorizationPluginMock.isAuthorized(
    			Mockito.any(), 
    			Mockito.any(), 
    			Mockito.any(InstanceType.class))).thenReturn(true);
    	this.aaController.authorize(Mockito.any(), Mockito.any(), Mockito.any(InstanceType.class));
    }
    
    @Test
    public void testAuthorizeOnOrder() throws FogbowManagerException {
    	Mockito.when(this.authorizationPluginMock.isAuthorized(
    			Mockito.any(), 
    			Mockito.any(), 
    			Mockito.any(Order.class))).thenReturn(true);
    	this.aaController.authorize(Mockito.any(), Mockito.any(), Mockito.any(Order.class));
    }
    
    @Test
    public void testAuthorize() throws FogbowManagerException {
    	Mockito.when(this.authorizationPluginMock.isAuthorized(
    			Mockito.any(), 
    			Mockito.any())).thenReturn(true);
    	this.aaController.authorize(Mockito.any(), Mockito.any());
    }
    
    @Test (expected = UnauthorizedRequestException.class)
    public void testAuthorizeWhenUnauthorizedRequestExceptionOnInstance() throws UnauthorizedRequestException {
    	Mockito.when(this.authorizationPluginMock.isAuthorized(
    			Mockito.any(), 
    			Mockito.any(), 
    			Mockito.any(InstanceType.class))).thenReturn(false);
    	this.aaController.authorize(Mockito.any(), Mockito.any(), Mockito.any(InstanceType.class));
    }
    
    @Test (expected = UnauthorizedRequestException.class)
    public void testAuthorizeWhenUnauthorizedRequestExceptionOnOrder() throws FogbowManagerException {
    	Mockito.when(this.authorizationPluginMock.isAuthorized(
    			Mockito.any(), 
    			Mockito.any(), 
    			Mockito.any(Order.class))).thenReturn(false);
    	this.aaController.authorize(Mockito.any(), Mockito.any(), Mockito.any(Order.class));
    }
    
    @Test (expected = UnauthorizedRequestException.class)
    public void testAuthorizeWhenUnauthorizedRequestException() throws FogbowManagerException {
    	Mockito.when(this.authorizationPluginMock.isAuthorized(
    			Mockito.any(), 
    			Mockito.any())).thenReturn(false);
    	this.aaController.authorize(Mockito.any(), Mockito.any());
    }
    
    @Test
    public void testGetLocalToken() throws FogbowManagerException, UnexpectedException {
    	Mockito.when(this.localUserCredentialsMapperPluginMock.getCredentials(Mockito.any())).thenReturn(new HashMap<String, String>());
    	String accessId = "accessId";
    	User user = new User("id", "name");
    	Date date = new Date();
    	Map <String, String> attributes = new HashMap<String, String>();
    	Token token = new Token(accessId, user, date, attributes);
    	Mockito.when(this.localIdentityPluginMock.createToken(Mockito.any())).thenReturn(token);
    	Assert.assertEquals(token, this.aaController.getLocalToken(null));
    }
    
    @Test (expected = FogbowManagerException.class)
    public void testGetLocalTokenWhenFogbowManagerException() throws FogbowManagerException, UnexpectedException {
    	Mockito.when(this.localUserCredentialsMapperPluginMock.getCredentials(Mockito.any())).thenReturn(new HashMap<String, String>());
    	Mockito.when(this.localIdentityPluginMock.createToken(Mockito.any())).thenThrow(new FogbowManagerException());
    	this.aaController.getLocalToken(Mockito.any());
    }
    
    @Test (expected = UnexpectedException.class)
    public void testGetLocalTokenWhenUnexpectedException() throws FogbowManagerException, UnexpectedException {
    	Mockito.when(this.localUserCredentialsMapperPluginMock.getCredentials(Mockito.any())).thenReturn(new HashMap<String, String>());
    	Mockito.when(this.localIdentityPluginMock.createToken(Mockito.any())).thenThrow(new UnexpectedException());
    	this.aaController.getLocalToken(Mockito.any());
    }
    
    @Test
    public void testGetFederationUser() throws UnauthenticatedUserException, UnexpectedException {
    	FederationUser federationUser = new FederationUser("id", new HashMap<String, String>());
    	Mockito.when(this.federationIdentityPluginMock.getFederationUser(Mockito.anyString())).thenReturn(federationUser);
    	Assert.assertEquals(federationUser, this.aaController.getFederationUser(Mockito.anyString()));
    }
    
    @Test (expected = UnauthenticatedUserException.class)
    public void testGetFederationUserUnauthenticatedUserException() throws UnauthenticatedUserException, UnexpectedException {
    	Mockito.when(this.federationIdentityPluginMock.getFederationUser(Mockito.anyString())).thenThrow(new UnauthenticatedUserException());
    	this.aaController.getFederationUser(Mockito.anyString());
    }
    
    @Test (expected = UnexpectedException.class)
    public void testGetFederationUserUnexpectedException() throws UnauthenticatedUserException, UnexpectedException {
    	Mockito.when(this.federationIdentityPluginMock.getFederationUser(Mockito.anyString())).thenThrow(new UnexpectedException());
    	this.aaController.getFederationUser(Mockito.anyString());
    }
}

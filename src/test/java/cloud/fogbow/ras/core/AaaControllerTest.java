//package cloud.fogbow.ras.core;
//
//import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
//import cloud.fogbow.ras.core.constants.ConfigurationConstants;
//import cloud.fogbow.ras.core.models.Operation;
//import cloud.fogbow.ras.core.models.ResourceType;
//import cloud.fogbow.common.models.FederationUser;
//import cloud.fogbow.ras.core.plugins.mapper.FederationToLocalMapperPlugin;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Ignore;
//import org.junit.Test;
//import org.mockito.Mockito;
//
//public class AaaControllerTest {
//
//    private AuthorizationPlugin authorizationPluginMock;
//    private FederationToLocalMapperPlugin federationToLocalMapperPluginMock;
//
//    @Before
//    public void setUp() {
//        this.aaaPluginsHolderMock = Mockito.mock(AaaPluginsHolder.class);
//        this.authenticationPluginMock = Mockito.mock(AuthenticationPlugin.class);
//        this.authorizationPluginMock = Mockito.mock(AuthorizationPlugin.class);
//        this.federationToLocalMapperPluginMock = Mockito.mock(FederationToLocalMapperPlugin.class);
//        this.federationIdentityPluginMock = Mockito.mock(FederationIdentityPlugin.class);
//
//        Mockito.when(this.aaaPluginsHolderMock.getAuthorizationPlugin()).thenReturn(this.authorizationPluginMock);
//        Mockito.when(this.aaaPluginsHolderMock.getAuthenticationPlugin()).thenReturn(this.authenticationPluginMock);
//        Mockito.when(this.aaaPluginsHolderMock.getFederationIdentityPlugin()).thenReturn(this.federationIdentityPluginMock);
//        this.aaaController = new AaaController(this.aaaPluginsHolderMock,
//                PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID_KEY));
//    }
//
//    @Test(expected = UnauthorizedRequestException.class)
//    public void testGettingOrderWithNonAuthorizedUser() throws UnauthorizedRequestException {
//        // TODO implement this
//        throw new UnauthorizedRequestException();
//    }
//
//    //test case: Check if authenticate method throws no exception when the federation token is valid and federation token
//    //id is properly passed as parameter.
//    @Test
//    public void testAuthenticate() throws UnauthenticatedUserException, UnavailableProviderException {
//        //set up
//        FederationUser federationToken = new FederationUser("fake-provider",
//                "fake=federation-user", "fake-user-id", "fake-name");
//        Mockito.when(this.authenticationPluginMock.isAuthentic(Mockito.anyString(), Mockito.any(FederationUser.class))).thenReturn(true);
//
//        //exercise/verify
//        this.aaaController.authenticate("fake-member-id", federationToken);
//        Mockito.verify(this.authenticationPluginMock, Mockito.times(1)).isAuthentic("fake-member-id", federationToken);
//    }
//
//    //test case: Check if authenticate method throws Unauthenticated exception when the federation token is invalid.
//    @Test(expected = UnauthenticatedUserException.class)
//    public void testAuthenticateWhenUnauthenticatedUserException() throws UnauthenticatedUserException, UnavailableProviderException {
//        //set up
//        Mockito.when(this.authenticationPluginMock.isAuthentic(Mockito.anyString(), Mockito.any(FederationUser.class))).thenReturn(false);
//
//        //exercise/verify
//        this.aaaController.authenticate(Mockito.anyString(), Mockito.any(FederationUser.class));
//    }
//
//    //test case: Check if authorize method throws no exception when the operation is valid.
//    @Test
//    public void testAuthorizeOnInstanceType() throws UnauthorizedRequestException {
//        //set up
//        Mockito.when(this.authorizationPluginMock.isAuthorized(
//                Mockito.any(FederationUser.class), Mockito.anyString(),
//                Mockito.any(Operation.class),
//                Mockito.any(ResourceType.class))).thenReturn(true);
//
//        //exercise/verify
//        this.aaaController.authorize(Mockito.anyString(), Mockito.any(FederationUser.class),
//                Mockito.any(Operation.class), Mockito.any(ResourceType.class));
//    }
//
//    //test case: Check if authorize method throws no exception when the operation is valid.
//    @Test
//    public void testAuthorize() throws FogbowRasException {
//        //set up
//        Mockito.when(this.authorizationPluginMock.isAuthorized(
//                Mockito.any(FederationUser.class), Mockito.anyString(),
//                Mockito.any(Operation.class),
//                Mockito.any(ResourceType.class))).thenReturn(true);
//
//        //exercise/verify
//        this.aaaController.authorize(Mockito.anyString(), Mockito.any(FederationUser.class),
//                Mockito.any(Operation.class), Mockito.any(ResourceType.class));
//    }
//
//    //test case: Check if getLocalToken() is returning a valid token.
//    @Ignore
//    @Test
//    public void testGetLocalToken() throws FogbowRasException, UnexpectedException {
//        //set up
//
//        //exercise
//
//        //verify
//    }
//
//    //test case: Check if getLocalToken is properly forwarding FogbowRasException thrown by FederationIdentityPlugin.
//    @Ignore
//    @Test(expected = FogbowRasException.class)
//    public void testGetLocalTokenWhenFogbowManagerException() throws FogbowRasException, UnexpectedException {
//        //set up
//        //exercise/verify
//    }
//
//    //test case: Check if getLocalToken is properly forwading UnexpectedException thrown by FederationIdentityPlugin.
//    @Ignore
//    @Test(expected = UnexpectedException.class)
//    public void testGetLocalTokenWhenUnexpectedException() throws FogbowRasException, UnexpectedException {
//        //set up
//        //exercise/verify
//    }
//
//    //test case: Check if federation user token is returning a valid token properly.
//    @Test
//    public void testGetFederationUser() throws InvalidTokenException {
//        //set up
//        FederationUser expectedFederationUser = new FederationUser("fake-token-provider", "token-value", "id", "fake-name");
//        Mockito.when(this.federationIdentityPluginMock.createToken(Mockito.anyString())).thenReturn(expectedFederationUser);
//
//        //exercise
//        FederationUser aaFederationUser = this.aaaController.getFederationUser(Mockito.anyString());
//
//        //verify
//        Assert.assertEquals(expectedFederationUser, aaFederationUser);
//    }
//
//    //test case: Check if AaaController is properly forwarding InvalidParameterException thrown by FederationIdentityPlugin.
//    @Test(expected = InvalidTokenException.class)
//    public void testGetFederationUserInvalidTokenException() throws InvalidTokenException {
//        //set up
//        Mockito.when(this.federationIdentityPluginMock.createToken(Mockito.anyString())).thenThrow(new InvalidTokenException());
//
//        //exercise/verify
//        this.aaaController.getFederationUser(Mockito.anyString());
//    }
//}

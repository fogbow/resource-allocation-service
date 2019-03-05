//package cloud.fogbow.ras.core;
//
//import cloud.fogbow.common.exceptions.*;
//import cloud.fogbow.common.plugins.authorization.AuthorizationController;
//import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
//import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
//import cloud.fogbow.ras.core.models.Operation;
//import cloud.fogbow.ras.core.models.ResourceType;
//import cloud.fogbow.common.models.SystemUser;
//import cloud.fogbow.ras.core.plugins.mapper.SystemToCloudMapperPlugin;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Ignore;
//import org.junit.Test;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//
//import java.util.HashMap;
//
//public class AaaControllerTest {
//    private static final String FAKE_OPERATION = "fake-operation";
//    private static final String FAKE_RESOURCE_TYPE = "fake-resource-type";
//
//    private AuthorizationPlugin authorizationPluginMock;
//    private AuthorizationController authorizationController;
//    private SystemToCloudMapperPlugin federationToLocalMapperPluginMock;
//
//    @Before
//    public void setUp() {
//        this.authorizationPluginMock = Mockito.mock(AuthorizationPlugin.class);
//        this.authorizationController =  new AuthorizationController(this.authorizationPluginMock);
//        this.federationToLocalMapperPluginMock = Mockito.mock(SystemToCloudMapperPlugin.class);
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
//    public void testAuthenticate() throws UnexpectedException, UnauthorizedRequestException {
//        //set up
//        SystemUser federationToken = new SystemUser(new HashMap<>());
//        Mockito.when(this.authorizationPluginMock.isAuthorized(federationToken, Mockito.anyString(), Mockito.anyString()))
//            .thenReturn(true);
//
//        //exercise
//        this.authorizationController.authorize(federationToken, "fake-operation", "fake-resource-type");
//
//        //verify
//        Mockito.verify(this.authorizationPluginMock, Mockito.times(1))
//                .isAuthorized(federationToken, Mockito.anyString(), Mockito.anyString());
//    }
//
//    //test case: Check if authenticate method throws Unauthenticated exception when the federation token is invalid.
//    @Test(expected = UnauthenticatedUserException.class)
//    public void testAuthenticateWhenUnauthenticatedUserException() throws UnauthenticatedUserException, UnavailableProviderException {
//        //set up
//        Mockito.when(this.authenticationPluginMock.isAuthentic(Mockito.anyString(), Mockito.any(SystemUser.class))).thenReturn(false);
//
//        //exercise/verify
//        this.aaaController.authenticate(Mockito.anyString(), Mockito.any(SystemUser.class));
//    }
//
//    //test case: Check if authorize method throws no exception when the operation is valid.
//    @Test
//    public void testAuthorizeOnInstanceType() throws UnauthorizedRequestException {
//        //set up
//        Mockito.when(this.authorizationPluginMock.isAuthorized(
//                Mockito.any(SystemUser.class), Mockito.anyString(),
//                Mockito.any(Operation.class),
//                Mockito.any(ResourceType.class))).thenReturn(true);
//
//        //exercise/verify
//        this.aaaController.authorize(Mockito.anyString(), Mockito.any(SystemUser.class),
//                Mockito.any(Operation.class), Mockito.any(ResourceType.class));
//    }
//
//    //test case: Check if authorize method throws no exception when the operation is valid.
//    @Test
//    public void testAuthorize() throws FogbowException {
//        //set up
//        Mockito.when(this.authorizationPluginMock.isAuthorized(
//                Mockito.any(SystemUser.class), Mockito.anyString(),
//                Mockito.any(Operation.class),
//                Mockito.any(ResourceType.class))).thenReturn(true);
//
//        //exercise/verify
//        this.aaaController.authorize(Mockito.anyString(), Mockito.any(SystemUser.class),
//                Mockito.any(Operation.class), Mockito.any(ResourceType.class));
//    }
//
//    //test case: Check if getLocalToken() is returning a valid token.
//    @Ignore
//    @Test
//    public void testGetLocalToken() throws FogbowException, UnexpectedException {
//        //set up
//
//        //exercise
//
//        //verify
//    }
//
//    //test case: Check if getLocalToken is properly forwarding FogbowRasException thrown by FederationIdentityPlugin.
//    @Ignore
//    @Test(expected = FogbowException.class)
//    public void testGetLocalTokenWhenFogbowManagerException() throws FogbowException, UnexpectedException {
//        //set up
//        //exercise/verify
//    }
//
//    //test case: Check if getLocalToken is properly forwading UnexpectedException thrown by FederationIdentityPlugin.
//    @Ignore
//    @Test(expected = UnexpectedException.class)
//    public void testGetLocalTokenWhenUnexpectedException() throws FogbowException, UnexpectedException {
//        //set up
//        //exercise/verify
//    }
//
//    //test case: Check if federation user token is returning a valid token properly.
//    @Test
//    public void testGetFederationUser() throws InvalidTokenException {
//        //set up
//        SystemUser expectedFederationUser = new SystemUser("fake-token-provider", "token-value", "id", "fake-name");
//        Mockito.when(this.federationIdentityPluginMock.createToken(Mockito.anyString())).thenReturn(expectedFederationUser);
//
//        //exercise
//        SystemUser aaFederationUser = this.aaaController.getCloudUser(Mockito.anyString());
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
//        this.aaaController.getCloudUser(Mockito.anyString());
//    }
//}

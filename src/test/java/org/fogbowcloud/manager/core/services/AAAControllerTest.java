//package org.fogbowcloud.manager.core.services;
//
//import static org.mockito.BDDMockito.given;
//
//import java.util.Properties;
//
//import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
//import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
//import org.fogbowcloud.manager.core.constants.Operation;
//import org.fogbowcloud.manager.core.manager.plugins.behavior.authorization.AuthorizationPlugin;
//import org.fogbowcloud.manager.core.manager.plugins.behavior.federationidentity.FederationIdentityPlugin;
//import org.fogbowcloud.manager.core.manager.plugins.cloud.localidentity.LocalIdentityPlugin;
//import org.fogbowcloud.manager.core.manager.plugins.exceptions.TokenCreationException;
//import org.fogbowcloud.manager.core.manager.plugins.exceptions.UnauthorizedException;
//import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
//import org.fogbowcloud.manager.core.models.orders.Order;
//import org.fogbowcloud.manager.core.models.orders.OrderType;
//import org.fogbowcloud.manager.core.models.token.FederationUser;
//import org.fogbowcloud.manager.core.models.token.Token;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.Mockito;
//import org.powermock.api.mockito.PowerMockito;
//import org.powermock.core.classloader.annotations.PrepareForTest;
//import org.powermock.modules.junit4.PowerMockRunner;
//
//@RunWith(PowerMockRunner.class)
//@PrepareForTest({AuthenticationControllerUtil.class})
//public class AAAControllerTest {
//
//    	private AaController AaController;
//    	private AuthorizationPlugin authorizationPlugin;
//    	private FederationIdentityPlugin federationIdentityPlugin;
//    	private LocalIdentityPlugin localIdentityPlugin;
//    	private Properties properties;
//
//    	@Before
//    	public void setUp() {
//    		this.properties = new Properties();
//    		this.federationIdentityPlugin = Mockito.mock(FederationIdentityPlugin.class);
//    		this.localIdentityPlugin = Mockito.mock(LocalIdentityPlugin.class);
//    		this.authorizationPlugin = Mockito.mock(AuthorizationPlugin.class);
//    		this.AaController = Mockito.spy(new AaController(this.federationIdentityPlugin,
//    				this.localIdentityPlugin, this.authorizationPlugin, this.properties));
//    		PowerMockito.mockStatic(AuthenticationControllerUtil.class);
//    	}
//
//    	@Test
//    	public void testAuthenticate() throws UnauthenticatedException {
//    		boolean isAuthenticated = true;
//
//    	    Mockito.doReturn(isAuthenticated).when(this.federationIdentityPlugin).isValid(Mockito.anyString());
//    		this.AaController.authenticate(Mockito.anyString());
//    	}
//
//    	@Test(expected=UnauthorizedException.class)
//    	public void testAuthenticationFail() throws UnauthenticatedException {
//    		Mockito.doThrow(UnauthorizedException.class).when(
//    				this.federationIdentityPlugin).isValid(Mockito.anyString());
//    		this.AaController.authenticate(Mockito.anyString());
//    	}
//
//    	@Test
//    	public void testAuthorizePassingOrderTypeParam() {
//				FederationUser federationUser = new FederationUser(-1L, null);
//    		Mockito.doReturn(true).when(this.authorizationPlugin).isAuthorized(Mockito.eq(federationUser),
//                    Mockito.any(Operation.class), Mockito.any(OrderType.class));
//
//    		try {
//    		    Operation operation = Operation.GET;
//    		    OrderType orderType = OrderType.COMPUTE;
//    			this.AaController.authorize(federationUser, operation, orderType);
//    		} catch (Exception e) {
//    			Assert.fail();
//    		}
//    	}
//
//        @Test
//        public void testAuthorizePassingOrderParam() {
//            FederationUser federationUser = new FederationUser(-1L, null);
//            Order order = new ComputeOrder();
//            Mockito.doReturn(true).when(this.authorizationPlugin).isAuthorized(Mockito.eq(federationUser),
//                    Mockito.any(Operation.class), Mockito.eq(order));
//
//            try {
//                Operation operation = Operation.GET;
//                this.AaController.authorize(federationUser, operation, order);
//            } catch (Exception e) {
//                Assert.fail();
//            }
//        }
//
//        @SuppressWarnings("unchecked")
//		@Test
//        public void testGetLocalToken() throws UnauthorizedException, TokenCreationException, PropertyNotSpecifiedException {
//            Token localToken = Mockito.mock(Token.class);
//            Mockito.doReturn(localToken).when(this.localIdentityPlugin).createToken(Mockito.anyMap());
//            Token tokenGenarated = this.AaController.getLocalToken();
//            Assert.assertEquals(localToken, tokenGenarated);
//        }
//
//        @SuppressWarnings("unchecked")
//		@Test(expected=PropertyNotSpecifiedException.class)
//        public void testGetLocalTokenWithNoCredentials() throws PropertyNotSpecifiedException, UnauthorizedException,
//                TokenCreationException {
//            // Mocking authentication controller util
//            Properties emptyProperties = new Properties();
//            PowerMockito.mockStatic(AuthenticationControllerUtil.class);
//            given(AuthenticationControllerUtil.getDefaultLocalTokenCredentials(emptyProperties))
//                    .willThrow(PropertyNotSpecifiedException.class);
//            this.AaController.getLocalToken();
//        }
//
//}

package org.fogbowcloud.manager.core.services;

import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.manager.constants.Operation;
import org.fogbowcloud.manager.core.manager.plugins.AuthorizationPlugin;
import org.fogbowcloud.manager.core.manager.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.token.Token;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.given;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Properties;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AuthenticationControllerUtil.class})
public class AAAControllerTest {

    	private AAAController AAAController;
    	private AuthorizationPlugin authorizationPlugin;
    	private IdentityPlugin federationIdentityPlugin;
    	private IdentityPlugin localIdentityPlugin;
    	private Properties properties;

    	@Before
    	public void setUp() {
    		this.properties = new Properties();
    		this.federationIdentityPlugin = Mockito.mock(IdentityPlugin.class);
    		this.localIdentityPlugin = Mockito.mock(IdentityPlugin.class);
    		this.authorizationPlugin = Mockito.mock(AuthorizationPlugin.class);
    		this.AAAController = Mockito.spy(new AAAController(this.federationIdentityPlugin,
    				this.localIdentityPlugin, this.authorizationPlugin, this.properties));
    		PowerMockito.mockStatic(AuthenticationControllerUtil.class);
    	}

    	@Test
    	public void testAuthenticate() throws UnauthenticatedException {
    		boolean isAuthenticated = true;

    	    Mockito.doReturn(isAuthenticated).when(this.federationIdentityPlugin).isValid(Mockito.anyString());
    		this.AAAController.authenticate(Mockito.anyString());
    	}

    	@Test(expected=UnauthorizedException.class)
    	public void testAuthenticationFail() throws UnauthenticatedException {
    		Mockito.doThrow(UnauthorizedException.class).when(
    				this.federationIdentityPlugin).isValid(Mockito.anyString());
    		this.AAAController.authenticate(Mockito.anyString());
    	}

    	@Test
    	public void testAuthorizePassingOrderTypeParam() {
    	    Token federationToken = new Token();
    		Mockito.doReturn(true).when(this.authorizationPlugin).isAuthorized(Mockito.eq(federationToken),
                    Mockito.any(Operation.class), Mockito.any(OrderType.class));

    		try {
    		    Operation operation = Operation.GET;
    		    OrderType orderType = OrderType.COMPUTE;
    			this.AAAController.authorize(federationToken, operation, orderType);
    		} catch (Exception e) {
    			Assert.fail();
    		}
    	}

        @Test
        public void testAuthorizePassingOrderParam() {
            Token federationToken = new Token();
            Order order = new ComputeOrder();
            Mockito.doReturn(true).when(this.authorizationPlugin).isAuthorized(Mockito.eq(federationToken),
                    Mockito.any(Operation.class), Mockito.eq(order));

            try {
                Operation operation = Operation.GET;
                this.AAAController.authorize(federationToken, operation, order);
            } catch (Exception e) {
                Assert.fail();
            }
        }

        @Test
        public void testGetLocalToken() throws UnauthorizedException, TokenCreationException, PropertyNotSpecifiedException {
            Token localToken = Mockito.mock(Token.class);
            Mockito.doReturn(localToken).when(this.localIdentityPlugin).createToken(Mockito.anyMap());
            Token tokenGenarated = this.AAAController.getLocalToken();
            Assert.assertEquals(localToken, tokenGenarated);
        }

        @Test(expected=PropertyNotSpecifiedException.class)
        public void testGetLocalTokenWithNoCredentials() throws PropertyNotSpecifiedException, UnauthorizedException,
                TokenCreationException {
            // Mocking authentication controller util
            Properties emptyProperties = new Properties();
            PowerMockito.mockStatic(AuthenticationControllerUtil.class);
            given(AuthenticationControllerUtil.getDefaultLocalTokenCredentials(emptyProperties))
                    .willThrow(PropertyNotSpecifiedException.class);
            this.AAAController.getLocalToken();
        }

}

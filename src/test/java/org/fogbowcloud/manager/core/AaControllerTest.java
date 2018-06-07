package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import java.util.Map;
import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.behavior.authorization.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.federationidentity.FederationIdentityPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.DefaultLocalUserCredentialsMapper;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.LocalUserCredentialsMapperPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.localidentity.LocalIdentityPlugin;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.services.PluginInstantiationService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

public class AaControllerTest {

    private AaController AaController;
    private AuthorizationPlugin authorizationPlugin;
    private FederationIdentityPlugin federationIdentityPlugin;
    private LocalIdentityPlugin localIdentityPlugin;

    private PluginInstantiationService pluginInstantiationService;
    private BehaviorPluginsHolder behaviorPluginsHolder;

    @Before
    public void setUp() {
        this.localIdentityPlugin = Mockito.mock(LocalIdentityPlugin.class);
        this.authorizationPlugin = Mockito.mock(AuthorizationPlugin.class);

        this.pluginInstantiationService = PluginInstantiationService.getInstance();
        
        this.behaviorPluginsHolder =
                Mockito.spy(new BehaviorPluginsHolder(pluginInstantiationService));
        this.federationIdentityPlugin =
                Mockito.spy(this.behaviorPluginsHolder.getFederationIdentityPlugin());
        this.AaController =
                Mockito.spy(new AaController(this.localIdentityPlugin, this.behaviorPluginsHolder));
    }

    @Test
    public void testAuthenticate() throws UnauthenticatedException {
        boolean isAuthenticated = true;
        Mockito.doReturn(isAuthenticated).when(this.federationIdentityPlugin)
                .isValid(Mockito.anyString());
        this.AaController.authenticate(Mockito.anyString());
    }

    /**
     * Method 'isValid(String federationTokenValue)' 
     * in 'DefaultFederationIdentityPlugin' class, 
     * is set to always return true
     */
    @Ignore
    @Test(expected = UnauthorizedException.class)
    public void testAuthenticationFail() throws UnauthenticatedException {
        Mockito.doThrow(UnauthorizedException.class).when(this.federationIdentityPlugin)
                .isValid(Mockito.anyString());
        this.AaController.authenticate(Mockito.anyString());
    }

    @Test
    public void testAuthorizePassingOrderTypeParam() {
        FederationUser federationUser = new FederationUser("fake-user", null);
        Mockito.doReturn(true).when(this.authorizationPlugin).isAuthorized(
                Mockito.eq(federationUser), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        try {
            Operation operation = Operation.GET;
            InstanceType orderType = InstanceType.COMPUTE;
            this.AaController.authorize(federationUser, operation, orderType);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testAuthorizePassingOrderParam() {
        FederationUser federationUser = new FederationUser("fake-user", null);
        Order order = new ComputeOrder();
        Mockito.doReturn(true).when(this.authorizationPlugin).isAuthorized(
                Mockito.eq(federationUser), Mockito.any(Operation.class), Mockito.eq(order));

        try {
            Operation operation = Operation.GET;
            this.AaController.authorize(federationUser, operation, order);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    /**
     * This method was not implemented in 'KeystoneV3IdentityPlugin' class
     */
    @SuppressWarnings("unchecked")
    @Ignore
    @Test
    public void testGetLocalToken()
            throws UnauthorizedException, TokenCreationException, PropertyNotSpecifiedException {
        Token localToken = Mockito.mock(Token.class);
        Mockito.doReturn(localToken).when(this.localIdentityPlugin).createToken(Mockito.anyMap());
        Token tokenGenarated = this.AaController.getLocalToken(null);
        Assert.assertEquals(localToken, tokenGenarated);
    }

    @Test(expected = PropertyNotSpecifiedException.class)
    public void testGetLocalTokenWithNoCredentials()
            throws PropertyNotSpecifiedException, UnauthorizedException, TokenCreationException {
        FederationUser federationUser = Mockito.mock(FederationUser.class);
        LocalUserCredentialsMapperPlugin localUserCredentialsMapperPlugin =
                new DefaultLocalUserCredentialsMapper();
        Map<String, String> userCredentials =
                localUserCredentialsMapperPlugin.getCredentials(federationUser);
        Mockito.doReturn(userCredentials).when(localUserCredentialsMapperPlugin)
                .getCredentials(federationUser);
        Mockito.doThrow(PropertyNotSpecifiedException.class).when(this.AaController)
                .getLocalToken(federationUser);
    }

}

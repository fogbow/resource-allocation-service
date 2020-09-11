package cloud.fogbow.ras.core.plugins.authorization;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.Order;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest({ DatabaseManager.class })
public class DefaultAuthorizationPluginTest extends BaseUnitTests {

    private AuthorizationPlugin<RasOperation> authorizationPlugin;

    @Before
    public void setUp() throws FogbowException {
        this.authorizationPlugin = new DefaultAuthorizationPlugin();
        this.testUtils.mockReadOrdersFromDataBase();
    }

    // test case: When calling the isAuthorized method with resource type
    // different from COMPUTE, NETWORK, ATTACHMENT, VOLUME or PUBLIC_IP, it must
    // throw an UnauthorizedRequestException;
    @Test
    public void testAuthorizeOrderWithDifferentResourceType() {
        // set up
        Order order = this.testUtils.createLocalComputeOrder();
        SystemUser requester = order.getSystemUser();

        String expected = Messages.Exception.MISMATCHING_RESOURCE_TYPE;

        try {
            // exercise
            this.authorizationPlugin.isAuthorized(requester, new RasOperation(null,
                    ResourceType.INVALID_RESOURCE, null, order));
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    // test case: When calling the authorizeOrder method with requester
    // different from order owner, it must throws an UnauthorizedRequestException;
    @Test
    public void testAuthorizeOrderWithDifferentRequester() {
        // set up
        SystemUser requester = Mockito.mock(SystemUser.class);
        Order order = this.testUtils.createLocalComputeOrder();

        String expected = Messages.Exception.REQUESTER_DOES_NOT_OWN_REQUEST;

        try {
            // exercise
            this.authorizationPlugin.isAuthorized(requester, new RasOperation(null,
                    ResourceType.COMPUTE, null, order));
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
}

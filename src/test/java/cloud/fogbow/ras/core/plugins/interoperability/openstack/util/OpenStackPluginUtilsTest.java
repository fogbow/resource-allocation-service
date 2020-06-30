package cloud.fogbow.ras.core.plugins.interoperability.openstack.util;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.TestUtils;
import org.junit.Assert;
import org.junit.Test;

public class OpenStackPluginUtilsTest {

    private static final String FAKE_PROJECT_ID = "fake-project-id";
    private static final String FAKE_TOKEN_VALUE = "fake-token-value";

    @Test
    public void testGetProjectIdFromCloudUserFail() {
        // set up
        OpenStackV3User cloudUser = createOpenStackUser();
        cloudUser.setProjectId(null);

        String expected = Messages.Exception.NO_PROJECT_ID;

        try {
            // exercise
            OpenStackPluginUtils.getProjectIdFrom(cloudUser);
            Assert.fail();
        } catch (Exception e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    // test case: given a cloudUser with projectId field, should return the
    // projectId
    @Test
    public void testGetProjectId() throws InvalidParameterException {
        // set up
        OpenStackV3User cloudUser = createOpenStackUser();

        // exercise
        String projectId = OpenStackPluginUtils.getProjectIdFrom(cloudUser);

        // verify
        Assert.assertEquals(this.FAKE_PROJECT_ID, projectId);
    }

    private OpenStackV3User createOpenStackUser() {
        String userId = TestUtils.FAKE_USER_ID;
        String userName = TestUtils.FAKE_USER_NAME;
        String tokenValue = FAKE_TOKEN_VALUE;
        String projectId = FAKE_PROJECT_ID;
        return new OpenStackV3User(userId, userName, tokenValue, projectId);
    }
}
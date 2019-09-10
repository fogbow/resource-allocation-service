package cloud.fogbow.ras.core.plugins.interoperability.opennebula;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

@Ignore
@RunWith(PowerMockRunner.class)
public class OpenNebulaBaseTests extends BaseUnitTests {

    private static final String CLOUD_NAME = "opennebula";
    private static final String FAKE_USER_NAME = "fake-user-name";
    private static final String LOCAL_TOKEN_VALUE = "user:password";

    protected String openNebulaConfFilePath;
    protected CloudUser cloudUser;

    @Before
    public void setUp() throws FogbowException {
        this.testUtils.mockReadOrdersFromDataBase();
        this.cloudUser = this.createCloudUser();

        this.openNebulaConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
                + File.separator + CLOUD_NAME + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        PowerMockito.mockStatic(OpenNebulaClientUtil.class);
    }

    private CloudUser createCloudUser() {
        String userId = null;
        String userName = FAKE_USER_NAME;
        String tokenValue = LOCAL_TOKEN_VALUE;

        return new CloudUser(userId, userName, tokenValue);
    }
}

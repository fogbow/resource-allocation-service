package cloud.fogbow.ras.core.plugins.interoperability.opennebula;

import cloud.fogbow.common.exceptions.*;
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
    protected String openNebulaConfFilePath;

    @Before
    public void setUp() throws FogbowException {
        this.testUtils.mockReadOrdersFromDataBase();

        this.openNebulaConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
                + File.separator + CLOUD_NAME + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        PowerMockito.mockStatic(OpenNebulaClientUtil.class);
    }
}

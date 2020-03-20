package cloud.fogbow.ras.core.plugins.interoperability.azure.attachment;

import java.io.File;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.microsoft.azure.management.Azure;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.AzureClientCacheManager;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.attachment.sdk.AzureAttachmentOperationSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureStateMapper;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Azure.class, AzureClientCacheManager.class, AzureGeneralUtil.class })
public class AzureAttachmentPluginTest {

    private String defaultRegionName;
    private String defaultResourceGroupName;
    private AzureAttachmentOperationSDK operation;
    private AzureAttachmentPlugin plugin;
    private AzureUser azureUser;

    @Before
    public void setUp() throws Exception {
        String azureConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
                + File.separator + AzureTestUtils.AZURE_CLOUD_NAME + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        Properties properties = PropertiesUtil.readProperties(azureConfFilePath);
        this.defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
        this.defaultResourceGroupName = properties.getProperty(AzureConstants.DEFAULT_RESOURCE_GROUP_NAME_KEY);
        this.operation = Mockito.mock(AzureAttachmentOperationSDK.class);
        this.plugin = Mockito.spy(new AzureAttachmentPlugin(azureConfFilePath));
        this.plugin.setOperation(this.operation);
        this.azureUser = AzureTestUtils.createAzureUser();
    }

    // test case: When calling the isReady method and the instance state is
    // Attached, it must verify that returns the true value.
    @Test
    public void testIsReadyWhenInstanceStateIsAttached() {
        // set up
        String instanceState = AzureStateMapper.ATTACHED_STATE;

        // exercise
        boolean status = this.plugin.isReady(instanceState);

        // verify
        Assert.assertTrue(status);
    }

    // test case: When calling the isReady method and the instance state is not
    // Attached, it must verify than returns false value.
    @Test
    public void testIsReadyWhenInstanceStateIsNotAttached() {
        // set up
        String[] instanceStates = { AzureStateMapper.FAILED_STATE, AzureStateMapper.UNATTACHED_STATE };

        for (String instanceState : instanceStates) {
            // exercise
            boolean status = this.plugin.isReady(instanceState);

            // verify
            Assert.assertFalse(status);
        }
    }

    // test case: When calling the hasFailed method and the instance state is
    // Unattached or failed, it must verify than returns true value.
    @Test
    public void testHasFailedWhenInstanceStateIsFailed() {
        // set up
        String[] instanceStates = { AzureStateMapper.FAILED_STATE, AzureStateMapper.UNATTACHED_STATE };

        for (String instanceState : instanceStates) {
            // exercise
            boolean status = this.plugin.hasFailed(instanceState);

            // verify
            Assert.assertTrue(status);
        }
    }

    // test case: When calling the hasFailed method and the instance state is not
    // failed, it must verify than returns false value.
    @Test
    public void testHasFailedWhenInstanceStateIsNotFailed() {
        // set up
        String instanceState = AzureStateMapper.ATTACHED_STATE;

        // exercise
        boolean status = this.plugin.hasFailed(instanceState);

        // verify
        Assert.assertFalse(status);
    }
}

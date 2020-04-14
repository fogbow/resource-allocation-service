package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.ras.constants.SystemConstants;
import org.junit.Assert;
import org.junit.Test;

public class AzureGeneralUtilTest {

    // test case: When calling the generateResourceName method,
    // it must verify if it returns a string with prefix and a limited length.
    @Test
    public void testGenerateResourceNameSuccessfully() {
        // exercise
        String resourceName = AzureGeneralUtil.generateResourceName();

        // verify
        Assert.assertTrue(resourceName.startsWith(SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX));
        Assert.assertTrue(resourceName.length() > SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX.length());
        Assert.assertTrue(resourceName.length() <= AzureConstants.MAXIMUM_RESOURCE_NAME_LENGTH);
    }

    // test case: When calling the defineResourceName method,
    // it must verify if it returns resourceName value equals to instanceId.
    @Test
    public void testDefineResourceNameSuccessfully() {
        // set up
        String instanceId = "instanceId";

        // exercise
        String resourceName = AzureGeneralUtil.defineResourceName(instanceId);

        // verify
        Assert.assertEquals(instanceId, resourceName);
    }

    // test case: When calling the defineInstanceId method,
    // it must verify if it returns instanceId value equals to resourceName.
    @Test
    public void testDefineInstanceIdSuccessfully() {
        // set up
        String resourceName = "resourceName";

        // exercise
        String instanceId = AzureGeneralUtil.defineInstanceId(resourceName);

        // verify
        Assert.assertEquals(resourceName, instanceId);
    }

}

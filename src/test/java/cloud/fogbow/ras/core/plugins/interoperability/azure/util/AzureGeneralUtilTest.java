package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.UnacceptableOperationException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.LoggerAssert;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;

import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.microsoft.azure.management.Azure;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Azure.class, AzureResourceGroupOperationUtil.class })
public class AzureGeneralUtilTest {

    private LoggerAssert loggerAssert;

    @Before
    public void setUp() {
        this.loggerAssert = new LoggerAssert(AzureGeneralUtil.class);
    }

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

    // test case: When calling the defineResourceGroupName method, and the
    // creation of the resource group is successful, it must return the name of
    // the created resource group.
    @Test
    public void testDefineResourceGroupNameReturnResourceName() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String regionName = "brazilsouth";
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String defaultResourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;

        PowerMockito.mockStatic(AzureResourceGroupOperationUtil.class);
        PowerMockito.doReturn(resourceName).when(AzureResourceGroupOperationUtil.class, "createResourceGroup",
                Mockito.eq(azure), Mockito.eq(regionName), Mockito.eq(resourceName));

        // exercise
        String resourceGroupName = AzureGeneralUtil
                .defineResourceGroupName(azure, regionName, resourceName, defaultResourceGroupName);

        // verify
        PowerMockito.verifyStatic(AzureResourceGroupOperationUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureResourceGroupOperationUtil.createResourceGroup(Mockito.eq(azure), Mockito.eq(regionName),
                Mockito.eq(resourceName));

        Assert.assertSame(resourceName, resourceGroupName);
    }

    // test case: When calling the defineResourceGroupName method and a problem
    // occurs when exceeding the creation limit for this resource, it must
    // return the name of the default resource group.
    @Test
    public void testDefineResourceGroupNameReturnDefaultResourceGroupName() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String regionName = "brazilsouth";
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String defaultResourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;

        Exception exception =new UnacceptableOperationException(Messages.Exception.RESOURCE_GROUP_LIMIT_EXCEEDED);
        PowerMockito.mockStatic(AzureResourceGroupOperationUtil.class);
        PowerMockito.doThrow(exception).when(AzureResourceGroupOperationUtil.class, "createResourceGroup",
                Mockito.eq(azure), Mockito.eq(regionName), Mockito.eq(resourceName));

        // exercise
        String resourceGroupName = AzureGeneralUtil
                .defineResourceGroupName(azure, regionName, resourceName, defaultResourceGroupName);

        // verify
        PowerMockito.verifyStatic(AzureResourceGroupOperationUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureResourceGroupOperationUtil.createResourceGroup(Mockito.eq(azure), Mockito.eq(regionName),
                Mockito.eq(resourceName));

        this.loggerAssert
                .assertEqualsInOrder(Level.WARN, String.format(Messages.Warn.RESOURCE_CREATION_FAILED_S, exception))
                .assertEqualsInOrder(Level.INFO, Messages.Info.CHANGE_TO_DEFAULT_RESOURCE_GROUP);

        Assert.assertEquals(defaultResourceGroupName, resourceGroupName);
    }

    // test case: When calling the selectResourceGroupName method, from an existing
    // resource group, it must verify that it returned the same resource name
    // passed by parameter.
    @Test
    public void testSelectResourceGroupNameReturnResourceName() throws Exception {
        //set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String defaultResourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;

        PowerMockito.mockStatic(AzureResourceGroupOperationUtil.class);
        PowerMockito.doReturn(true).when(AzureResourceGroupOperationUtil.class, "existsResourceGroup", Mockito.eq(azure),
                Mockito.eq(resourceName));

        // exercise
        String result = AzureGeneralUtil.selectResourceGroupName(azure, resourceName, defaultResourceGroupName);

        // verify
        PowerMockito.verifyStatic(AzureResourceGroupOperationUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureResourceGroupOperationUtil.existsResourceGroup(Mockito.eq(azure), Mockito.eq(resourceName));

        Assert.assertSame(resourceName, result);
    }

    // test case: When calling the getResourceGroupName method, from a
    // non-existent resource group, it must return the default resource group
    // name.
    @Test
    public void testGetResourceGroupNameReturnDefaultResourceGroupName() throws Exception {
        //set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String defaultResourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;

        PowerMockito.mockStatic(AzureResourceGroupOperationUtil.class);
        PowerMockito.doReturn(false).when(AzureResourceGroupOperationUtil.class, "existsResourceGroup", Mockito.eq(azure),
                Mockito.eq(resourceName));

        // exercise
        String result = AzureGeneralUtil.selectResourceGroupName(azure, resourceName, defaultResourceGroupName);

        // verify
        PowerMockito.verifyStatic(AzureResourceGroupOperationUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureResourceGroupOperationUtil.existsResourceGroup(Mockito.eq(azure), Mockito.eq(resourceName));

        Assert.assertEquals(defaultResourceGroupName, result);
    }
}

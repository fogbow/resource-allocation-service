package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.QuotaExceededException;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.ResourceGroup.DefinitionStages;
import com.microsoft.azure.management.resources.ResourceGroups;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Azure.class, AzureResourceGroupOperationUtil.class })
public class AzureResourceGroupOperationUtilTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    // test case: When calling the createResourceGroup method and an unexpected
    // error occurs, it must verify than a QuotaExceededException has been
    // thrown.
    @Test
    public void testCreateResourceGroupFail() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String regionName = AzureTestUtils.DEFAULT_REGION_NAME;
        String resourceGroupName = AzureTestUtils.RESOURCE_NAME;

        PowerMockito.mockStatic(AzureResourceGroupOperationUtil.class);
        PowerMockito.doCallRealMethod().when(AzureResourceGroupOperationUtil.class, "createResourceGroup",
                Mockito.eq(azure), Mockito.eq(regionName), Mockito.eq(resourceGroupName));

        // verify
        this.expectedException.expect(QuotaExceededException.class);

        // exercise
        AzureResourceGroupOperationUtil.createResourceGroup(azure, regionName, resourceGroupName);
    }

    // test case: When calling the createResourceGroup method and the limit for
    // creating a resource group has not been exceeded, it must verify that the
    // call was successful.
    @Test
    public void testCreateResourceGroupSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String regionName = AzureTestUtils.DEFAULT_REGION_NAME;
        String resourceGroupName = AzureTestUtils.RESOURCE_NAME;

        PowerMockito.mockStatic(AzureResourceGroupOperationUtil.class);
        PowerMockito.doCallRealMethod().when(AzureResourceGroupOperationUtil.class, "createResourceGroup",
                Mockito.eq(azure), Mockito.eq(regionName), Mockito.eq(resourceGroupName));

        ResourceGroups resourceGroups = Mockito.mock(ResourceGroups.class);
        Mockito.when(azure.resourceGroups()).thenReturn(resourceGroups);
        
        DefinitionStages.Blank definitionStagesBlank = Mockito.mock(DefinitionStages.Blank.class);
        Mockito.when(resourceGroups.define(Mockito.eq(resourceGroupName))).thenReturn(definitionStagesBlank);
        
        DefinitionStages.WithCreate definitionStagesWithCreate = Mockito.mock(DefinitionStages.WithCreate.class);
        Mockito.when(definitionStagesBlank.withRegion(Mockito.eq(regionName))).thenReturn(definitionStagesWithCreate);

        ResourceGroup resourceGroup = Mockito.mock(ResourceGroup.class);
        Mockito.when(definitionStagesWithCreate.create()).thenReturn(resourceGroup);

        // exercise
        AzureResourceGroupOperationUtil.createResourceGroup(azure, regionName, resourceGroupName);

        // verify
        Mockito.verify(azure, Mockito.times(TestUtils.RUN_ONCE)).resourceGroups();
        Mockito.verify(resourceGroups, Mockito.times(TestUtils.RUN_ONCE)).define(Mockito.eq(resourceGroupName));
        Mockito.verify(definitionStagesBlank, Mockito.times(TestUtils.RUN_ONCE)).withRegion(Mockito.eq(regionName));
        Mockito.verify(definitionStagesWithCreate, Mockito.times(TestUtils.RUN_ONCE)).create();
        Mockito.verify(resourceGroup, Mockito.times(TestUtils.RUN_ONCE)).name();
    }

    // test case: When calling the existingResourceGroup method, it must verify
    // that the call was successful.
    @Test
    public void testExistsResourceGroupReturnedTrueValue() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceGroupName = AzureTestUtils.RESOURCE_NAME;

        PowerMockito.mockStatic(AzureResourceGroupOperationUtil.class);
        PowerMockito.doCallRealMethod().when(AzureResourceGroupOperationUtil.class, "existsResourceGroup",
                Mockito.eq(azure), Mockito.eq(resourceGroupName));

        ResourceGroups resourceGroups = Mockito.mock(ResourceGroups.class);
        Mockito.when(azure.resourceGroups()).thenReturn(resourceGroups);

        // exercise
        AzureResourceGroupOperationUtil.existsResourceGroup(azure, resourceGroupName);

        // verify
        Mockito.verify(azure, Mockito.times(TestUtils.RUN_ONCE)).resourceGroups();
        Mockito.verify(resourceGroups, Mockito.times(TestUtils.RUN_ONCE))
                .checkExistence(Mockito.eq(resourceGroupName));
    }

    // test case: When calling the deleteResourceGroupAsync method, it must
    // verify that the call was successful.
    @Test
    public void testDeleteResourceGroupAsyncSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceGroupName = AzureTestUtils.RESOURCE_NAME;

        PowerMockito.mockStatic(AzureResourceGroupOperationUtil.class);
        PowerMockito.doCallRealMethod().when(AzureResourceGroupOperationUtil.class, "deleteResourceGroupAsync",
                Mockito.eq(azure), Mockito.eq(resourceGroupName));

        ResourceGroups resourceGroups = Mockito.mock(ResourceGroups.class);
        Mockito.when(azure.resourceGroups()).thenReturn(resourceGroups);

        // exercise
        AzureResourceGroupOperationUtil.deleteResourceGroupAsync(azure, resourceGroupName);

        // verify
        Mockito.verify(azure, Mockito.times(TestUtils.RUN_ONCE)).resourceGroups();
        Mockito.verify(resourceGroups, Mockito.times(TestUtils.RUN_ONCE))
                .deleteByNameAsync(Mockito.eq(resourceGroupName));
    }

}

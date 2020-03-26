package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9.CidrUtils;
import com.microsoft.azure.management.resources.fluentcore.utils.SdkContext;

import javax.annotation.Nullable;

public interface AzureGeneralUtil {

    // Generate a pattern name to all Resources that it will be created in the cloud
    static String generateResourceName() {
        return SdkContext.randomResourceName(SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX, AzureConstants.MAXIMUM_RESOURCE_NAME_LENGTH);
    }

    // Define the Fogbow instance-id as Azure resource name
    static String defineInstanceId(String resourceName) {
        return resourceName;
    }

    // Define the Azure resource name as Fogbow instance-id
    static String defineResourceName(String instanceId) {
        return instanceId;
    }

}

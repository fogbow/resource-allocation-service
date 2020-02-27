package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.Order;

public class AzureResourceIdBuilder {

    public static AzureResourceIdConfigured configure() {
        return new AzureResourceIdConfigured();
    }
    
    public static AzureResourceIdConfigured configure(String structure) {
        return new AzureResourceIdConfigured(structure);
    }

    public static class AzureResourceIdConfigured {
        private String structure;
        private String subscriptionId;
        private String resourceGroupName;
        private String resourceName;
        
        public AzureResourceIdConfigured() {}
        
        public AzureResourceIdConfigured(String structure) {
            this.structure = structure;
        }
        
        public AzureResourceIdConfigured withSubscriptionId(String subscriptionId) {
            this.subscriptionId = subscriptionId;
            return this;
        }

        public AzureResourceIdConfigured withResourceGroupName(String resourceGroupName) {
            this.resourceGroupName = resourceGroupName;
            return this;
        }

        public AzureResourceIdConfigured withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public String buildResourceId() {
            return String.format(this.structure, this.subscriptionId, this.resourceGroupName, this.resourceName);
        }

        /**
         * It checks the resource name size in relation to Database Instance Order Id Maximum Size.
         * It happens because the resource name makes up the instance Id. Also, it might happen due
         * to the fact that the user choose some values such as resourceName and resourceGroupName.
         */
        public void checkIdSizePolicy() throws InvalidParameterException {
            this.structure = AzureConstants.BIGGER_STRUCTURE;
            String idBuilt = this.buildResourceId();
            int sizeExceeded = idBuilt.length() - Order.FIELDS_MAX_SIZE;
            if (sizeExceeded > 0) {
                throw new InvalidParameterException(
                        String.format(Messages.Error.ERROR_ID_LIMIT_SIZE_EXCEEDED, sizeExceeded));
            }
        }

    }

}

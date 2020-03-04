package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.ras.constants.Messages;

public class AzureResourceIdBuilder {

    public static AzureResourceIdConfigured configure() {
        return new AzureResourceIdConfigured();
    }
    
    public static AzureResourceIdConfigured configure(String structure) {
        return new AzureResourceIdConfigured(structure);
    }

    public static class AzureResourceIdConfigured {
        
        private static final int MAXIMUM_RESOURCE_NAME_LENGTH = 80;
        
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

        public String build() {
            return String.format(this.structure, this.subscriptionId, this.resourceGroupName, this.resourceName);
        }

        /**
         * Checks the size of the resource name according to the limit stipulated by the
         * Azure cloud.
         */
        public void checkSizePolicy() throws InvalidParameterException {
            int sizeExceeded = this.resourceName.length() - MAXIMUM_RESOURCE_NAME_LENGTH;
            if (sizeExceeded > 0) {
                String message = String.format(Messages.Error.ERROR_ID_LIMIT_SIZE_EXCEEDED, sizeExceeded);
                throw new InvalidParameterException(message);
            }
        }

    }

}

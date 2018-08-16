package org.fogbowcloud.manager.core.plugins.cloud.cloudstack;

public class CloudStackRestApiConstants {

    public static class Compute {
        public static final String SERVER_KEY_JSON = "virtualmachine";
        public static final String ID_KEY_JSON = "id";
        public static final String NAME_KEY_JSON = "name";
        // NOTE(pauloewerton): Disk property does not come with the compute response.
        // Leaving it here for future reference.
        public static final String DISK_KEY_JSON = "disksize";
        public static final String MEMORY_KEY_JSON = "memory";
        public static final String VCPUS_KEY_JSON = "cpunumber";

        public static final String ADDRESSES_KEY_JSON = "nic";
        // NOTE(pauloewerton): CloudStack has no integrated flavor concept. Service offering (compute "flavor") is
        // the only flavor property returned by the compute response in my tests.
        public static final String FLAVOR_KEY_JSON = "serviceofferingid";
        public static final String STATUS_KEY_JSON = "state";
        public static final String ADDRESS_KEY_JSON = "ipaddress";
    }
    
    public static class Volume {
        public static final String LIST_DISK_OFFERING_RESPONSE_KEY_JSON = "listdiskofferingsresponse";
        public static final String DISK_OFFERING_KEY_JSON = "diskoffering";
        public static final String ID_KEY_JSON = "id";
        public static final String DISK_SIZE_KEY_JSON = "disksize";
        public static final String CUSTOMIZED_KEY_JSON = "iscustomized";
    }
}
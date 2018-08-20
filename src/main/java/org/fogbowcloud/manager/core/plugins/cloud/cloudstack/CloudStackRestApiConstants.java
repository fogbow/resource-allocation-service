package org.fogbowcloud.manager.core.plugins.cloud.cloudstack;

public class CloudStackRestApiConstants {

    public static class Compute {

        public static final String VIRTUAL_MACHINES_KEY_JSON = "listvirtualmachinesresponse";
        public static final String VIRTUAL_MACHINE_KEY_JSON = "virtualmachine";
        public static final String ID_KEY_JSON = "id";
        public static final String NAME_KEY_JSON = "name";
        public static final String STATE_KEY_JSON = "state";
        public static final String CPU_NUMBER_KEY_JSON = "cpunumber";
        public static final String MEMORY_KEY_JSON = "memory";
        public static final String NIC_KEY_JSON = "nic";
        public static final String IP_ADDRESS_KEY_JSON = "ipaddress";
    }

    public static class Volume {

        public static final String VOLUMES_KEY_JSON = "listvolumesresponse";
        public static final String VOLUME_KEY_JSON = "volume";
        public static final String DISK_KEY_JSON = "disksize";
        public static final String CREATE_VOLUME_KEY_JSON = "createvolumeresponse";
        public static final String CUSTOMIZED_KEY_JSON = "iscustomized";
        public static final String DELETE_VOLUME_KEY_JSON = "deletevolumeresponse";
        public static final String DISK_OFFERING_KEY_JSON = "diskoffering";
        public static final String DISK_OFFERINGS_KEY_JSON = "listdiskofferingsresponse";
        public static final String DISPLAY_TEXT_KEY_JSON = "displaytext";
        public static final String ID_KEY_JSON = "id";
        public static final String JOB_ID_KEY_JSON = "jobid";
        public static final String NAME_KEY_JSON = "name";
        public static final String SIZE_KEY_JSON = "size";
        public static final String STATE_KEY_JSON = "state";
        public static final String SUCCESS_KEY_JSON = "success";
    }

    public static class Network {

        public static final String NETWORKS_KEY_JSON = "listnetworksresponse";
        public static final String NETWORK_KEY_JSON = "network";

    }

}
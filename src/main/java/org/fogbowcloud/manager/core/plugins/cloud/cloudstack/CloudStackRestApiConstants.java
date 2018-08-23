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

    public static class Network {

        public static final String NETWORKS_KEY_JSON = "listnetworksresponse";
        public static final String CREATE_NETWORK_RESPONSE_KEY_JSON = "createnetworkresponse";
        public static final String NETWORK_KEY_JSON = "network";
        public static final String ID_KEY = "id";
    }
}

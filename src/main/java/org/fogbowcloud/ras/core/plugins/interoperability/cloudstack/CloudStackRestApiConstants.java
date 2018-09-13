package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack;

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
        public static final String LIST_SERVICE_OFFERINGS_KEY_JSON = "listserviceofferingsresponse";
        public static final String SERVICE_OFFERING_KEY_JSON = "serviceoffering";
        public static final String JOB_RESULT_KEY_JSON = "jobresult";
        public static final String DEPLOY_VIRTUAL_MACHINE = "deployvirtualmachineresponse";
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
        public static final String CREATE_NETWORK_RESPONSE_KEY_JSON = "createnetworkresponse";

        public static final String NETWORK_KEY_JSON = "network";
        public static final String ID_KEY = "id";
    }

    public static class PublicIp {

        public static final String ASSOCIATE_IP_ADDRESS_COMMAND = "associateIpAddress";
        public static final String QUERY_ASYNC_JOB_RESULT = "queryAsyncJobResult";
        public static final String ENABLE_STATIC_NAT_COMMAND = "enableStaticNat";

        // TODO confirm this values !
        public static final String ASSOCIATE_IP_ADDRESS_RESPONSE_KEY_JSON = "associateipaddressresponse";
        public static final String QUERY_ASYNC_JOB_RESULT_KEY_JSON = "queryasyncjobresultresponse";

        public static final String VM_ID_KEY_JSON = "virtualmachineid";
        public static final String NETWORK_ID_KEY_JSON = "networkid";
        public static final String IP_ADDRESS_KEY_JSON = "ipaddress";
        public static final String IP_ADDRESS_ID_KEY_JSON = "ipaddressid";

        public static final String PROTOCOL_KEY_JSON = "protocol";
        public static final String STARTPORT_KEY_JSON = "startport";

        public static final String ENDPORT_KEY_JSON = "endport";

        public static final String ID_KEY_JSON = "id";
        public static final String JOB_ID_KEY_JSON = "jobid";
        public static final String JOB_RESULT_KEY_JSON = "jobresult";
        public static final String JOB_STATUS_KEY_JSON = "jobstatus";

    }

    public static class Attachment {

        public static final String ATTACH_VOLUME_KEY_JSON = "attachvolumeresponse";
        public static final String DETACH_VOLUME_KEY_JSON = "detachvolumeresponse";
        public static final String JOB_ID_KEY_JSON = "jobid";
        public static final String QUERY_ASYNC_JOB_RESULT_KEY_JSON = "queryasyncjobresultresponse";
        public static final String JOB_STATUS_KEY_JSON = "jobstatus";
        public static final String JOB_RESULT_KEY_JSON = "jobresult";
        public static final String VOLUME_KEY_JSON = "volume";
        public static final String ID_KEY_JSON = "id";
        public static final String DEVICE_ID_KEY_JSON = "deviceid";
        public static final String VIRTUAL_MACHINE_ID_KEY_JSON = "virtualmachineid";
        public static final String STATE_KEY_JSON = "state";
    }

    public static class Quota {

        public static final String LIST_RESOURCE_LIMITS_KEY_JSON = "listresourcelimitsresponse";
        public static final String RESOURCE_LIMIT_KEY_JSON = "resourcelimit";
        public static final String RESOURCE_TYPE_KEY_JSON = "resourcetype";
        public static final String MAX_KEY_JSON = "max";
    }
}
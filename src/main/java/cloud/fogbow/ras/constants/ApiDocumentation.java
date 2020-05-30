package cloud.fogbow.ras.constants;

public class ApiDocumentation {
    public static class ApiInfo {
        public static final String API_TITLE = "Fogbow Resource Allocation Service API";
        public static final String API_DESCRIPTION =
                "This documentation introduces readers to Fogbow RAS REST API, provides guidelines on\n" +
                        "how to use it, and describes the available features accessible from it.";
    }

    public static class Attachment {
        public static final String API = "Manages attachments.";
        public static final String CREATE_OPERATION = "Creates an attachment.";
        public static final String GET_OPERATION = "Lists all attachments created by the user (their state may be stale).";
        public static final String GET_BY_ID_OPERATION = "Lists a specific attachment.";
        public static final String DELETE_OPERATION = "Deletes a specific attachment.";
        public static final String ID = "The ID of the specific attachment.";

        public static final String CREATE_REQUEST_BODY =
                "The body of the request must specify the ID of the volume and the ID of the " +
                "compute instance where the volume will be attached; optionally, it may specify " +
                "the name of the device that is going to be created in the compute.";
    }

    public static class Cloud {
        public static final String API = "Queries the names of the clouds managed by the RAS.";
        public static final String GET_OPERATION = "Returns the names of the clouds managed by the RAS.";
        public static final String GET_OPERATION_FOR_PROVIDER = "Returns the names of the clouds managed by the RAS " +
                "whose ID is indicated in the request.";
    }

    public static class Compute {
        public static final String API = "Manages compute instances.";
        public static final String CREATE_OPERATION = "Creates a compute instance.";
        public static final String GET_OPERATION = "Lists all compute instances created by the user (their state may be stale).";
        public static final String GET_BY_ID_OPERATION = "Lists a specific compute instance.";
        public static final String DELETE_OPERATION = "Deletes a specific compute instance.";
        public static final String GET_ALLOCATION = "Gets the current compute allocation for the user on a particular provider.";
        public static final String ID = "The ID of the specific compute instance.";
        public static final String CREATE_REQUEST_BODY = "The body of the request is quite complex; please, have a look at the model description.";
    }

    public static class Image {
        public static final String API = "Queries images.";
        public static final String GET_OPERATION = "Lists all images available to the user at the indicated provider and cloud.";
        public static final String GET_BY_ID_OPERATION = "Lists a specific image available to the user at the indicated provider and cloud.";
        public static final String ID = "The ID of the specific image.";
    }

    public static class Network {
        public static final String API = "Manages private networks.";
        public static final String CREATE_OPERATION = "Creates a private network.";
        public static final String GET_OPERATION = "Lists all private networks created by the user (their state may be stale).";
        public static final String GET_BY_ID_OPERATION = "Lists a specific private network.";
        public static final String DELETE_OPERATION = "Deletes a specific private network.";
        public static final String ID = "The ID of the specific private network.";
        public static final String CREATE_REQUEST_BODY =
                "The body of the request must specify the CIDR of the network to be created, " +
                "the allocation mode of the network, the gateway address of the " +
                "network; optionally, it may specify the provider and the cloud where the " +
                "network should be created, and a name to be assigned to the network.";
        public static final String CREATE_SECURITY_RULE_OPERATION = "Creates a security rule for the network.";
        public static final String CREATE_SECURITY_RULE_REQUEST_BODY =
                "The body of the request must specify the ID of the network to which this security rule will be associated.";
        public static final String GET_SECURITY_RULE_OPERATION = "Lists all security rules associated to the network.";
        public static final String DELETE_SECURITY_RULE_OPERATION = "Deletes a specific security rule.";
        public static final String SECURITY_RULE_ID = "The ID of the specific security rule.";
        public static final String GET_ALLOCATION = "Gets the current network allocation for the user on a particular provider.";
    }

    public static class PublicIp {
        public static final String API = "Manages public IPs.";
        public static final String CREATE_OPERATION = "Creates a public IP and attaches it to a compute instance.";
        public static final String GET_OPERATION = "Lists all public IPs created by the user (their state may be stale).";
        public static final String GET_BY_ID_OPERATION = "Lists a specific public IP.";
        public static final String DELETE_OPERATION = "Deletes a specific public IP.";
        public static final String ID = "The ID of the specific public IP.";
        public static final String CREATE_REQUEST_BODY =
                "The body of the request must specify the ID of the compute to which this IP will be assigned.";
        public static final String CREATE_SECURITY_RULE_OPERATION = "Creates a security rule for the public IP.";
        public static final String CREATE_SECURITY_RULE_REQUEST_BODY =
                "The body of the request must specify the ID of the public IP to which this security rule will be associated.";
        public static final String GET_SECURITY_RULE_OPERATION = "Lists all security rules associated to the public IP.";
        public static final String DELETE_SECURITY_RULE_OPERATION = "Deletes a specific security rule.";
        public static final String SECURITY_RULE_ID = "The ID of the specific security rule.";
        public static final String GET_ALLOCATION = "Gets the current public ip allocation for the user on a particular provider.";
    }
    
    public static class Quota {
        public static final String API = "Manages resource quota.";
        public static final String GET_QUOTA = "Gets the resources quotas for the user on a particular provider.";
    }

    public static class Volume {
        public static final String API = "Manages volumes.";
        public static final String CREATE_OPERATION = "Creates a volume.";
        public static final String GET_OPERATION = "Lists all volumes created by the user (their state may be stale).";
        public static final String GET_BY_ID_OPERATION = "Lists a specific volume.";
        public static final String DELETE_OPERATION = "Deletes a specific volume.";
        public static final String ID = "The ID of the specific volume.";
        public static final String CREATE_REQUEST_BODY =
                "The body of the request must specify the size of the volume to be " +
                "created; optionally, it may specify the provider and the cloud where the " +
                "volume should be created, and a name to be assigned to the volume.";
        public static final String GET_ALLOCATION = "Gets the current volume allocation for the user on a particular provider.";
    }

    public static class CommonParameters {
        public static final String PROVIDER_ID = "The ID of the specific target provider.";
        public static final String CLOUD_NAME = "The name of the specific target cloud.";
    }

    public static class Model {
        public static final String COMPUTE_ID = "8537af26-72ee-461a-99a9-1e5d59076a98";
        public static final String VOLUME_ID = "d3c38f2d-03d4-4489-82a0-62c8581c633d";
        public static final String DEVICE = "/dev/b";
        public static final String DEVICE_NOTE = "(in some cloud orchestrators this parameter is ignored)";
        public static final String PROVIDER = "provider-name.domain";
        public static final String PROVIDER_NOTE = "(the provider that will allocate the resource)";
        public static final String CLOUD_NAME = "cloud-name";
        public static final String CLOUD_NAME_NOTE = "(the cloud where the compute is to be created)";
        public static final String COMPUTE_NAME = "my compute";
        public static final String COMPUTE_NAME_NOTE = "(a friendly name to identify the compute)";
        public static final String VCPU_NOTE = "(the minimum number of vCPUs in the compute)";
        public static final String RAM_NOTE = "(the minimum RAM size of the compute in Mega bytes)";
        public static final String DISK_NOTE = "(the minimum disk size of the compute in Giga bytes)";
        public static final String IMAGE_ID = "4489-82a0-622ee-461a-99a9-1e5d59076a98";
        public static final String IMAGE_ID_NOTE = "(the ID of the image to be used to create the compute)";
        public static final String SSH_PUBLIC_KEY = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDesnSKdu1oTNYXNo8HXGf/0c8iI3qHmjdGJB7KFNtRCUJ2Ix\n" +
                "l8JDNMXn8XsZb3+kyAJ29xfmoVlTeSVW4k51YBuJ6MJC0oUH/ipcfcveK7xQAvxU/v8eABC4tZsSEA\n" +
                "NuJRXE2N5OcG7FwsZDfjtAeEVl3Ed8H3GLmWbDIXH2fYSODWeyW5x61wjxd9l8WsEvromZej2tMKyw\n" +
                "6AyVtujLutYESGQmoLxYPzdspWE/N/Q9XvO5XjsWi+qylYgUkUlZnQ1M5uhjWgMIwuF5OdESaTpcw5\n" +
                "rP5WN36UUqttHPjhavS25SUceDPTSrtlAMHsX39DT0va976qSZDSH8gEq4N1 user@domain";
        public static final String SSH_PUBLIC_KEY_NOTE = "(the ssh public key to allow access to the compute)";
        public static final String USER_DATA = "[{ " +
                "\"extraUserDataFileContent\":\"#!/bin/bash my-script\", " +
                "\"extraUserDataFileType\":\"SHELL_SCRIPT\", " +
                "\"tag\": \"my-user-data-tag\" }]";
        public static final String USER_DATA_NOTE = "(a script that is executed by cloudinit when the compute is booted up";
        public static final String NETWORK_IDS = "[\"13310a2c-5df3-4a0e-a1ee-202125583ffa\", \"3c6931f1-71be-4cea-af8b-0566605cc869\"]";
        public static final String NETWORK_IDS_NOTE = "(the list of network IDs to which the compute will be attached)";
        public static final String COMPUTE_REQUIREMENTS = "{\"sgx\": \"true\", \"gpgpu\": \"true\"}";
        public static final String COMPUTE_REQUIREMENTS_NOTE = "(this can be used to select particular features in the compute; each provider might " +
                "use a different tagging standard)";
        public static final String NETWORK_NAME = "my network";
        public static final String VOLUME_NAME = "my volume";
        public static final String VOLUME_SIZE_NOTE = "(size in Giga bytes)";
        public static final String VOLUME_REQUIREMENTS = "{\"ssd\": \"true\"}";
        public static final String VOLUME_REQUIREMENTS_NOTE = "(this can be used to select particular features in the volume; each provider might " +
                "use a different tagging standard)";
        public static final String CLOUD_LIST = "[ \"cloudA\", \"cloudB\" ]";
        public static final String IMAGE_NAME = "ubuntu-16.04";
        public static final String INSTANCE_ID = "9632af26-72ee-461a-99a9-1e5d59076a98";
        public static final String INSTANCE_NAME = "instance name";
        public static final String INSTANCE_ID2 = "13310a2c-5df3-4a0e-a1ee-202125583ffa";
        public static final String IMAGE_LIST = "\"images\": [\n" +
                "    { \"id\": \"f1d97fdd-19af-45d1-bb91-f6a92183875f\", \"name\": \"fedora-27\"},\n" +
                "    { \"id\": \"670f78c3-5d50-4600-90bb-d01820fddb8d\", \"name\": \"ubuntu-16.04\"},\n" +
                "    { \"id\": \"2ee823a2-6a36-4aeb-9a74-715960b2fcea\", \"name\": \"centos6\"},\n" +
                "    { \"id\": \"b6de0caa-680b-4f5b-851b-11e53eed1733\", \"name\": \"opensuse-42.3\"},\n" +
                "    { \"id\": \"54b4f5dd-7a11-46bd-8d6f-1d702051c900\", \"name\": \"debian\"}\n" +
                "]";
        public static final String IMAGE_LIST_NOTE = "(the list of IDs and names of the images that are available to the user.)";
        public static final String NETWORKS = "";
        public static final String IP_ADDRESSES = "[\n" +
                "    \"10.11.4.94\",\n" +
                "    \"10.0.0.6\"\n" +
                "  ]";
        public static final String IMAGE_NAME_NOTE = "(the name of the image)";
        public static final String NETWORK_ID = "13310a2c-5df3-4a0e-a1ee-202125583ffa";
        public static final String NETWORK_ID_NOTE = "(the network ID)";
        public static final String NETWORK_NAME_NOTE = "(the network name)";
        public static final String COMPUTE_ID_NOTE = "(the ID of the compute to which the IP has been assigned)";
    }
}

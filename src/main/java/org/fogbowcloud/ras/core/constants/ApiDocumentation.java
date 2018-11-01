package org.fogbowcloud.ras.core.constants;

public class ApiDocumentation {
    public static class Attachment {
        public static final String API = "Manages attachments.";
        public static final String CREATE_OPERATION = "Creates an attachment.";
        public static final String GET_OPERATION = "Lists all attachments created by the user.";
        public static final String GET_BY_ID_OPERATION = "Lists a specific attachment.";
        public static final String DELETE_OPERATION = "Deletes a specific attachment.";
        public static final String ID = "The ID of the specific attachment.";

        public static final String CREATE_REQUEST_BODY =
                "The body of the request must specify the ID of the volume (volumeId) and the ID of the\n" +
                "compute instance (computeId) where the volume will be attached; Optionally, it can specify\n" +
                "the name of the device that is going to be created in the compute, and the provider where\n" +
                "the attachment will be created.";
    }

    public static class Compute {
        public static final String API = "Manages compute instances.";
        public static final String CREATE_OPERATION = "Creates a compute instance.";
        public static final String GET_OPERATION = "Lists all compute instances created by the user.";
        public static final String GET_BY_ID_OPERATION = "Lists a specific compute instance.";
        public static final String DELETE_OPERATION = "Deletes a specific compute instance.";
        public static final String GET_QUOTA = "Gets the compute quota for the user on a particular provider.";
        public static final String GET_ALLOCATION = "Gets the current compute allocation for the user on a particular provider.";
        public static final String ID = "The ID of the specific compute instance.";

        public static final String CREATE_REQUEST_BODY =
                "The body of the request must specify the amount of vCpus (vCPU), the amount of memory (in MB)\n" +
                "(memory), the disk size in GB (disk) that is required, the image id (imageId) that is going to\n" +
                "be used to create the compute, and optionally, the provider (provider) where the compute should\n" +
                "be created, a name (name) that can be assigned to the compute, an array of private network ids\n" +
                "(networkIds) that were previously created using RAS, the public key (publicKey) that is going to\n" +
                "be used to allow remote connection to the compute, and information (userData) for the customization\n" +
                "of the compute through the cloudinit mechanism.";
    }

    public static class Image {
        public static final String API = "Queries images.";
        public static final String GET_OPERATION = "Lists all images available to the user.";
        public static final String GET_BY_ID_OPERATION = "Lists a specific image available to the user.";
        public static final String ID = "The ID of the specific image.";
    }

    public static class Network {
        public static final String API = "Manages private networks.";
        public static final String CREATE_OPERATION = "Creates a private network.";
        public static final String GET_OPERATION = "Lists all private networks created by the user.";
        public static final String GET_BY_ID_OPERATION = "Lists a specific private network.";
        public static final String DELETE_OPERATION = "Deletes a specific private network.";
        public static final String ID = "The ID of the specific private network.";
        public static final String CREATE_REQUEST_BODY =
                "The body of the request must specify the CIDR of the network to be created (cidr),\n" +
                "the allocation mode of the network (dynamic or static), the gateway address of the\n" +
                "network (gateway) and optionally, the provider (provider) where the network should\n" +
                "be created, and a name (name) to be assigned to the network.";
    }

    public static class PublicIp {
        public static final String API = "Manages public IPs.";
        public static final String CREATE_OPERATION = "Creates a public IP and attaches it to a compute instance.";
        public static final String GET_OPERATION = "Lists all public IPs created by the user.";
        public static final String GET_BY_ID_OPERATION = "Lists a specific public IP.";
        public static final String DELETE_OPERATION = "Deletes a specific public IP.";
        public static final String ID = "The ID of the specific public IP.";
        public static final String CREATE_REQUEST_BODY =
                "The body of the request must specify the ID of the compute (computeId)\n" +
                "to which this IP will be assigned.";
    }

    public static class Token {
        public static final String API = "Creates tokens for the users of the federation.";
        public static final String CREATE_OPERATION = "Creates a token.";
        public static final String CREATE_REQUEST_BODY =
                "The body of the request must specify the values and keys for the type of IdentityPlugin\n" +
                "your RAS deploy is using. For instance, if you are using LDAP, you should provide a username\n" +
                "and a password. For Openstack Keystone, you should provide a username, a password, a domain\n" +
                "and a project name (projectName).";
    }

    public static class Version {
        public static final String API = "Queries the version of the service's API.";
        public static final String GET_OPERATION = "Returns the version of the API.";
    }

    public static class Volume {
        public static final String API = "Manages volumes.";
        public static final String CREATE_OPERATION = "Creates a volume.";
        public static final String GET_OPERATION = "Lists all volumes created by the user.";
        public static final String GET_BY_ID_OPERATION = "Lists a specific volume.";
        public static final String DELETE_OPERATION = "Deletes a specific volume.";
        public static final String ID = "The ID of the specific volume.";
        public static final String CREATE_REQUEST_BODY =
                "The body of the request must specify the size (volumeSize) of the volume to be\n" +
                "created and, optionally, the provider (provider) where the volume should be\n" +
                "created and a name (name) to be assigned to the volume.";
    }

    public static class CommonParameters {
        public static final String FEDERATION_TOKEN = "This is the token that identifies a federation user.\n" +
                                                      "It is typically created via a call to the /tokens endpoint.";
        public static final String MEMBER_ID = "The ID of the specific provider.";
    }

}

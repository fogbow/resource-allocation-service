package org.fogbowcloud.ras.core.constants;

public class ApiDocumentation {
    public static class Attachment {
        public static final String API = "Manages attachments.";
        public static final String CREATE_OPERATION = "Creates an attachment.";
        public static final String CREATE_REQUEST_BODY =
                "The body of the request must specify the ID of the volume (volumeId) and the ID of the\n" +
                "compute instance (computeId) where the volume will be attached; device specifies the\n" +
                "name of the device that will be created in the compute instance.";
        public static final String GET_OPERATION = "Lists all attachments created by the user.";
        public static final String GET_BY_ID_OPERATION = "Lists a specific attachment.";
        public static final String DELETE_OPERATION = "Deletes a specific attachment.";
        public static final String ID = "The ID of the specific attachment.";
    }

    public static class Compute {
        public static final String API = "Manages compute instances.";
        public static final String CREATE_OPERATION = "Creates a compute instance.";
        public static final String CREATE_REQUEST_BODY =
                        "The body of the request must specify the ID of the volume (volumeId) and the ID of the\n" +
                        "compute instance (computeId) where the volume will be attached; device specifies the\n" +
                        "name of the device that will be created in the compute instance.";
        public static final String GET_OPERATION = "Lists all compute instances created by the user.";
        public static final String GET_BY_ID_OPERATION = "Lists a specific compute instance.";
        public static final String DELETE_OPERATION = "Deletes a specific compute instance.";
        public static final String GET_QUOTA = "Gets the compute quota for the user on a particular provider.";
        public static final String GET_ALLOCATION = "Gets the current compute allocation for the user on a particular provider.";
        public static final String ID = "The ID of the specific compute instance.";
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
        public static final String CREATE_REQUEST_BODY =
                        "The body of the request must specify the ID of the volume (volumeId) and the ID of the\n" +
                        "compute instance (computeId) where the volume will be attached; device specifies the\n" +
                        "name of the device that will be created in the compute instance.";
        public static final String GET_OPERATION = "Lists all private networks created by the user.";
        public static final String GET_BY_ID_OPERATION = "Lists a specific private network.";
        public static final String DELETE_OPERATION = "Deletes a specific private network.";
        public static final String ID = "The ID of the specific private network.";
    }

    public static class PublicIp {
        public static final String API = "Manages public IPs.";
        public static final String CREATE_OPERATION = "Creates a public IP and attaches it to a compute instance.";
        public static final String CREATE_REQUEST_BODY =
                "The body of the request must specify the ID of the volume (volumeId) and the ID of the\n" +
                        "compute instance (computeId) where the volume will be attached; device specifies the\n" +
                        "name of the device that will be created in the compute instance.";
        public static final String GET_OPERATION = "Lists all public IPs created by the user.";
        public static final String GET_BY_ID_OPERATION = "Lists a specific public IP.";
        public static final String DELETE_OPERATION = "Deletes a specific public IP.";
        public static final String ID = "The ID of the specific public IP.";
    }

    public static class Token {
        public static final String API = "Creates tokens for the users of the federation.";
        public static final String CREATE_OPERATION = "Creates a token.";
        public static final String CREATE_REQUEST_BODY =
                "The body of the request must specify the ID of the volume (volumeId) and the ID of the\n" +
                        "compute instance (computeId) where the volume will be attached; device specifies the\n" +
                        "name of the device that will be created in the compute instance.";
    }

    public static class Version {
        public static final String API = "Queries the version of the service's API.";
        public static final String GET_OPERATION = "Returns the version of the API.";
    }

    public static class Volume {
        public static final String API = "Manages volumes.";
        public static final String CREATE_OPERATION = "Creates a volume.";
        public static final String CREATE_REQUEST_BODY =
                "The body of the request must specify the ID of the volume (volumeId) and the ID of the\n" +
                        "compute instance (computeId) where the volume will be attached; device specifies the\n" +
                        "name of the device that will be created in the compute instance.";
        public static final String GET_OPERATION = "Lists all volumes created by the user.";
        public static final String GET_BY_ID_OPERATION = "Lists a specific volume.";
        public static final String DELETE_OPERATION = "Deletes a specific volume.";
        public static final String ID = "The ID of the specific volume.";
    }

    public static class CommonParameters {
        public static final String FEDERATION_TOKEN = "This is the token that identifies a federation user.\n" +
                                                      "It is typically created via a call to the /tokens endpoint.";
        public static final String MEMBER_ID = "The ID of the specific provider";
    }
}

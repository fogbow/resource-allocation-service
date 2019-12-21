package cloud.fogbow.ras.constants;

public class Messages {

    public static class Exception {
        public static final String CLOUD_NAMES_DONT_MATCH = "The embedded resource has not been instantiated in the same cloud.";
        public static final String CORRUPTED_INSTANCE = "Corrupted instance.";
        public static final String DEPENDENCY_DETECTED = "Cannot delete order '%s'. There are other orders associated with it: ids '%s'. You should remove those dependencies first.";
        public static final String FAILED_TO_GENERATE_METHOD_S = "Failed to generate method: %s.";
        public static final String FAILED_TO_GET_QUOTA = "Failer to get quota.";
        public static final String FAILED_TO_INVOKE_METHOD_S = "Failed to invoke method: %s.";
        public static final String GENERIC_EXCEPTION = "Operation returned error: %s";
        public static final String IMAGE_NOT_FOUND = "Image not found";
        public static final String INCORRECT_PROVIDER = "Incorrect provider.";
        public static final String INCORRECT_REQUESTING_PROVIDER = "Mismatch on requesting provider information.";
        public static final String INSTANCE_NOT_FOUND = "Instance not found.";
        public static final String INSTANCE_NULL_S = "There is no active instance with id: <%s>.";
        public static final String INVALID_CIDR = "CIDR %s is not valid.";
        public static final String INVALID_CLOUDSTACK_PROTOCOL = "Protocol <%s> couldn't be mapped to a valid protocol";
        public static final String INVALID_ONE_RESOURCE_S = "Invalid oneResource: %s.";
        public static final String INVALID_ONE_METHOD_S = "Invalid oneMethod: %s.";
        public static final String INVALID_PARAMETER = "Invalid parameter.";
        public static final String INVALID_PARAMETER_S = "Invalid parameter: %s.";
        public static final String INVALID_PROTOCOL = "Protocol <%s> is not one of %s.";
        public static final String INVALID_RESOURCE = "Invalid resource type.";
        public static final String INVALID_RESOURCE_ID_S = "Invalid resourceId: %s.";
        public static final String INVALID_URL_S = "Invalid url: %s.";
        public static final String INVALID_CIDR_FORMAT = "The cidr %s does not follow the expected format";
        public static final String JOB_HAS_FAILED = "Instance associated to job %s has failed.";
        public static final String JOB_TIMEOUT = "Instance associated to job %s has failed, because it took too long to process.";
        public static final String MALFORMED_GENERIC_REQUEST_URL = "Malformed generic request URL <%s>";
        public static final String MISMATCHING_RESOURCE_TYPE = "Mismatching resource type.";
        public static final String MULTIPLE_SECURITY_GROUPS_EQUALLY_NAMED = "There should be exactly one security group with name <%s>";
        public static final String NO_MATCHING_FLAVOR = "No matching flavor.";
        public static final String NO_PROJECT_ID = "No projectId in local token.";
        public static final String NO_SECURITY_GROUP_FOUND = "There is no security group with the id %s";
        public static final String NON_EXISTENT_REQUEST = "Request does not exist.";
        public static final String NOT_FOUND_ORDER_ID_S = "Order ID %s not found.";
        public static final String NULL_VALUE_RETURNED = "Plugin returned a null value for the instanceId.";
        public static final String PORT_NOT_FOUND = "No port found connecting virtual machine %s to default network %s.";
        public static final String PROVIDERS_DONT_MATCH = "The attachment provider does not match with the compute and/or volume providers.";
        public static final String QUOTA_ENDPOINT_NOT_IMPLEMENTED = "Quota endpoint for %s not yet implemented.";
        public static final String REQUEST_ALREADY_EXIST = "Request already exists.";
        public static final String REQUEST_ID_ALREADY_ACTIVATED = "Request %s has already been activated.";
        public static final String REQUEST_INSTANCE_NULL = "Request instance id for request %s is null.";
        public static final String REQUESTER_DOES_NOT_OWN_REQUEST = "Requester does not own request.";
        public static final String RESOURCE_TYPE_NOT_COMPATIBLE_S = "Resource type not compatible with %s request.";
        public static final String RESOURCE_TYPE_NOT_IMPLEMENTED = "Resouce type not yet implemented.";
        public static final String RULE_NOT_AVAILABLE = "Rule not available for deletion.";
        public static final String SIGNALING_PROVIDER_DIFFERENT_OF_PROVIDER = "Signalling provider %s is not the provider %s.";
        public static final String TOKEN_ALREADY_SPECIFIED = "There should be no OpenStack token specified on the request";
        public static final String TOO_BIG_USER_DATA_FILE_CONTENT = "Too big user data file.";
        public static final String TRYING_TO_USE_RESOURCES_FROM_ANOTHER_USER = "Trying to use resources from another user.";
        public static final String UNABLE_TO_DESERIALIZE_SYSTEM_USER = "Unable to deserialize system user.";
        public static final String UNABLE_TO_FIND_LIST_FOR_REQUESTS = "Unable to find list for requests in state %s.";
        public static final String UNABLE_TO_MATCH_REQUIREMENTS = "Unable to match requirements.";
        public static final String UNABLE_TO_PROCESS_EMPTY_REQUEST = "Unable to process request with null reference.";
        public static final String UNABLE_TO_REMOVE_INACTIVE_REQUEST = "Unable to remove inactive request %s.";
        public static final String UNABLE_TO_RETRIEVE_RESPONSE_FROM_PROVIDER = "Unable to retrieve response from provider: %s.";
        public static final String UNEXPECTED_ERROR = "Unexpected error.";
        public static final String UNEXPECTED_OPERATION_S = "Unexpected operation: %s.";
        public static final String UNSUPPORTED_REQUEST_TYPE = "Request type %s not supported.";
        public static final String WRONG_URI_SYNTAX = "Wrong syntax for endpoint %s.";
        public static final String UNABLE_TO_CREATE_NETWORK_RESERVE = "Unable to create network reserve with CIDR: <%s>." +
                " This address range is likely in use.";
    }

    public static class Fatal {
        public static final String DEFAULT_CREDENTIALS_NOT_FOUND = "Default credentials not found.";
        public static final String DEFAULT_NETWORK_NOT_FOUND = "Default network not found.";
        public static final String EMPTY_PROPERTY_MAP = "Empty property getCloudUser.";
        public static final String EXTERNAL_NETWORK_NOT_FOUND = "External network not found.";
        public static final String NEUTRON_ENDPOINT_NOT_FOUND = "Neutron endpoint not found.";
        public static final String NO_CLOUD_SPECIFIED = "No cloud names specified in ras.conf file";
    }

    public static class Warn {
        public static final String INCONSISTENT_DIRECTION = "The direction(%s) is inconsistent";
        public static final String INCONSISTENT_PROTOCOL_S = "The protocol(%s) is inconsistent";
        public static final String INCONSISTENT_RANGE_S = "The range(%s) is inconsistent";
        public static final String INSTANCE_S_ALREADY_DELETED = "Instance <%s> has already been deleted.";
        public static final String NETWORK_NOT_FOUND = "Network id %s was not found when trying to delete it.";
        public static final String UNABLE_TO_ADD_EXTRA_USER_DATA_FILE_CONTENT_NULL = "Unable to add the extra user data file; content is null.";
        public static final String UNABLE_TO_ADD_EXTRA_USER_DATA_FILE_TYPE_NULL = "Unable to add the extra user data file; file type is null.";
        public static final String UNABLE_TO_DECODE_URL = "Unable to decode url %s.";
        public static final String UNABLE_TO_LOCATE_ORDER_S_S = "Unable to locate order <%s> notified by <%s>.";
        public static final String UNABLE_TO_NOTIFY_REQUESTING_PROVIDER = "Unable to notify requesting provider %s for request %s.";
        public static final String UNABLE_TO_RETRIEVE_ROOT_VOLUME = "Unable to retrieve root volume for virtual machine %s; assigning -1 to disk size.";
    }

    public static class Info {
        public static final String ACTIVATING_NEW_REQUEST = "Activating new request.";
        public static final String CONNECTING_UP_PACKET_SENDER = "Connecting XMPP packet sender.";
        public static final String DELETING_INSTANCE = "Deleting instance %s with token %s.";
        public static final String DELETING_INSTANCE_S = "Deleting instance %s.";
        public static final String GET_PUBLIC_KEY = "Get public key received.";
        public static final String GETTING_INSTANCE_S = "Getting instance %s.";
        public static final String GETTING_QUOTA = "Getting quota.";
        public static final String INSTANCE_HAS_FAILED = "Instance associated to request %s has failed.";
        public static final String INSTANCE_NOT_FOUND_S = "Instance not found: <%s>.";
        public static final String MAPPED_USER = "User mapped to: %s.";
        public static final String MAPPING_USER_OP = "Mapping user for operation %s on order/systemUser %s.";
        public static final String NO_REMOTE_COMMUNICATION_CONFIGURED = "No remote communication configured.";
        public static final String PACKET_SENDER_INITIALIZED = "XMPP packet sender initialized.";
        public static final String RECEIVING_COMPUTE_QUOTA_REQUEST = "Get compute %s request for provider %s received.";
        public static final String RECEIVING_CREATE_REQUEST = "Create request for %s received.";
        public static final String RECEIVING_DELETE_REQUEST = "Delete request for %s %s received.";
        public static final String RECEIVING_GET_ALL_IMAGES_REQUEST = "Get all images request received.";
        public static final String RECEIVING_GET_ALL_REQUEST = "Get status request for all %s received.";
        public static final String RECEIVING_GET_CLOUDS_REQUEST = "Get request for cloud names received.";
        public static final String RECEIVING_GET_IMAGE_REQUEST = "Get request for image %s received.";
        public static final String RECEIVING_GET_REQUEST = "Get request for %s %s received.";
        public static final String RECEIVING_REMOTE_REQUEST = "Received remote request for request: %s.";
        public static final String RECOVERING_LIST_OF_ORDERS = "Recovering requests in %s state: %d requests recovered so far.";
        public static final String REQUESTING_INSTANCE_FROM_PROVIDER = "Requesting instance from provider.";
        public static final String REQUESTING_GET_ALL_FROM_PROVIDER = "Requesting all images from provider.";
        public static final String REQUESTING_TO_CLOUD = "Requesting to the cloud by the user %s. URL: %s";
        public static final String RESPONSE_RECEIVED = "Received response: %s.";
        public static final String SENDING_MSG = "Sending remote request for request: %s.";
        public static final String SETTING_UP_PACKET_SENDER = "Setting up XMPP packet sender.";
        public static final String STARTING_THREADS = "Starting processor threads.";
        public static final String SUCCESS = "Successfully executed operation.";
        public static final String XMPP_HANDLERS_SET = "XMPP handlers set.";
        public static final String ASYNCHRONOUS_PUBLIC_IP_STATE =
                "The asynchronous public ip request %s is in the state %s.";
    }

    public static class Error {
    	public static final String CONTENT_SECURITY_GROUP_NOT_DEFINED = "The content of SecuriryGroups in the VirtualNetwork template is not defined.";
        public static final String COULD_NOT_FIND_DEPENDENCY_S_S = "Could not find dependency %s for order %s.";
        public static final String ERROR_MESSAGE = "Error message is: %s.";
        public static final String ERROR_WHILE_ATTACHING_VOLUME = "Error while attaching volume image disk: %s, with response: %s.";
        public static final String ERROR_WHILE_ATTACHING_VOLUME_GENERAL = "Error while attaching volume with response: %s.";
        public static final String ERROR_WHILE_CONVERTING_INSTANCE_ID = "Error while converting instanceid %s to integer.";
        public static final String ERROR_WHILE_CONVERTING_TO_INTEGER = "Error while converting the input string to an integer.";
        public static final String ERROR_WHILE_CREATING_CLIENT = "Error while creating client.";
        public static final String ERROR_WHILE_CREATING_IMAGE = "Error while creating a image from template: %s.";
        public static final String ERROR_WHILE_CREATING_NETWORK = "Error while creating a network from template: %s.";
        public static final String ERROR_WHILE_CREATING_NIC = "Error while creating a network interface connected from template: %s.";
        public static final String ERROR_WHILE_CREATING_RESOURCE_S = "Error while creating %s.";
        public static final String ERROR_WHILE_CREATING_RESPONSE_BODY = "Error while creating response body.";
        public static final String ERROR_WHILE_CREATING_SECURITY_GROUPS = "Error while creating a security groups from template: %s.";
        public static final String ERROR_WHILE_DETACHING_VOLUME = "Error while detaching volume image disk: %s, with response: %s.";
        public static final String ERROR_WHILE_GETTING_DISK_SIZE = "Error while getting disk size.";
        public static final String ERROR_WHILE_GETTING_GROUP = "Error while getting info about group %s: %s.";
        public static final String ERROR_WHILE_GETTING_INSTANCE_FROM_CLOUD = "Error while getting instance from the cloud.";
        public static final String ERROR_WHILE_GETTING_RESOURCE_S_FROM_CLOUD = "Error while getting %s from the cloud.";
        public static final String ERROR_WHILE_GETTING_TEMPLATES = "Error while getting info about templates: %s.";
        public static final String ERROR_WHILE_GETTING_USER = "Error while getting info about user %s: %s.";
        public static final String ERROR_WHILE_GETTING_USERS = "Error while getting info about users: %s.";
        public static final String ERROR_WHILE_GETTING_VOLUME_INSTANCE = "Error while getting volume instance.";
        public static final String ERROR_WHILE_INSTANTIATING_FROM_TEMPLATE = "Error while instatiating an instance from template: %s.";
        public static final String ERROR_WHILE_PROCESSING_VOLUME_REQUIREMENTS = "Error while processing volume requirements";
        public static final String ERROR_WHILE_REMOVING_RESOURCE = "An error occurred while removing %s: %s.";
        public static final String ERROR_WHILE_PROCESSING_ASYNCHRONOUS_REQUEST_INSTANCE_STEP =
                "Error while It was trying to pass to the next step in the asynchronous request instance.";
        public static final String ERROR_WHILE_REMOVING_VOLUME_IMAGE = "Error while removing volume image: %s, with response: %s.";
        public static final String ERROR_WHILE_UPDATING_NETWORK = "Error while updating a network from template: %s.";
        public static final String ERROR_WHILE_UPDATING_SECURITY_GROUPS = "Error while updating a security groups from template: %s.";
        public static final String ERROR_WHILE_REMOVING_VM = "Error while removing virtual machine: %s, with response: %s.";
        public static final String INSTANCE_TYPE_NOT_DEFINED = "Instance type not defined.";
        public static final String INVALID_LIST_SECURITY_RULE_TYPE = "Invalid list security rule type. Order irregular: %s.";
        public static final String NO_PACKET_SENDER = "PacketSender was not initialized. Trying again.";
        public static final String REQUEST_ALREADY_CLOSED = "Request %s is already in the closed state.";
        public static final String THREAD_HAS_BEEN_INTERRUPTED = "Thread has been interrupted.";
        public static final String UNABLE_TO_COMPLETE_REQUEST_CLOUDSTACK = "Unable to complete request; template ID is required parameter.";
        public static final String UNABLE_TO_COMPLETE_REQUEST_SERVICE_OFFERING_CLOUDSTACK = "Service Offering no available.";
        public static final String UNABLE_TO_COMPLETE_REQUEST_DISK_OFFERING_CLOUDSTACK = "Disk Offering no available.";
        public static final String UNABLE_TO_CREATE_ATTACHMENT = "Unable to create an attachment from json.";
        public static final String UNABLE_TO_DELETE_INSTANCE = "Unable to delete instance %s.";
        public static final String UNABLE_TO_DELETE_NETWORK = "Unable to delete network with id %s.";
        public static final String UNABLE_TO_DELETE_SECURITY_GROUP = "Unable to delete security group with id %s.";
        public static final String UNABLE_TO_GENERATE_JSON = "Unable to generate json.";
        public static final String UNABLE_TO_GET_ATTACHMENT_INSTANCE = "Unable to get attachment instance from json.";
        public static final String UNABLE_TO_GET_NETWORK = "Unable to get network information from json %s.";
        public static final String UNABLE_TO_GET_SECURITY_GROUP = "Unable to get security group information from json %s.";
        public static final String UNABLE_TO_MARSHALL_IN_XML = "Unable to marshall in xml.";
        public static final String UNABLE_TO_RETRIEVE_NETWORK_ID = "Unable to retrieve network id from json %s.";
        public static final String UNABLE_TO_UNMARSHALL_XML_S = "Unable to unmarshall xml: %s.";
        public static final String UNDEFINED_INSTANCE_STATE_MAPPING = "State %s was not mapped to a Fogbow state by %s.";
        public static final String UNEXPECTED_ERROR = "Unexpected error.";
        public static final String UNEXPECTED_ERROR_WITH_MESSAGE = "Unexpected exception error: %s.";
        public static final String UNEXPECTED_JOB_STATUS = "Job status must be one of {0, 1, 2}.";
        public static final String UNSPECIFIED_PROJECT_ID = "Unspecified projectId.";
        public static final String INSTANCE_OPERATIONAL_LOST_MEMORY_FAILURE =
                "The instanceid %s had an operational failure due to the memory lost. It might left trash in the cloud.";
    }
}

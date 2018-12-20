package org.fogbowcloud.ras.core.constants;

public class Messages {

    public static class Exception {
        public static final String ATTEMPTING_TO_ADD_A_NULL_REQUEST = "Attempting to add a null request.";
        public static final String ATTEMPTING_TO_REMOVE_A_NULL_REQUEST = "Attempting to remove a null request.";
        public static final String AUTHENTICATION_ERROR = "Authentication error.";
        public static final String AUTHORIZATION_ERROR = "Authorization error.";
        public static final String FATAL_ERROR = "Fatal error.";
        public static final String FOGBOW_RAS = "Fogbow RAS exception.";
        public static final String GENERIC_EXCEPTION = "Operation returned error: %s";
        public static final String INCORRECT_PROVIDING_MEMBER = "Incorrect providing member.";
        public static final String INEXISTENT_REQUEST = "Request does not exist.";
        public static final String INSTANCE_ID_NOT_INFORMED = "No instance identification informed.";
        public static final String INSTANCE_NOT_FOUND = "Instance not found.";
        public static final String INVALID_CIDR = "CIDR %s is not valid.";
        public static final String INVALID_CREDENTIALS = "Invalid credentials.";
        public static final String INVALID_PARAMETER = "Invalid parameter.";
        public static final String INVALID_PORT_SIZE = "Invalid port size %s for virtual machine %s and default network %s.";
        public static final String INVALID_PROTOCOL = "Protocol <%s> is not one of %s.";
        public static final String INVALID_RESOURCE = "Invalid resource type.";
        public static final String INVALID_TOKEN_SIGNATURE = "Invalid token signature.";
        public static final String JOB_HAS_FAILED = "Instance associated to job %s has failed.";
        public static final String JOB_TIMEOUT = "Instance associated to job %s has failed, because it took too long to process.";
        public static final String LDAP_URL_MISSING = "No LDAP url in configuration file.";
        public static final String MISMATCHING_RESOURCE_TYPE = "Mismatching resource type.";
        public static final String NO_AVAILABLE_RESOURCES = "No available resources.";
        public static final String NO_MATCHING_FLAVOR = "No matching flavor.";
        public static final String NO_PACKET_SENDER = "PacketSender was not initialized";
        public static final String NO_PROJECT_ID = "No projectId in local token.";
        public static final String NO_USER_CREDENTIALS = "No user credentials given.";
        public static final String NULL_VALUE_RETURNED = "Plugin returned a null value for the instanceId.";
        public static final String REQUEST_ALREADY_EXIST = "Request already exists.";
        public static final String REQUEST_ID_ALREADY_ACTIVATED = "Request %s has already been activated.";
        public static final String REQUEST_INSTANCE_NULL = "Request instance id for request %s is null.";
        public static final String PLUGIN_FOR_REQUEST_INSTANCE_NOT_IMPLEMENTED = "No requestInstance method implemented for request %s.";
        public static final String PORT_NOT_FOUND = "No port found connecting virtual machine %s to default network %s.";
        public static final String PROVIDERS_DONT_MATCH = "The attachment provider does not match with the compute and/or volume providers.";
        public static final String QUOTA_ENDPOINT_NOT_IMPLEMENTED = "Quota endpoint for %s not yet implemented.";
        public static final String QUOTA_EXCEEDED = "Quota exceeded.";
        public static final String REQUESTER_DOES_NOT_OWN_REQUEST = "Requester does not own request.";
        public static final String RESOURCE_TYPE_NOT_IMPLEMENTED = "Resouce type not yet implemented.";
        public static final String SIGNALING_MEMBER_DIFFERENT_OF_PROVIDER = "Signalling member %s is not the provider %s.";
        public static final String TOO_BIG_PUBLIC_KEY = "Too big public key.";
        public static final String TOO_BIG_USER_DATA_FILE_CONTENT = "Too big user data file.";
        public static final String TRYING_TO_USE_RESOURCES_FROM_ANOTHER_USER = "Trying to use resources from another user.";
        public static final String UNABLE_TO_FIND_LIST_FOR_REQUESTS = "Unable to find list for requests in state %s.";
        public static final String UNABLE_TO_LOAD_LDAP_ACCOUNT = "Unable to load account summary from LDAP Network.";
        public static final String UNABLE_TO_PROCESS_EMPTY_REQUEST = "Unable to process request with null reference.";
        public static final String UNABLE_TO_REMOVE_INACTIVE_REQUEST = "Unable to remove inactive request %s.";
        public static final String UNABLE_TO_RETRIEVE_RESPONSE_FROM_PROVIDING_MEMBER = "Unable to retrieve response from providing member: %s.";
        public static final String UNABLE_TO_SIGN_LDAP_TOKEN = "Unable to sign LDAP token.";
        public static final String UNAVAILABLE_PROVIDER = "Provider is not available.";
        public static final String UNEXPECTED_ERROR = "Unexpected error.";
        public static final String UNSUPPORTED_REQUEST_TYPE = "Request type %s not supported.";
        public static final String WRONG_URI_SYNTAX = "Wrong syntax for endpoint %s.";
        public static final String INVALID_SHIBBOLETH_TOKEN = "Is not possible create the Shibboleth token: %s";
        public static final String MULTIPLE_SECURITY_GROUPS_EQUALLY_NAMED = "There should be exactly one security group with name <%s>";
        public static final String INVALID_CLOUDSTACK_PROTOCOL = "Protocol <%s> couldn't be mapped to a valid protocol";
        public static final String MALFORMED_GENERIC_REQUEST_URL = "Malformed generic request URL <%s>";
        public static final String TOKEN_ALREADY_SPECIFIED = "There should be no OpenStack token specified on the request";
    }

    public static class Fatal {
        public static final String DEFAULT_CREDENTIALS_NOT_FOUND = "Default credentials not found.";
        public static final String DEFAULT_NETWORK_NOT_FOUND = "Default network not found.";
        public static final String EMPTY_PROPERTY_MAP = "Empty property map.";
        public static final String ERROR_INSTANTIATING_DATABASE_MANAGER = "Error instantiating database manager.";
        public static final String ERROR_READING_PRIVATE_KEY_FILE = "Error reading private key file %s.";
        public static final String ERROR_READING_PUBLIC_KEY_FILE = "Error reading public key file %s.";
        public static final String EXTERNAL_NETWORK_NOT_FOUND = "External network not found.";
        public static final String INVALID_SERVICE_URL = "Invalid service URL: %s.";
        public static final String NEUTRON_ENDPOINT_NOT_FOUND = "Neutron endpoint not found.";
        public static final String NO_CLOUD_SPECIFIED = "No cloud names specified in clouds.conf file";
        public static final String PROPERTY_FILE_NOT_FOUND = "Property file %s not found.";
        public static final String UNABLE_TO_CONNECT_TO_XMPP_SERVER = "Unable to connect to XMPP server.";
        public static final String UNABLE_TO_FIND_CLASS = "Unable to find class %s.";
        public static final String UNABLE_TO_INITIALIZE_HTTP_REQUEST_UTIL = "Unable to initialize HttpRequestUtil.";
        public static final String UNABLE_TO_READ_COMPOSED_AUTHORIZATION_PLUGIN_CONF_FILE = "Unable to read ComposedAuthorizationPlugin configuration file %s.";
    }

    public static class Warn {
        public static final String INSTANCE_ALREADY_DELETED = "Instance has already been deleted.";
        public static final String NETWORK_NOT_FOUND = "Network id %s was not found when trying to delete it.";
        public static final String UNABLE_TO_ADD_EXTRA_USER_DATA_FILE_CONTENT_NULL = "Unable to add the extra user data file; content is null.";
        public static final String UNABLE_TO_ADD_EXTRA_USER_DATA_FILE_TYPE_NULL = "Unable to add the extra user data file; file type is null.";
        public static final String UNABLE_TO_DECODE_URL = "Unable to decode url %s.";
        public static final String UNABLE_TO_GENERATE_SIGNATURE = "Unable to generate signature.";
        public static final String UNABLE_TO_NOTIFY_REQUESTING_MEMBER = "Unable to notify requesting member %s for request %s.";
        public static final String UNABLE_TO_RETRIEVE_ROOT_VOLUME = "Unable to retrieve root volume for virtual machine %s; assigning -1 to disk size.";
        public static final String INCONSISTENT_DIRECTION = "The direction(%s) is inconsistent";
    }

    public static class Info {
        public static final String ACTIVATING_NEW_REQUEST = "Activating new request.";
        public static final String DATABASE_URL = "Database URL: %s.";
        public static final String DELETING_INSTANCE = "Deleting instance %s with token %s.";
        public static final String DELETING_ORDER_INSTANCE_NOT_FOUND = "Deleting order %s associated with nonexistent instance.";
        public static final String GETTING_INSTANCE = "Getting instance %s with token %s.";
        public static final String INSTANCE_HAS_FAILED = "Instance associated to request %s has failed.";
        public static final String MOUNTING_INSTANCE = "Mounting instance structure of id: %s.";
        public static final String RECEIVING_COMPUTE_QUOTA_REQUEST = "Get compute %s request for member %s received.";
        public static final String RECEIVING_CREATE_REQUEST = "Create request for %s received.";
        public static final String RECEIVING_CREATE_TOKEN_REQUEST = "Create token request received with a credentials map of size %s.";
        public static final String RECEIVING_DELETE_REQUEST = "Delete request for %s %s received.";
        public static final String RECEIVING_GET_ALL_IMAGES_REQUEST = "Get all images request received.";
        public static final String RECEIVING_GET_ALL_REQUEST = "Get status request for all %s received.";
        public static final String RECEIVING_GET_CLOUDS_REQUEST = "Get request for cloud names received.";
        public static final String RECEIVING_GET_DEFAULT_CLOUD_REQUEST = "Get request for default cloud name received.";
        public static final String RECEIVING_GET_IMAGE_REQUEST = "Get request for image %s received.";
        public static final String RECEIVING_GET_REQUEST = "Get request for %s %s received.";
        public static final String RECEIVING_GET_VERSION_REQUEST = "Get request for version received.";
        public static final String RECEIVING_REMOTE_REQUEST = "Received remote request for request %s.";
        public static final String RECOVERING_LIST_OF_ORDERS = "Recovering requests in %s state: %d requests recovered so far.";
        public static final String REQUESTING_INSTANCE = "Requesting instance with token %s.";
        public static final String SKIPPING_BECAUSE_PROVIDER_IS_REMOTE = "Member %s skipping monitoring of request %s provided by %s.";
        public static final String STARTING_THREADS = "Starting processor threads.";
        public static final String TEMPLATE_POOL_LENGTH = "Template pool length: %s.";
        public static final String INSTANCE_WAS_NOT_FOUND = "Instance associated with request %s was not found.";
		public static final String USER_POOL_LENGTH = "User pool length: %s.";
    }

    public static class Error {
        public static final String DELETE_INSTANCE_PLUGIN_NOT_IMPLEMENTED = "No deleteInstance plugin implemented for resource type %s.";
        public static final String ERROR_MESSAGE = "Error message is: %s.";
        public static final String ERROR_WHILE_CREATING_CLIENT = "Error while creating client.";
        public static final String ERROR_WHILE_CREATING_NETWORK = "Error while creating a network from template: %s.";
        public static final String ERROR_WHILE_CREATING_IMAGE = "Error while creating a image from template: %s.";
        public static final String ERROR_WHILE_CREATING_ROUTER = "Error while creating a router from template: %s.";
        public static final String INVALID_LIST_SECURITY_RULE_TYPE = "Invalid list security rule type. Order irregular: %s.";
        public static final String ERROR_WHILE_CONSUMING_RESPONSE = "Error while consuming response %s.";
        public static final String ERROR_WHILE_CONVERTING_INSTANCE_ID = "Error while converting instanceid %s to integer.";
        public static final String ERROR_WHILE_INSTANTIATING_FROM_TEMPLATE = "Error while instatiating an instance from template: %s.";
        public static final String ERROR_WHILE_GETTING_INSTANCE_FROM_REQUEST = "Error while trying to get an instance for request %s.";
        public static final String ERROR_WHILE_GETTING_INSTANCE_FROM_CLOUD = "Error while getting instance from the cloud.";
        public static final String ERROR_WHILE_GETTING_TEMPLATES = "Error while getting info about templates: %s.";
        public static final String ERROR_WHILE_GETTING_NEW_CONNECTION = "Error while getting a new connection from the connection pool.";
        public static final String ERROR_WHILE_GETTING_VOLUME_INSTANCE = "Error while getting volume instance.";
        public static final String ERROR_WHILE_GETTING_SECURITY_RULES_INSTANCE = "Error while getting security rules instance.";
        public static final String ERROR_WHILE_REMOVING_VM = "Error while removing virtual machine: %s, with response: %s.";
        public static final String ERROR_WHILE_REMOVING_VN = "Error while removing virtual network: %s, with response: %s.";
        public static final String ERROR_WHILE_REMOVING_SECURITY_RULE = "Error while removing security group: %s, with response: %s.";
        public static final String ERROR_WHILE_REMOVING_VR = "Error while removing virtual router: %s, with response: %s.";
        public static final String INSTANCE_TYPE_NOT_DEFINED = "Instance type not defined.";
        public static final String INVALID_DATASTORE_DRIVER = "Invalid datastore driver.";
        public static final String INVALID_TOKEN_VALUE = "Invalid token value: %s.";
        public static final String REQUEST_ALREADY_CLOSED = "Request %s is already in the closed state.";
        public static final String THREAD_HAS_BEEN_INTERRUPTED = "Thread has been interrupted.";
        public static final String UNABLE_TO_ADD_TIMESTAMP = "Unable to add timestamp.";
        public static final String UNABLE_TO_CLOSE_CONNECTION = "Unable to close connection.";
        public static final String UNABLE_TO_CLOSE_FILE = "Unable to close file %s.";
        public static final String UNABLE_TO_CLOSE_STATEMENT = "Unable to close statement.";
        public static final String UNABLE_TO_COMPLETE_REQUEST = "Unable to complete request; template, zone and default network IDs are required parameters.";
        public static final String UNABLE_TO_CREATE_TIMESTAMP_TABLE = "Unable to create timestamp table.";
        public static final String UNABLE_TO_DELETE_INSTANCE = "Unable to delete instance %s.";
        public static final String UNABLE_TO_DELETE_NETWORK = "Unable to delete network with id %s.";
        public static final String UNABLE_TO_DELETE_SECURITY_GROUP = "Unable to delete security group with id %s.";
        public static final String UNABLE_TO_GENERATE_JSON = "Unable to generate json.";
        public static final String UNABLE_TO_GET_ATTACHMENT_INSTANCE = "Unable to get attachment instance from json.";
        public static final String UNABLE_TO_GET_NETWORK = "Unable to get network information from json %s.";
        public static final String UNABLE_TO_GET_SECURITY_GROUP = "Unable to get security group information from json %s.";
        public static final String UNABLE_TO_GET_TOKEN_FROM_JSON = "Unable to get token from json.";
        public static final String UNABLE_TO_RETRIEVE_NETWORK_ID = "Unable to retrieve network id from json %s.";
        public static final String UNABLE_TO_ROLLBACK_TRANSACTION = "Unable to rollback transaction.";
        public static final String UNDEFINED_INSTANCE_STATE_MAPPING = "State %s was not mapped to a Fogbow state by %s.";
        public static final String UNEXPECTED_ERROR = "Unexpected error.";
        public static final String UNEXPECTED_ERROR_WITH_MESSAGE = "Unexpected exception error: %s.";
        public static final String UNEXPECTED_JOB_STATUS = "Job status must be one of {0, 1, 2}.";
        public static final String UNSPECIFIED_PROJECT_ID = "Unspecified projectId.";
        public static final String EMPTY_OR_NULL_TOKEN = "A null or empty token was received";
        public static final String INVALID_FORMAT_TOKEN = "Invalid format token was received";
		public static final String ERROR_WHILE_CREATING_SECURITY_GROUPS = "Error while creating a security groups from template: %s.";
		public static final String ERROR_WHILE_CREATING_NIC = "Error while creating a network interface connected from template: %s.";
		public static final String ERROR_WHILE_CREATING_AR = "Error while creating a address range from template: %s.";
		public static final String ERROR_WHILE_REMOVING_VI = "Error while removing volume image: %s, with response: %s.";
		public static final String ERROR_WHILE_DETACH_VOLUME = "Error while detaching volume image disk: %s, with response: %s.";
		public static final String ERROR_WHILE_ATTACH_VOLUME = "Error while attaching volume image disk: %s, with response: %s.";
		public static final String ERROR_WHILE_GETTING_USERS = "Error while getting info about users: %s.";
		public static final String ERROR_WHILE_GETTING_USER = "Error while getting info about user %s: %s.";
		public static final String ERROR_WHILE_UPDATING_SECURITY_GROUPS = "Error while updating a security groups from template: %s.";
		public static final String ERROR_WHILE_REMOVING_RESOURCE = "An error occurred while removing %s: %s.";
		public static final String CONTENT_SECURITY_GROUP_NOT_DEFINED = "The content of SecuriryGroups in the VirtualNetwork template is not defined.";
		public static final String CONTENT_SECURITY_GROUP_WRONG_FORMAT = "The contents of the security groups in the Virtual Network template may be in the wrong format.";
        public static final String ERROR_WHILE_GETTING_GROUP = "Error while getting info about group %s: %s.";
    }
}

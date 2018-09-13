package org.fogbowcloud.ras.core.constants;

public class Messages {

	public static class Exception {
		public static final String EXPIRED_TOKEN = "Token exprired.";
		public static final String PLUGIN_FOR_REQUEST_INSTANCE_NOT_IMPLEMENTED = "No requestInstance method implemented for order %s.";
		public static final String RETURNED_NULL_VALUE = "Plugin returned a null value for the instanceId.";
		public static final String ORDER_TYPE_NOT_SUPPORTED = "Order type %s not supported.";
		public static final String QUOTA_ENDPOINT_NOT_IMPLEMENTED = "Quota endpoint for %s not yet implemented.";
		public static final String ORDER_ALREADY_EXIST = "Order already exists.";
		public static final String ORDER_NOT_EXIST = "Order does not exist.";
		public static final String FATAL_ERROR = "Fatal error exception.";
		public static final String FOGBOW_RAS = "Fogbow RAS exception.";
		public static final String INSTANCE_NOT_FOUND = "Instance not found.";
		public static final String INVALID_PARAMETER = "Invalid parameter.";
		public static final String INVALID_CREDENTIALS = "Invalid credentials.";
		public static final String RESOURCES_NOT_AVAILABLE = "No available resources.";
		public static final String QUOTA_EXCEEDED = "Quota exceeded.";
		public static final String UNAUTHENTICATED_ERROR = "Unauthenticated error.";
		public static final String UNAUTHENTIC_TOKENS = "Unauthentic token.";
		public static final String UNAUTHORIZED_ERROR = "Unauthorized.";
		public static final String UNAVAILABLE_PROVIDER = "Unavailable provider.";
		public static final String UNEXPECTED = "Unexpected exception.";
		public static final String UNABLE_RETRIEVE_RESPONSE_FROM_PROVIDING_MEMBER = "Unable to retrieve response from providing member: %s.";
		public static final String ATTEMPTING_ADD_NULL_ORDER = "Attempting to add a null order.";
		public static final String ATTEMPTING_REMOVE_NULL_ORDER = "Attempting to remove a null order.";
		public static final String PORT_NOT_FOUND = "No port found connecting virtual machine %s to default network %s.";
		public static final String ILLEGAL_PORT_SIZE = "Illegal port size %s for virtual machine %s and default network %s.";
		public static final String MISMATCHING_RESOURCE_TYPE = "Mismatching resource type.";
		public static final String REQUESTER_NOT_OWN_ORDER = "Requester does not own order.";
		public static final String PROVIDING_MEMBER_NOT_CORRECT = "Incorrect providing member.";
		public static final String INSTANCE_ID_NOT_INFORMED = "No instance identification informed.";
		public static final String RESOURCE_TYPE_NOT_IMPLEMENTED = "Resouce type not yet implemented.";
		public static final String CANNOT_PROCESS_ORDER_REQUEST_NULL = "Cannot process new order request; order reference is null.";
		public static final String ORDER_ID_ALREADY_ACTIVATED = "Order %s is already in active orders map.";
		public static final String REMOVE_ORDER_NOT_ACTIVE = "Order %s not active.";
		public static final String COULD_NOT_FIND_LIST_FOR_STATE = "Could not find list for orders in state %s.";
		public static final String INVALID_TOKEN = "Invalid token %s.";
		public static final String EXPIRATION_DATE = "Token has expired at %s.";
		public static final String ERROR_VALIDATE_TOKEN_SINGNATURE = "Invalid token signature.";
		public static final String WRONG_SINTAX = "Wrong syntax.";
		public static final String ORDER_INSTANCE_NULL = "Order instance id for order %s is null.";
		public static final String TOO_BIG_PUBLIC_KEY = "Too big public key.";
		public static final String TOO_BIG_USER_DATA_FILE_CONTENT = "Too big user data file.";
	}
	
	public static class Fatal {
		public static final String UNABLE_CONNECTION_XMPP = "Unable to connect to XMPP server.";
		public static final String DATABASE_MANAGER_ERROR = "Error instantiating database manager.";
		public static final String PUBLIC_KEY_ERROR = "Error reading public key file %s.";
		public static final String INVALID_KEYSTONE_URL = "Invalid Keystone_V3_URL %s.";
		public static final String NO_CLASS_UNDER_REPOSITORY = "No %s class found.";
		public static final String INITIALIZATION_NOT_POSSIBLE = "Unable to initialize HttpRequestUtil.";
		public static final String DEFAULT_NETWORK_NOT_FOUND = "Default network not found.";
		public static final String EXTERNAL_NETWORK_NOT_FOUND = "External network not found.";
		public static final String NEUTRO_ENDPOINT_NOT_FOUND = "Neutron endpoint not found.";
		public static final String RESOURCES_FILE_NOT_FOUND = "No %s file was found at resources.";
	}
	
	public static class Debug {
		public static final String SEARCHING_NETWORK_PORT = "Searching the network port of the virtual machine %s with token %s.";
	}
	
	public static class Out {
		public static final String COULD_NOT_ROLLBACK_TRANSACTION = "Could not rollback transaction.";
	}
	
	public static class Warn {
		public static final String INSTANCE_ALREADY_DELETED = "Instance has already been deleted.";
		public static final String USER_DATA_NOT_ENCODE = "Could not encode user data; sending request without it.";
		public static final String COULD_NOT_RETRIEVE_ROOT_VOLUME = "Root volume could not be retrieved for virtual machine %s; assigning -1 to disk size.";
		public static final String SIGNATURE_NOT_GENERATE = "Could not generate signature.";
		public static final String NETWORK_NOT_FOUND = "Network with id %s not found, trying to delete security group.";
		public static final String COULD_NOT_NOTIFY_REQUESTING_MEMBER = "Could not notify requesting member %s for order %s.";
		public static final String NOT_POSSIBLE_ADD_EXTRA_USER_DATA_FILE_CONTENT_NULL = "Unable to add the extra user data file; content is null.";
		public static final String NOT_POSSIBLE_ADD_EXTRA_USER_DATA_FILE_TYPE_NULL = "Unable to add the extra user data file; file type is null.";
	}
	
	public static class Info {
		public static final String REQUEST_RECEIVED_FOR_NEW_ORDER = "New %s order request received %s.";
		public static final String REQUEST_RECEIVED_FOR_GET_ALL_ORDER = "Get the status of all %s order requests received.";
		public static final String REQUEST_RECEIVED_FOR_GET_ORDER = "Get request for %s order %s received.";
		public static final String REQUEST_RECEIVED_FOR_DELETE_ORDER = "Delete %s order %s received.";
		public static final String REQUEST_RECEIVED_FOR_USER_INFORMATION = "User %s information request for member %s received.";
		public static final String REQUEST_RECEIVED_FOR_GET_ALL_IMAGES = "Get all images request received.";
		public static final String REQUEST_RECEIVED_FOR_GET_IMAGE = "Get image request for %s received.";
		public static final String REQUEST_RECEIVED_FOR_NEW_TOKEN_CREATE = "New token create request received; size of credentials is: %s.";
		public static final String REQUEST_RECEIVED_FOR_NEW_VERSION = "New version request received.";
		public static final String RECEIVED_REQUEST_FOR_ORDER = "Received request for order: %s.";
		public static final String DELETING_INSTANCE = "Deleting instance %s with tokens %s.";
		public static final String GETTING_INSTANCE = "Getting instance %s with tokens %s.";
		public static final String CREATING_FLOATING_IP = "Creating floating ip in the %s with tokens %s.";
		public static final String DELETING_FLOATING_IP = "Deleting floating ip %s with tokens %s.";
		public static final String ACTIVATING_NEW_ORDER = "Activating new order request received.";
		public static final String GETTING_INTANCE_FOR_ORDER = "Trying to get an instance for order %s.";
		public static final String INSTANCE_STATE_FAILED = "Instance state is failed for order %s.";
	}
	
	public static class Error {
		public static final String DELETE_INSTANCE_PLUGIN_NOT_IMPLEMENTED = "No deleteInstance plugin implemented for order %s.";
		public static final String INVALID_DATASTORE_DRIVE = "Invalid datastore driver.";
		public static final String WHILE_GETTING_NEW_CONNECTION = "Error while getting a new connection from the connection pool.";
		public static final String COULD_NOT_CLOSE_STATEMENT = "Could not close statement.";
		public static final String COULD_NOT_CLOSE_CONNECTION = "Could not close connection.";
		public static final String COULD_NOT_ROLLBACK_TRANSACTION = "Could not rollback transaction.";
		public static final String INVALID_TOKEN_VALUE = "Invalid token value: %s.";
		public static final String ORDER_CANNOT_BE_COMPLETED = "Order cannot be completed; template, zone and default network IDs are required parameters.";
		public static final String COULD_NOT_DELETE_INSTANCE = "Could not delete instance %s.";
		public static final String CREATING_TIMESTAMP_TABLE = "Error creating timestamp table.";
		public static final String COULD_NOT_ADD_TIMESTAMP = "Could not add timestamp.";
		public static final String COULD_NOT_GENERATING_JSON = "An error occurred when generating json.";
		public static final String PROJECT_ID_NOT_SPECIFIED = "Project id is not specified.";
		public static final String WHILE_GETTING_ATTACHMENT_INSTANCE = "There was an exception while getting attachment instance from json.";
		public static final String NOT_POSSIBLE_RETRIEVE_NETWORK_ID = "It was not possible retrieve network id from json %s.";
		public static final String NOT_POSSIBLE_DELETE_NETWORK = "It was not possible delete network with id %s.";
		public static final String NOT_POSSIBLE_DELETE_SECURITY_GROUP = "It was not possible delete security group with id %s.";
		public static final String NOT_POSSIBLE_GET_NETWORK = "It was not possible to get network informations from json %s.";
		public static final String WHILE_GETTING_VOLUME_INSTANCE = "There was an exception while getting volume instance.";
		public static final String INSTANCE_STATE_NOT_MAPPED = "%s was not mapped to a well-defined OpenStack instance state when %s were implemented.";
		public static final String INSTANCE_TYPE_NOT_DEFINED = "Instance type not defined.";
		public static final String THREAD_INTERRUPTED = "Thread interrupted.";
		public static final String UNEXPECTED = "Unexpected error.";
		public static final String WHILE_GETTING_INSTANCE_FROM_CLOUD = "Error while getting instance from the cloud.";
		public static final String WHILE_GETTING_INSTANCE_FOR_ORDER = "Error while trying to get an instance for order %s.";
		public static final String ORDER_ALREADY_CLOSED = "Order %s is already in the closed state.";
		public static final String WHILE_CONSUMING_RESPONSE = "Error while consuming the response %s.";
		public static final String COULD_NOT_CLOSE_FILE = "Could not close file %s.";
	}
}

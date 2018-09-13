package org.fogbowcloud.ras.core.constants;

public class Messages {

	public static class Exception {
		public static final String EXPIRED_TOKEN = "Expired tokens exception";
		public static final String PLUGIN_FOR_REQUEST_INSTANCE_NOT_IMPLEMENTED = "No requestInstance plugin implemented for order %s.";
		public static final String RETURNED_NULL_VALUE = "Plugin returned a null value for the instanceId.";
		public static final String ORDER_TYPE_NOT_SUPPORTED = "Not supported order type %s.";
		public static final String QUOTA_ENDPOINT_NOT_IMPLEMENTED = "Not yet implemented quota endpoint for %s.";
		public static final String ORDER_ALREADY_EXIST = "Order already exists";
		public static final String ORDER_NOT_EXIST = "Order doesn't exist";
		public static final String FATAL_ERROR = "Fatal error exception";
		public static final String FOGBOW_RAS = "Fogbow RAS exception";
		public static final String INSTANCE_NOT_FOUND = "Instance not found exception";
		public static final String INVALID_PARAMETER = "Invalid parameter exception";
		public static final String INVALID_CREDENTIALS = "Invalid Credentials";
		public static final String RESOURCES_NOT_AVAILABLE = "No available resources exception";
		public static final String QUOTA_EXCEEDED = "Quota exceeded exception";
		public static final String UNAUTHENTICATED_ERROR = "Unauthenticated error";
		public static final String UNAUTHENTIC_TOKENS = "Unauthentic tokens exception";
		public static final String UNAUTHORIZED_ERROR = "Unauthorized error";
		public static final String UNAVAILABLE_PROVIDER = "Unavailable provider exception";
		public static final String UNEXPECTED = "Unexpected exception";
		public static final String UNABLE_RETRIEVE_RESPONSE_FROM_PROVIDING_MEMBER = "Unable to retrieve the response from providing member: %s.";
		public static final String ATTEMPTING_ADD_NULL_ORDER = "Attempting to add a null order.";
		public static final String ATTEMPTING_REMOVE_NULL_ORDER = "Attempting to remove a null order.";
		public static final String PORT_NOT_FOUND = "None port found of the virtual machine %s and default network %s.";
		public static final String IRREGULAR_PORT_SIZE = "Irregular ports size %s of the virtual machine %s and default network %s.";
		public static final String MISMATCHING_RESOURCE_TYPE = "Mismatching resource type";
		public static final String REQUESTER_NOT_OWN_ORDER = "Requester does not own order";
		public static final String PROVIDING_MEMBER_NOT_CORRECT = "This is not the correct providing member.";
		public static final String INSTANCE_ID_NOT_INFORMED = "No instance id informed";
		public static final String RESOURCE_TYPE_NOT_IMPLEMENTED = "Resouce type not implemented yet.";
		public static final String CANNOT_PROCESS_ORDER_REQUEST_NULL = "Cannot process new order request. Order reference is null.";
		public static final String ORDER_ID_ALREADY_ACTIVATED = "Order with id %s is already in active orders map.";
		public static final String REMOVE_ORDER_NOT_ACTIVE = "Tried to remove order %s from the active orders but it was not active.";
		public static final String COULD_NOT_FIND_LIST_FOR_STATE = "Could not find list for state %s.";
		public static final String COULD_NOT_FIND_DESTINATION_LIST_FOR_STATE = "Could not find destination list for state %s.";
		public static final String INVALID_TOKENS = "Invalid tokens: %s.";
		public static final String EXPIRATION_DATE = "Expiration date: %s.";
		public static final String ERROR_VALIDATE_TOKEN_SINGNATURE = "Error while trying to validate sing of the tokens.";
		public static final String IRREGULAR_SYNTAX = "Irregular syntax.";
		public static final String ORDER_INSTANCE_NULL = "Order instance id for order [%s] is null.";
	}
	
	public static class Fatal {
		public static final String UNABLE_CONNECTION_XMPP = "Unable to connect to XMPP, check XMPP configuration file.";
		public static final String DATABASE_MANAGER_ERROR = "Error instantiating database manager.";
		public static final String PUBLIC_KEY_ERROR = "Error reading public key: %s.";
		public static final String INVALID_KEYSTONE_URL = "Invalid Keystone_V3_URL %s.";
		public static final String NO_CLASS_UNDER_REPOSITORY = "No %s class under this repository. Please inform a valid class.";
		public static final String INITIALIZATION_NOT_POSSIBLE = "It is not possible to initialize HttpRequestUtil.";
		public static final String DEFAULT_NETWORK_NOT_FOUND = "Default network not found";
		public static final String EXTERNAL_NETWORK_NOT_FOUND = "External network not found";
		public static final String NEUTRO_ENDPOINT_NOT_FOUND = "Neutro endpoint not found";
		public static final String RESOURCES_FILE_NOT_FOUND = "No %s file was found at resources.";
	}
	
	public static class Debug {
		public static final String SEARCHING_NETWORK_PORT = "Searching the network port of the VM %s with tokens %s.";
	}
	
	public static class Out {
		public static final String COULD_NOT_ROLLBACK_TRANSACTION = "Couldn't rollback transaction.";
	}
	
	public static class Warn {
		public static final String INSTANCE_ALREADY_DELETED = "Instance has already been deleted.";
		public static final String USER_DATA_NOT_ENCODE = "Could not encode user data. Sending request without it.";
		public static final String COULD_NOT_RETRIEVE_ROOT_VOLUME = "Root volume could not be retrieved for virtual machine %s. Assigning -1 to disk size.";
		public static final String SIGNATURE_NOT_GENERATE = "Couldn't generate signature.";
		public static final String NETWORK_NOT_FOUND = "Network with id %s not found, trying to delete security group.";
		public static final String COULD_NOT_NOTIFY_REQUESTING_MEMBER = "Could not notify requesting member %s for order %s.";
		public static final String NOT_POSSIBLE_ADD_EXTRA_USER_DATA_FILE_CONTENT_NULL = "It was not possible to add the extra user data file, whose content is null.";
		public static final String NOT_POSSIBLE_ADD_EXTRA_USER_DATA_FILE_TYPE_NULL = "It was not possible to add the extra user data file, the extra user data file type is null.";
	}
	
	public static class Info {
		public static final String REQUEST_RECEIVED_FOR_ORDER = "Request received for order: %s.";
		public static final String REQUEST_RECEIVED_FOR_NEW_ORDER = "New %s order request received <%s>.";
		public static final String REQUEST_RECEIVED_FOR_GET_ALL_ORDER = "Get the status of all %s order requests received.";
		public static final String REQUEST_RECEIVED_FOR_GET_ORDER = "Get request for %s order <%s> received.";
		public static final String REQUEST_RECEIVED_FOR_DELETE_ORDER = "Delete %s order <%s> received.";
		public static final String REQUEST_RECEIVED_FOR_USER_INFORMATION = "User %s information request for member <%s> received.";
		public static final String REQUEST_RECEIVED_FOR_GET_ALL_IMAGES = "Get all images request received.";
		public static final String REQUEST_RECEIVED_FOR_GET_IMAGE = "Get image request for <%s> received.";
		public static final String REQUEST_RECEIVED_FOR_NEW_TOKEN_CREATE = "New token create request received; size of credentials is: %s.";
		public static final String REQUEST_RECEIVED_FOR_NEW_VERSION = "New version request received.";
		public static final String RECEIVED_REQUEST_FOR_ORDER = "Received request for order: %s.";
		public static final String DELETING_INSTANCE = "Deleting instance %s with tokens %s.";
		public static final String GETTING_INSTANCE = "Getting instance %s with tokens %s.";
		public static final String CREATING_FLOATING_IP = "Creating floating ip in the %s with tokens %s.";
		public static final String DELETING_FLOATING_IP = "Deleting floating ip %s with tokens %s.";
		public static final String ACTIVATING_NEW_ORDER = "Activating new order request received.";
		public static final String GETTING_INTANCE_FOR_ORDER = "Trying to get an instance for order [%s]";
		public static final String INSTANCE_STATE_FAILED = "Instance state is failed for order [%s]";
	}
	
	public static class Error {
		public static final String DELETE_INSTANCE_PLUGIN_NOT_IMPLEMENTED = "No deleteInstance plugin implemented for order %s.";
		public static final String INVALID_DATASTORE_DRIVE = "Invalid datastore driver";
		public static final String WHILE_GETTING_NEW_CONNECTION = "Error while getting a new connection from the connection pool.";
		public static final String COULD_NOT_CLOSE_STATEMENT = "Couldn't close statement";
		public static final String COULD_NOT_CLOSE_CONNECTION = "Couldn't close connection";
		public static final String COULD_NOT_ROLLBACK_TRANSACTION = "Couldn't rollback transaction.";
		public static final String INVALID_TOKEN_VALUE = "Invalid token value: %s.";
		public static final String ORDER_CANNOT_BE_COMPLETED = "Order cannot be completed. Template, zone and default network IDs are required parameters.";
		public static final String COULD_NOT_DELETE_INSTANCE = "Could not delete instance %s.";
		public static final String CREATING_TIMESTAMP_TABLE = "Error creating timestamp table.";
		public static final String COULD_NOT_ADD_TIMESTAMP = "Couldn't add timestamp.";
		public static final String COULD_NOT_GENERATING_JSON = "An error occurred when generating json.";
		public static final String PROJECT_ID_NOT_SPECIFIED = "Project id is not specified.";
		public static final String WHILE_GETTING_ATTACHMENT_INSTANCE = "There was an exception while getting attchment instance from json.";
		public static final String NOT_POSSIBLE_RETRIEVE_NETWORK_ID = "It was not possible retrieve network id from json %s.";
		public static final String NOT_POSSIBLE_DELETE_NETWORK = "It was not possible delete network with id %s.";
		public static final String NOT_POSSIBLE_DELETE_SECURITY_GROUP = "It was not possible delete security group with id %s.";
		public static final String NOT_POSSIBLE_GET_NETWORK = "It was not possible to get network informations from json %s.";
		public static final String WHILE_GETTING_VOLUME_INSTANCE = "There was an exception while getting volume instance.";
		public static final String INSTANCE_STATE_NOT_MAPPED = "%s was not mapped to a well-defined OpenStack instance state when %s were implemented.";
		public static final String INSTANCE_TYPE_NOT_DEFINED = "Instance type not defined.";
		public static final String THREAD_INTERRUPTED = "Thread interrupted";
		public static final String UNEXPECTED = "Unexpected error";
		public static final String WHILE_GETTING_INSTANCE_FROM_CLOUD = "Error while getting instance from the cloud.";
		public static final String WHILE_GETTING_INSTANCE_FOR_ORDER = "Error while trying to get an instance for order: %s.";
		public static final String ORDER_ALREADY_CLOSED = "Order [%s] is already in the closed state.";
		public static final String WHILE_CONSUMING_RESPONSE = "Error while consuming the response: %s.";
		public static final String COULD_NOT_CLOSE_FILE = "Could not close file %s.";
	}

}

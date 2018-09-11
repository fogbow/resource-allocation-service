package org.fogbowcloud.ras.core.constants;

public class Messages {

	public static class Exception {
		public static final String EXPIRED_TOKEN = "Expired tokens exception";
		public static final String PLUGIN_FOR_REQUEST_INSTANCE_NOT_IMPLEMENTED = "No requestInstance plugin implemented for order ";
		public static final String NULL_VALUE_RETURNED = "Plugin returned a null value for the instanceId.";
		public static final String ORDER_TYPE_NOT_SUPPORTED = "Not supported order type ";
		public static final String QUOTA_ENDPOINT_NOT_IMPLEMENTED = "Not yet implemented quota endpoint for ";
		public static final String ORDER_ALREADY_EXISTS = "Order already exists";
		public static final String ORDER_NOT_EXISTS = "Order doesn't exist";
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
		public static final String UNABLE_RESPONSE_FROM_PROVIDING_MEMBER = "Unable to retrieve the response from providing member: ";
		public static final String ADD_NULL_ORDER_ATTEMPTING = "Attempting to add a null order.";
		public static final String REMOVE_NULL_ORDER_ATTEMPTING = "Attempting to remove a null order.";
		public static final String PORT_NOT_FOUND = "None port found of the virtual machine(%s) and default network(%s) ";
		public static final String IRREGULAR_PORT_SIZE = "Irregular ports size(%s) of the virtual machine(%s) and default network(%s) ";
		public static final String DEFAULT_NETWORK_NOT_FOUND = "Default network not found";
		public static final String EXTERNAL_NETWORK_NOT_FOUND = "External network not found";
		public static final String NEUTRO_ENDPOINT_NOT_FOUND = "Neutro endpoint not found";
		public static final String RESOURCE_TYPE_MISMATCHING = "Mismatching resource type";
		public static final String NOT_OWN_ORDER_REQUESTER = "Requester does not own order";
		public static final String NOT_CORRECT_PROVIDING_MEMBER = "This is not the correct providing member";
		public static final String INSTANCE_ID_NOT_INFORMED = "No instance id informed";
		public static final String NOT_IMPLEMENTED = "Not yet implemented.";
		public static final String CAN_NOT_PROCESS_ORDER_REQUEST_NULL = "Cannot process new order request. Order reference is null.";
		public static final String ORDER_ID_ALREADY_ACTIVATED = "Order with id %s is already in active orders map.";
		public static final String TRY_TO_REMOVE_ORDER_NOT_ACTIVE = "Tried to remove order %s from the active orders but it was not active";
		public static final String COULD_NOT_FIND_LIST_FOR_STATE = "Could not find list for state %s";
		public static final String COULD_NOT_FIND_DESTINATION_LIST_FOR_STATE = "Could not find destination list for state %s";
		public static final String INVALID_TOKENS = "Invalid tokens: ";
		public static final String EXPIRATION_DATE = "Expiration date: ";
		public static final String TOKEN_SIGNATURE_VALIDATION_ERROR = "Error while trying to validate sing of the tokens.";
	}
	
	public static class Fatal {
		public static final String XMPP_CONNECTION_UNABLE = "Unable to connect to XMPP, check XMPP configuration file.";
		public static final String DATABASE_MANAGER_ERROR = "Error instantiating database manager";
		public static final String PUBLIC_KEY_ERROR = "Error reading public key: ";
		public static final String INVALID_KEYSTONE_V3_URL = "Invalid Keystone_V3_URL ";
		public static final String NO_CLASS_UNDER_REPOSITORY = "No %s class under this repository. Please inform a valid class.";
		public static final String INITIALIZATION_NOT_POSSIBLE = "It is not possible to initialize HttpRequestUtil.";
	}

}

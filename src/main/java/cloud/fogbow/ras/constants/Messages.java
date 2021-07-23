package cloud.fogbow.ras.constants;

public class Messages {

    public static class Exception {
        public static final String ACTIVE_SOURCE_COMPUTE = "The source compute needs to be paused or hibernated before taking the snapshot.";
        public static final String ADMIN_ROLE_NOT_SPECIFIED = "Admin role is not specified in the configuration file.";
        public static final String CLOUD_NAMES_DO_NOT_MATCH = "The embedded resource has not been instantiated in the same cloud.";
        public static final String DEFAULT_CREDENTIALS_NOT_FOUND = "Default credentials not found.";
        public static final String DEFAULT_NETWORK_NOT_FOUND = "Default network not found.";
        public static final String DEFAULT_ROLE_NAME_IS_INVALID = "Default role name is invalid.";
        public static final String DELETE_OPERATION_ALREADY_ONGOING = "Delete operation is already on-going.";
        public static final String DEPENDENCY_DETECTED_S_S = "Cannot delete order '%s'. There are other orders associated with it: ids '%s'. You should remove those dependencies first.";
        public static final String EMPTY_PROPERTY_MAP = "Empty property getCloudUser.";
        public static final String ERROR_WHILE_CHECKING_INSTANCE_STATUS_ATTEMPTS_LEFT_D = "Error while checking instance status; attempts left: %d";
        public static final String ERROR_WHILE_CREATING_RESOURCE_S = Log.ERROR_WHILE_CREATING_RESOURCE_S;
        public static final String ERROR_WHILE_GETTING_RESOURCE_S_FROM_CLOUD = Log.ERROR_WHILE_GETTING_RESOURCE_S_FROM_CLOUD;
        public static final String ERROR_WHILE_GETTING_VOLUME_INSTANCE = Log.ERROR_WHILE_GETTING_VOLUME_INSTANCE;
        public static final String ERROR_WHILE_PROCESSING_VOLUME_REQUIREMENTS = Log.ERROR_WHILE_PROCESSING_VOLUME_REQUIREMENTS;
        public static final String ERROR_WHILE_REMOVING_RESOURCE_S_S = Log.ERROR_WHILE_REMOVING_RESOURCE_S_S;
        public static final String ERROR_WHILE_REMOVING_VM_S_S = "Error while removing virtual machine: %s, with response: %s.";
        public static final String ERROR_WHILE_REMOVING_VOLUME_IMAGE_S_S = Log.ERROR_WHILE_REMOVING_VOLUME_IMAGE_S_S;
        public static final String EXTERNAL_NETWORK_NOT_FOUND = "External network not found.";
        public static final String FAILED_TO_GET_QUOTA = "Failed to get quota.";
        public static final String GENERIC_EXCEPTION_S = Log.GENERIC_EXCEPTION_S;
        public static final String HIBERNATE_OPERATION_ONGOING = "The virtual Machine is already being hibernated.";
        public static final String IMAGE_NOT_FOUND = "Image not found.";
        public static final String INCORRECT_PROVIDER = "Incorrect provider.";
        public static final String INCORRECT_REQUESTING_PROVIDER = "Mismatch on requesting provider information.";
        public static final String INSTANCE_NOT_FOUND = "Instance not found.";
        public static final String INSTANCE_NULL_S = "There is no active instance with id: <%s>.";
        public static final String INVALID_CIDR_S = "CIDR %s is not valid.";
        public static final String INVALID_CIDR_FORMAT_S = "The cidr %s does not follow the expected format.";
        public static final String INVALID_PARAMETER = "Invalid parameter.";
        public static final String INVALID_PARAMETER_S = "Invalid parameter: %s.";
        public static final String INVALID_REGION_NAME_S = "The region name '%s' is invalid.";
        public static final String INVALID_RESOURCE = "Invalid resource type.";
        public static final String JOB_HAS_FAILED = "Instance associated to job %s has failed.";
        public static final String JOB_TIMEOUT = "Instance associated to job %s has failed, because it took too long to process.";
        public static final String MANY_NETWORKS_NOT_ALLOWED = "This cloud does not allow connection to more than one network.";
        public static final String MISMATCHING_RESOURCE_TYPE = "Mismatching resource type.";
        public static final String MULTIPLE_SECURITY_GROUPS_EQUALLY_NAMED_S = "There should be exactly one security group with name %s.";
        public static final String NEUTRON_ENDPOINT_NOT_FOUND = "Neutron endpoint not found.";
        public static final String NON_EXISTENT_REQUEST = "Request does not exist.";
        public static final String NOT_FOUND_ORDER_ID_S = "Order ID %s not found.";
        public static final String NO_CLOUD_SPECIFIED = "No cloud names specified in ras.conf file.";
        public static final String NO_IMAGES_PUBLISHER = "No virtual machine images publishers specified in azure cloud.conf.";
        public static final String NO_MATCHING_FLAVOR = "No matching flavor.";
        public static final String NO_PROJECT_ID = "No projectId in local token.";
        public static final String NO_SECURITY_GROUP_FOUND_S = "There is no security group with the id %s";
        public static final String NULL_VALUE_RETURNED = "Plugin returned a null value for the instanceId.";
        public static final String PAUSE_OPERATION_ONGOING = "The virtual Machine is already being paused.";
        public static final String POLICY_CLASS_NOT_SPECIFIED = "Policy class is not specified in the configuration file.";
        public static final String POLICY_FILE_NAME_NOT_SPECIFIED = "Policy file name is not specified in the configuration file.";
        public static final String PORT_NOT_FOUND_S = "No port found connecting virtual machine %s to default network %s.";
        public static final String PROVIDERS_DONT_MATCH = "The attachment provider does not match with the compute and/or volume providers.";
        public static final String PROVIDER_IS_NOT_AUTHORIZED = "Provider is not authorized.";
        public static final String REQUESTER_DOES_NOT_OWN_REQUEST = "Requester does not own request.";
        public static final String REQUEST_ALREADY_EXIST = "Request already exists.";
        public static final String REQUEST_ID_ALREADY_ACTIVATED_S = "Request %s has already been activated.";
        public static final String REQUEST_INSTANCE_NULL_S = "Request instance id for request %s is null.";
        public static final String RESOURCE_GROUP_LIMIT_EXCEEDED = "Resource group limit exceeded.";
        public static final String RESOURCE_TYPE_NOT_COMPATIBLE_S = "Resource type not compatible with %s request.";
        public static final String RESOURCE_TYPE_NOT_IMPLEMENTED = "Resource type not yet implemented.";
        public static final String RESUME_OPERATION_ONGOING = "The virtual Machine is already being resumed.";
        public static final String RULE_NOT_AVAILABLE = "Rule not available for deletion.";
        public static final String SECURITY_GROUP_EQUALLY_NAMED_S_NOT_FOUND_S = "There is no security group with name: %s.";
        public static final String SIGNALING_PROVIDER_DIFFERENT_OF_PROVIDER_S_S = "Signalling provider %s is not the provider %s.";
        public static final String STOP_OPERATION_ONGOING = "The virtual machine is already being stopped.";
        public static final String TOO_BIG_USER_DATA_FILE_CONTENT = "Too big user data file.";
        public static final String TRYING_TO_USE_RESOURCES_FROM_ANOTHER_USER = "Trying to use resources from another user.";
        public static final String UNABLE_TO_COMPLETE_REQUEST_CLOUDSTACK = "Unable to complete request.";
        public static final String UNABLE_TO_COMPLETE_REQUEST_DISK_OFFERING_CLOUDSTACK = "Unable to complete disk offering.";
        public static final String UNABLE_TO_COMPLETE_REQUEST_SERVICE_OFFERING_CLOUDSTACK = "Unable to complete service offering.";
        public static final String UNABLE_TO_CREATE_ATTACHMENT = Log.UNABLE_TO_CREATE_ATTACHMENT;
        public static final String UNABLE_TO_CREATE_NETWORK_RESERVE_S = "Unable to create network reserve with CIDR: <%s>. This address range is likely in use.";
        public static final String UNABLE_TO_DESERIALIZE_SYSTEM_USER = "Unable to deserialize system user.";
        public static final String UNABLE_TO_FIND_CLASS_S = "Unable to find class %s.";
        public static final String UNABLE_TO_FIND_LIST_FOR_REQUESTS_S = "Unable to find list for requests in state %s.";
        public static final String UNABLE_TO_GENERATE_JSON = Log.UNABLE_TO_GENERATE_JSON;
        public static final String UNABLE_TO_GET_ATTACHMENT_INSTANCE = Log.UNABLE_TO_GET_ATTACHMENT_INSTANCE;
        public static final String UNABLE_TO_GET_NETWORK_S = Log.UNABLE_TO_GET_NETWORK_S;
        public static final String UNABLE_TO_LOAD_FLAVOURS = Log.UNABLE_TO_LOAD_FLAVOURS;
        public static final String UNABLE_TO_MATCH_REQUIREMENTS = "Unable to match requirements.";
        public static final String UNABLE_TO_REMOVE_INACTIVE_REQUEST_S = "Unable to remove inactive request %s.";
        public static final String UNABLE_TO_RETRIEVE_NETWORK_ID_S = Log.UNABLE_TO_RETRIEVE_NETWORK_ID_S;
        public static final String UNABLE_TO_RETRIEVE_RESPONSE_FROM_PROVIDER_S = "Unable to retrieve response from provider: %s.";
        public static final String UNEXPECTED_ERROR = "Unexpected error.";
        public static final String UNEXPECTED_JOB_STATUS = "Job status must be one of {0, 1, 2}.";
        public static final String UNEXPECTED_OPERATION_S = "Unexpected operation: %s.";
        public static final String UNSUPPORTED_REQUEST_TYPE_S = "Request type %s not supported.";
        public static final String USER_DOES_NOT_HAVE_ENOUGH_PERMISSION = "User does not have permission to perform operation";
        public static final String USER_DOES_NOT_HAVE_REQUIRED_ROLE = "User does not have a role required by operation.";
        public static final String USER_IS_NOT_FINANCIALLY_AUTHORIZED = "User is not financially authorized.";
        public static final String VIRTUAL_MACHINE_ALREADY_HIBERNATED = "The Virtual Machine has already been hibernated.";
        public static final String VIRTUAL_MACHINE_ALREADY_PAUSED = "The Virtual Machine has already been paused.";
        public static final String VIRTUAL_MACHINE_IS_NOT_RUNNING = "The Virtual Machine is not running.";
        public static final String VIRTUAL_MACHINE_ALREADY_STOPPED = "The Virtual Machine has already been stopped.";
        public static final String WRONG_POLICY_TYPE = "Wrong policy type. Type should be '%s' but is '%s'.";
        public static final String WRONG_URI_SYNTAX_S = "Wrong syntax for endpoint %s.";
        public static final String VIRTUAL_MACHINE_IS_NOT_PAUSED_OR_HIBERNATED_OR_STOPPED = "The virtual machine is not paused or hibernated or stopped.";
    }

    public static class Log {
        public static final String ACTIVATING_NEW_REQUEST = "Activating new request.";
        public static final String ASYNCHRONOUS_PUBLIC_IP_STATE_S = "The asynchronous public ip request %s is in the state %s.";
        public static final String CHANGE_TO_DEFAULT_RESOURCE_GROUP = "Changing to the default resource group.";
        public static final String CONNECTING_UP_PACKET_SENDER = "Connecting XMPP packet sender.";
        public static final String CONTENT_SECURITY_GROUP_NOT_DEFINED = "The content of SecuriryGroups in the VirtualNetwork template is not defined.";
        public static final String CONTENT_SECURITY_GROUP_EMPTY = "The content of SecurityGroups is empty.";
        public static final String COULD_NOT_FIND_DEPENDENCY_S_S = "Could not find dependency %s for order %s.";
        public static final String DELETING_INSTANCE_S_WITH_TOKEN_S = "Deleting instance %s with token %s.";
        public static final String DELETING_INSTANCE_S = "Deleting instance %s.";
        public static final String DISK_OFFERING_COMPATIBLE_NOT_FOUND = "There is not disk offering compatible with volume order size.";
        public static final String DISK_OFFERING_CUSTOMIZED_NOT_FOUND ="There is not disk offering customized in the cloud.";
        public static final String END_ASYNC_INSTANCE_CREATION_S = "End instance (%s) creation.";
        public static final String END_ATTACH_DISK_ASYNC_BEHAVIOUR = "End asynchronous attach disk.";
        public static final String END_CREATE_DISK_ASYNC_BEHAVIOUR = "End asynchronous create disk.";
        public static final String END_CREATE_PUBLIC_IP_ASYNC_BEHAVIOUR = "End asynchronous create public IP address.";
        public static final String END_CREATE_VM_ASYNC_BEHAVIOUR = "End asynchronous create virtual machine";
        public static final String END_CREATE_VNET_ASYNC_BEHAVIOUR = "End asynchronous create virtual network.";
        public static final String END_DELETE_DISK_ASYNC_BEHAVIOUR = "End asynchronous delete disk.";
        public static final String END_DELETE_NIC_ASYNC_BEHAVIOUR = "End asynchronous delete network interface.";
        public static final String END_DELETE_PUBLIC_IP_ASYNC_BEHAVIOUR = "End asynchronous delete public IP address.";
        public static final String END_DELETE_RESOURCES_ASYNC_BEHAVIOUR = "End asynchronous delete resources.";
        public static final String END_DELETE_SECURITY_GROUP_ASYNC_BEHAVIOUR = "End asynchronous delete security group.";
        public static final String END_DELETE_VM_ASYNC_BEHAVIOUR = "End asynchronous delete virtual machine";
        public static final String END_DELETE_VNET_ASYNC_BEHAVIOUR = "End asynchronous delete virtual network.";
        public static final String END_DETACH_DISK_ASYNC_BEHAVIOUR = "End asynchronous detach disk.";
        public static final String END_DETACH_PUBLIC_IP_ASYNC_BEHAVIOUR = "End asynchronous disassociate public IP address.";
        public static final String END_DETACH_RESOURCES_ASYNC_BEHAVIOUR = "End asynchronous disassociate resources.";
        public static final String END_UPDATE_NIC_ASYNC_BEHAVIOUR = "End asynchronous update network interface.";
        public static final String ERROR_ASYNC_INSTANCE_CREATION_S = "Error at async instance (%s) creation.";
        public static final String ERROR_ATTACH_DISK_ASYNC_BEHAVIOUR = "Error while attaching disk asynchronously.";
        public static final String ERROR_CREATE_DISK_ASYNC_BEHAVIOUR = "Error while creating disk asynchronously.";
        public static final String ERROR_CREATE_PUBLIC_IP_ASYNC_BEHAVIOUR = "Error while creating public IP address asynchronously.";
        public static final String ERROR_CREATE_VM_ASYNC_BEHAVIOUR = "Error while creating virtual machine asynchronously.";
        public static final String ERROR_CREATE_VNET_ASYNC_BEHAVIOUR = "Error while creating virtual network asynchronously.";
        public static final String ERROR_DELETE_DISK_ASYNC_BEHAVIOUR = "Error while deleting disk asynchronously.";
        public static final String ERROR_DELETE_NIC_ASYNC_BEHAVIOUR = "Error while deleting network interface asynchronously.";
        public static final String ERROR_DELETE_PUBLIC_IP_ASYNC_BEHAVIOUR = "Error while deleting public IP address asynchronously.";
        public static final String ERROR_DELETE_RESOURCES_ASYNC_BEHAVIOUR = "Error while deleting resources asynchronously.";
        public static final String ERROR_DELETE_SECURITY_GROUP_ASYNC_BEHAVIOUR = "Error while deleting security group asynchronously.";
        public static final String ERROR_DELETE_VM_ASYNC_BEHAVIOUR = "Error while deleting virtual machine asynchronously.";
        public static final String ERROR_DELETE_VNET_ASYNC_BEHAVIOUR = "Error while deleting virtual network asynchronously.";
        public static final String ERROR_DETACH_DISK_ASYNC_BEHAVIOUR = "Error while detaching disk asynchronously.";
        public static final String ERROR_DETACH_PUBLIC_IP_ASYNC_BEHAVIOUR = "Error while detaching public IP address asynchronously.";
        public static final String ERROR_DETACH_RESOURCES_ASYNC_BEHAVIOUR = "Error while disassociating resources asynchronously.";
        public static final String ERROR_ON_REQUEST_ASYNC_PLUGIN = "Error on request at async plugin.";
        public static final String ERROR_UPDATE_NIC_ASYNC_BEHAVIOUR = "Error while updating network interface asynchronously.";
        public static final String ERROR_WHILE_ATTACHING_VOLUME_S_WITH_RESPONSE_S = "Error while attaching volume image disk: %s, with response: %s.";
        public static final String ERROR_WHILE_ATTACHING_VOLUME_GENERAL_S = "Error while attaching volume with response: %s.";
        public static final String ERROR_WHILE_CONVERTING_INSTANCE_ID_S = "Error while converting instance id %s to integer.";
        public static final String ERROR_WHILE_CONVERTING_TO_INTEGER = "Error while converting the input string to an integer.";
        public static final String ERROR_WHILE_CREATING_CLIENT = "Error while creating client.";
        public static final String ERROR_WHILE_CREATING_IMAGE_S = "Error while creating a image from template: %s.";
        public static final String ERROR_WHILE_CREATING_NETWORK_S = "Error while creating a network from template: %s.";
        public static final String ERROR_WHILE_CREATING_RESOURCE_S = "Error while creating %s.";
        public static final String ERROR_WHILE_CREATING_RESPONSE_BODY = "Error while creating response body.";
        public static final String ERROR_WHILE_CREATING_SECURITY_GROUPS_S = "Error while creating a security groups from template: %s.";
        public static final String ERROR_WHILE_DETACHING_VOLUME_S = "Error while detaching volume image disk: %s, with response: %s.";
        public static final String ERROR_WHILE_GETTING_DISK_SIZE = "Error while getting VM disk size.";
        public static final String ERROR_WHILE_GETTING_GROUP_S_S = "Error while getting info about group %s: %s.";
        public static final String ERROR_WHILE_GETTING_RESOURCE_S_FROM_CLOUD = "Error while getting %s from the cloud.";
        public static final String ERROR_WHILE_GETTING_TEMPLATES_S = "Error while getting info about templates: %s.";
        public static final String ERROR_WHILE_GETTING_USER_S_S = "Error while getting info about user %s: %s.";
        public static final String ERROR_WHILE_GETTING_USERS_S = "Error while getting info about users: %s.";
        public static final String ERROR_WHILE_GETTING_VOLUME_INSTANCE = "Error while getting volume instance.";
        public static final String ERROR_WHILE_INSTANTIATING_FROM_TEMPLATE_S = "Error while instantiating an instance from template: %s.";
        public static final String ERROR_WHILE_LOADING_IMAGE_S = "Error while loading the following image: %s";
        public static final String ERROR_WHILE_PROCESSING_ASYNCHRONOUS_REQUEST_INSTANCE_STEP = "Error while It was trying to pass to the next step in the asynchronous request instance.";
        public static final String ERROR_WHILE_PROCESSING_VOLUME_REQUIREMENTS = "Error while processing volume requirements";
        public static final String ERROR_WHILE_REMOVING_RESOURCE_S_S = "An error occurred while removing %s: %s.";
        public static final String ERROR_WHILE_REMOVING_VOLUME_IMAGE_S_S = "Error while removing volume image: %s, with response: %s.";
        public static final String ERROR_WHILE_UPDATING_NETWORK_S = "Error while updating a network from template: %s.";
        public static final String ERROR_WHILE_UPDATING_SECURITY_GROUPS_S = "Error while updating a security groups from template: %s.";
        public static final String FIRST_STEP_CREATE_PUBLIC_IP_ASYNC_BEHAVIOUR = "First step: Public IP Address created and associated with the virtual machine.";
        public static final String FIRST_STEP_CREATE_VNET_ASYNC_BEHAVIOUR = "First step on virtual network creation: Security group created.";
        public static final String FIRST_STEP_DETACH_PUBLIC_IP_ASYNC_BEHAVIOUR = "First step: Public IP Address disassociated from network interface.";
        public static final String FIRST_STEP_DETACH_RESOURCES_ASYNC_BEHAVIOUR = "First step: Public IP address and network security group disassociated from network instance.";
        public static final String GENERIC_EXCEPTION_S = "Operation returned error: %s.";
        public static final String GETTING_INSTANCE_S = "Getting instance %s.";
        public static final String GETTING_QUOTA = "Getting quota.";
        public static final String GET_PUBLIC_KEY = "Get public key received.";
        public static final String HIBERNATING_INSTANCE_S = "Hibernating instance %s.";
        public static final String INCONSISTENT_DIRECTION_S = "The direction (%s) is inconsistent.";
        public static final String INCONSISTENT_PROTOCOL_S = "The protocol (%s) is inconsistent.";
        public static final String INCONSISTENT_RANGE_S = "The range(%s) is inconsistent";
        public static final String INSTANCE_S_HAS_FAILED = "Instance associated to request %s has failed.";
        public static final String INSTANCE_NOT_FOUND_S = "Instance not found: %s.";
        public static final String INSTANCE_S_OPERATIONAL_LOST_MEMORY_FAILURE = "The instance %s had an operational failure due to the memory loss. It might have left trash in the cloud.";
        public static final String INSTANCE_S_ALREADY_DELETED = "Instance %s has already been deleted.";
        public static final String INSTANCE_S_ALREADY_HIBERNATED = "Instance %s has already been hibernated.";
        public static final String INSTANCE_S_ALREADY_PAUSED = "Instance %s has already been paused.";
        public static final String INSTANCE_S_ALREADY_RUNNING = "Instance %s is already running.";
        public static final String INSTANCE_S_ALREADY_STOPPED = "Instance %s has already been stopped.";
        public static final String INSTANCE_TYPE_NOT_DEFINED = "Instance type not defined.";
        public static final String INVALID_LIST_SECURITY_RULE_TYPE_S = "Invalid list security rule type: %s.";
        public static final String INVALID_NUMBER_FORMAT = "Invalid number format.";
        public static final String MAPPED_USER_S = "User mapped to: %s.";
        public static final String MAPPING_USER_OP_S = "Mapping user for operation %s on order/systemUser %s.";
        public static final String NETWORK_NOT_FOUND_S = "Network id %s was not found when trying to delete it.";
        public static final String NO_PACKET_SENDER = "PacketSender was not initialized. Trying again.";
        public static final String NO_REMOTE_COMMUNICATION_CONFIGURED = "No remote communication configured.";
        public static final String ORDER_S_CHANGED_STATE_TO_S = "Order changed %s state to %s.";
        public static final String PACKET_SENDER_INITIALIZED = "XMPP packet sender initialized.";
        public static final String PAUSING_INSTANCE_S = "Pausing instance %s.";
        public static final String RECEIVING_COMPUTE_QUOTA_REQUEST_S_S = "Get compute %s request for provider %s received.";
        public static final String RECEIVING_CREATE_REQUEST_S = "Create request for %s received.";
        public static final String RECEIVING_DELETE_REQUEST_S_S = "Delete request for %s %s received.";
        public static final String RECEIVING_GET_ALL_IMAGES_REQUEST = "Get all images request received.";
        public static final String RECEIVING_GET_ALL_REQUEST_S = "Get status request for all %s received.";
        public static final String RECEIVING_GET_CLOUDS_REQUEST = "Get request for cloud names received.";
        public static final String RECEIVING_GET_IMAGE_REQUEST_S = "Get request for image %s received.";
        public static final String RECEIVING_GET_REQUEST_S = "Get request for %s %s received.";
        public static final String RECEIVING_HIBERNATE_USER_REQUEST_S = "Hibernate request for user %s, provider %s, received.";
        public static final String RECEIVING_PAUSE_REQUEST_S = "Pause request for user %s, provider %s, received.";
        public static final String RECEIVING_RELOAD_CONFIGURATION_REQUEST = "Received reload configuration request.";
        public static final String RECEIVING_REMOTE_REQUEST_S = "Received remote request for request: %s.";
        public static final String RECEIVING_RESOURCE_S_REQUEST_S = "Get %s request for provider %s received.";
        public static final String RECEIVING_RESUME_REQUEST_S = "Resume request for user %s, provider %s, received.";
        public static final String RECEIVING_STOP_REQUEST_S = "Stop request for %s received.";
        public static final String RECEIVING_STOP_USER_REQUEST_S = "Stop request for user %s, provider %s, received.";
        public static final String RECOVERING_LIST_OF_ORDERS_S_D = "Recovering requests in %s list: %d requests recovered so far.";
        public static final String REMOVING_ORDER_IN_SELECT_STATE_S = "Order %s might have left garbage in cloud.";
        public static final String RESUMING_INSTANCE_S = "Resuming instance %s.";
        public static final String REQUESTING_GET_ALL_FROM_PROVIDER = "Requesting all images from provider.";
        public static final String REQUESTING_INSTANCE_FROM_PROVIDER = "Requesting instance from provider.";
        public static final String REQUESTING_TO_CLOUD_S_S = "Requesting to the cloud by the user %s: %s.";
        public static final String RESETTING_AS_PUBLIC_KEYS = "Resetting AS public keys.";
        public static final String RESETTING_AUTHORIZATION_PLUGIN = "Resetting authorization plugin.";
        public static final String RESETTING_AUTHORIZATION_PLUGIN_ON_REMOTE_FACADE = "Resetting authorization plugin on Remote Facade.";
        public static final String RESETTING_CLOUD_LIST_CONTROLLER = "Resetting cloud list controller.";
        public static final String RESETTING_CLOUD_LIST_CONTROLLER_ON_REMOTE_FACADE = "Resetting cloud list controller on Remote Facade.";
        public static final String RESETTING_PROPERTIES_HOLDER = "Resetting properties holder.";
        public static final String RESETTING_PROCESSORS_CONFIGURATION = "Resetting processors configuration.";
        public static final String RESOURCE_CREATION_FAILED_S = "Resource creation failed: %s";
        public static final String RESPONSE_RECEIVED_S = "Received response: %s.";
        public static final String SECOND_STEP_CREATE_AND_ATTACH_NSG_ASYNC_BEHAVIOUR = "Second step: create network security group and associate with the network interface.";
        public static final String SECOND_STEP_CREATE_VNET_ASYNC_BEHAVIOUR = "Second step on virtual network creation: network created.";
        public static final String SEEK_VIRTUAL_MACHINE_SIZE_BY_NAME_S_S = "Seek for the Virtual Machine Size by name %s at region %s.";
        public static final String SEEK_VIRTUAL_MACHINE_SIZE_NAME_S_S = "Seek for the Virtual Machine Size that fits with memory(%s) and vCpu(%s) at region %s.";
        public static final String SENDING_MSG_S = "Sending remote request for request: %s.";
        public static final String SETTING_UP_PACKET_SENDER = "Setting up XMPP packet sender.";
        public static final String SLEEP_THREAD_INTERRUPTED = "Thread is not able to sleep.";
        public static final String STARTING_THREADS = "Starting processor threads.";
        public static final String STOPPING_INSTANCE_S = "Stopping instance %s.";
        public static final String STOPPING_THREADS = "Stopping processor threads.";
        public static final String START_ASYNC_INSTANCE_CREATION_S = "Start instance (%s) creation.";
        public static final String SUCCESS = "Successfully executed operation.";
        public static final String TAKING_SNAPSHOT_OF_S = "Taking snapshot of %s.";
        public static final String THREADS_ARE_ALREADY_RUNNING = "Processor threads are already running!";
        public static final String THREADS_ARE_NOT_RUNNING = "Processor threads are not running!";
        public static final String THREAD_HAS_BEEN_INTERRUPTED = "Thread has been interrupted.";
        public static final String UNABLE_TO_ADD_EXTRA_USER_DATA_FILE_CONTENT_NULL = "Unable to add the extra user data file; content is null.";
        public static final String UNABLE_TO_ADD_EXTRA_USER_DATA_FILE_TYPE_NULL = "Unable to add the extra user data file; file type is null.";
        public static final String UNABLE_TO_CREATE_ATTACHMENT = "Unable to create an attachment from json.";
        public static final String UNABLE_TO_DECODE_URL_S = "Unable to decode url %s.";
        public static final String UNABLE_TO_DELETE_INSTANCE_S = "Unable to delete instance %s.";
        public static final String UNABLE_TO_DELETE_NETWORK_WITH_ID_S = "Unable to delete network with id %s.";
        public static final String UNABLE_TO_DELETE_SECURITY_GROUP_WITH_ID_S = "Unable to delete security group with id %s.";
        public static final String UNABLE_TO_GENERATE_JSON = "Unable to generate json.";
        public static final String UNABLE_TO_GET_ATTACHMENT_INSTANCE = "Unable to get attachment instance from json.";
        public static final String UNABLE_TO_GET_NETWORK_S = "Unable to get network information from json %s.";
        public static final String UNABLE_TO_LOAD_FLAVOURS = "Unable to load flavours.";
        public static final String UNABLE_TO_LOCATE_ORDER_S_S = "Unable to locate order %s notified by %s.";
        public static final String UNABLE_TO_MARSHALL_IN_XML = "Unable to marshall in xml.";
        public static final String UNABLE_TO_NOTIFY_REQUESTING_PROVIDER_S_S = "Unable to notify requesting provider %s for request %s.";
        public static final String UNABLE_TO_RETRIEVE_NETWORK_ID_S = "Unable to retrieve network id from json %s.";
        public static final String UNABLE_TO_RETRIEVE_ROOT_VOLUME_S = "Unable to retrieve root volume for virtual machine %s; assigning -1 to disk size.";
        public static final String UNABLE_TO_UNMARSHALL_XML_S = "Unable to unmarshall xml: %s.";
        public static final String UNDEFINED_INSTANCE_STATE_MAPPING_S_S = "State %s was not mapped to a Fogbow state by %s.";
        public static final String UNEXPECTED_ERROR = "Unexpected error.";
        public static final String UNEXPECTED_ERROR_WITH_MESSAGE_S = "Unexpected exception error: %s.";
        public static final String UNEXPECTED_JOB_STATUS = "Unexpected job status.";
        public static final String UNSPECIFIED_PROJECT_ID = "Unspecified projectId.";
        public static final String XMPP_HANDLERS_SET = "XMPP handlers set.";
    }
}

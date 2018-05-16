package org.fogbowcloud.manager.core.constants;

public class ConfigurationConstants {

	public static final String XMPP_ID_KEY = "xmpp_jid";

	// SCHEDULER PERIODS KEYS

	public static final String CLOSED_ORDERS_SLEEP_TIME_KEY = "closed_orders_scheduler_period";

	public static final String OPEN_ORDERS_SLEEP_TIME_KEY = "open_orders_sleep_time";
	public static final String SPAWNING_ORDERS_SLEEP_TIME_KEY = "spawning_orders_sleep_time";
	public static final String FULFILLED_ORDERS_SLEEP_TIME_KEY = "fulfilled_orders_sleep_time";

	// USER SSH KEY
	public static final String SSH_COMMON_USER_KEY = "ssh_common_user";

	// MANAGER KEYS
	public static final String MANAGER_SSH_PRIVATE_KEY_PATH = "manager_ssh_private_key_file_path";
	public static final String MANAGER_SSH_PUBLIC_KEY_PATH = "manager_ssh_public_key_file_path";

	// REVERSE TUNNEL KEYS
	public static final String REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY = "reverse_tunnel_private_address";
	public static final String REVERSE_TUNNEL_PUBLIC_ADDRESS_KEY = "reverse_tunnel_public_address";
	public static final String REVERSE_TUNNEL_PORT_KEY = "reverse_tunnel_port";
	public static final String REVERSE_TUNNEL_HTTP_PORT_KEY = "reverse_tunnel_http_port";


}
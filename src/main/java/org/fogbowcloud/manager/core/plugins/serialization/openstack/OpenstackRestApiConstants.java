package org.fogbowcloud.manager.core.plugins.serialization.openstack;

import com.google.gson.annotations.SerializedName;

// TODO review the same keys
public class OpenstackRestApiConstants {
	
	public static class Volume {
		public static final String VOLUME_KEY_JSON = "volume";		
		public static final String STATUS_KEY_JSON = "status";
		public static final String SIZE_KEY_JSON = "size";
		public static final String NAME_KEY_JSON = "name";
		public static final String ID_KEY_JSON = "id";
	}
	
	public static class Attachment {

		public static final String VOLUME_ATTACHMENT_KEY_JSON = "volumeAttachment";
		public static final String VOLUME_ID_KEY_JSON = "volumeId";
		public static final String SERVER_ID_KEY_JSON = "serverId";
		public static final String ID_KEY_JSON = "id";
		public static final String DEVICE_KEY_JSON = "device";
		
	}

	public static class Compute {

		public static final String SERVER_KEY_JSON = "server";
		public static final String ID_KEY_JSON = "id";
		public static final String NAME_KEY_JSON = "name";
		public static final String DISK_KEY_JSON = "disk";
		public static final String MEMORY_KEY_JSON = "ram";
		public static final String VCPUS_KEY_JSON = "vcpus";

		public static final String ADDRESSES_KEY_JSON = "addresses";
		public static final String FLAVOR_KEY_JSON = "flavor";
		public static final String STATUS_KEY_JSON = "status";
		public static final String PROVIDER_KEY_JSON = "provider";
		public static final String ADDRESS_KEY_JSON = "addr";

		public static final String IMAGE_REFERENCE_KEY_JSON = "imageRef";
		public static final String FLAVOR_REFERENCE_KEY_JSON = "flavorRef";
		public static final String USER_DATA_KEY_JSON = "user_data";
		public static final String KEY_NAME_KEY_JSON = "key_name";
		public static final String NETWORKS_KEY_JSON = "networks";
		public static final String SECURITY_GROUPS_KEY_JSON = "security_groups";
		public static final String UUID_KEY_JSON = "uuid";

	}

	public static class Network {

		public static final String NETWORK_KEY_JSON = "network";
		public static final String NAME_KEY_JSON = "name";
		public static final String TENANT_ID_KEY_JSON = "tenant_id";
		public static final String ID_KEY_JSON = "id";

		public static final String NETWORK_ID_KEY_JSON = "network_id";
		public static final String IP_VERSION_KEY_JSON = "ip_version";
		public static final String GATEWAY_IP_KEY_JSON = "gateway_ip";
		public static final String CIDR_KEY_JSON = "cidr";
		public static final String ENABLE_DHCP_KEY_JSON = "enable_dhcp";
		public static final String DNS_NAMESERVERS_KEY_JSON = "dns_nameservers";

	}
}

package org.fogbowcloud.manager.core.plugins.serialization.openstack.volume.v2;

// TODO review the same keys
public class OpenstackApiConstants {
	
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
	
}

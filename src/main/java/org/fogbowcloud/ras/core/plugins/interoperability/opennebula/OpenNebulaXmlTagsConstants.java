package org.fogbowcloud.ras.core.plugins.interoperability.opennebula;

public class OpenNebulaXmlTagsConstants {

	public static class VirtualMachine {
		
		public static final String TEMPLATE = "TEMPLATE";
		public static final String CONTEXT = "CONTEXT";
		public static final String CPU = "CPU";
		public static final String DISK = "DISK";
		public static final String GRAPHICS = "GRAPHICS";
		public static final String IMAGE_ID = "IMAGE_ID";
		public static final String LISTEN = "LISTEN";
		public static final String MEMORY = "MEMORY";
		public static final String NETWORK = "NETWORK";
		public static final String NETWORK_ID = "NETWORK_ID";
		public static final String NETWORK_INTERFACE_CONNECTED = "NIC";
		public static final String SIZE = "SIZE";
		public static final String TYPE = "TYPE";
		public static final String USERDATA = "USERDATA";
		public static final String USERDATA_ENCODING = "USERDATA_ENCODING";
	}
	
	public static class VirtualNetwork {
		
		public static final String TEMPLATE = "TEMPLATE";
		public static final String NAME = "NAME";
		public static final String DESCRIPTION = "DESCRIPTION";
		public static final String TYPE = "TYPE";
		public static final String BRIDGE = "BRIDGE";
		public static final String NETWORK_ADDRESS = "NETWORK_ADDRESS";
		public static final String NETWORK_GATEWAY = "NETWORK_GATEWAY";
		public static final String AR = "AR";
		public static final String IP = "IP";
		public static final String SIZE = "SIZE";
	}

}

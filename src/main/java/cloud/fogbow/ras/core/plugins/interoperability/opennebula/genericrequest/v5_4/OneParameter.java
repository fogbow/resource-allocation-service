package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import org.opennebula.client.host.Host;
import org.w3c.dom.Node;

public enum OneParameter {

	ACTION("action") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	APPEND("append") {
		@Override
		public Class getClassType() {
			return boolean.class;
		}

		@Override
		public Object getValue(String arg) {
			return Boolean.parseBoolean(arg);
		}
	},
	
	AR_ID("arId") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	ARGS("args") {
		@Override
		public Class getClassType() {
			return Object[].class;
		}

		@Override
		public Object getValue(String arg) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	
	AUTH("auth") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	CAPACITY_TEMPLATE("capacityTemplate") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	CLUSTER_ID("clusterId") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	DATASTORE_ID("datastoreId") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	DESCRIPTION("description") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	DISK_ID("diskId") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	DISK_TEMPLATE("diskTemplate") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	DS_ID("dsId") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	EGID("egid") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	ENABLE("enable") {
		@Override
		public Class getClassType() {
			return boolean.class;
		}

		@Override
		public Object getValue(String arg) {
			return Boolean.parseBoolean(arg);
		}
	},
	
	END_ID("endId") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	ENFORCE("enforce") {
		@Override
		public Class getClassType() {
			return boolean.class;
		}

		@Override
		public Object getValue(String arg) {
			return Boolean.parseBoolean(arg);
		}
	},
	
	EXPIRE("expire") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	EXTENDED("extended") {
		@Override
		public Class getClassType() {
			return boolean.class;
		}

		@Override
		public Object getValue(String arg) {
			return Boolean.parseBoolean(arg);
		}
	},
	
	FILTER("filter") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	GID("gid") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	GROUP_A("group_a") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	GROUP_ID("groupId") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	GROUP_M("group_m") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	GROUP_U("group_u") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	HARD("hard") {
		@Override
		public Class getClassType() {
			return boolean.class;
		}

		@Override
		public Object getValue(String arg) {
			return Boolean.parseBoolean(arg);
		}
	},
	
	HID("hid") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	HOST_ID("hostId") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	HOST_NAME("hostname") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	ID("id") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	IM("im") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	IMAGE_NAME("imageName") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	IMAGE_TYPE("imageType") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	INFO_METHOD("infoMethod") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	IP("ip") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	LIVE("live") {
		@Override
		public Class getClassType() {
			return boolean.class;
		}

		@Override
		public Object getValue(String arg) {
			return Boolean.parseBoolean(arg);
		}
	},
	
	MARKET_ID("marketId") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	NAME("name") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	NEW_CONF("new_conf") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	NEW_DOCUMENT("new_document") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	NEW_SIZE("newSize") {
		@Override
		public Class getClassType() {
			return long.class;
		}

		@Override
		public Object getValue(String arg) {
			return Long.parseLong(arg);
		}
	},
	
	NEW_TEMPLATE("new_template") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	NIC_ID("nicId") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	NIC_TEMPLATE("nicTemplate") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	NODE("node") {
		@Override
		public Class getClassType() {
			return Node.class;
		}

		@Override
		public Object getValue(String arg) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	
	NVMS("nVMs") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	OCTET("octet") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	ON_HOLD("onHold") {
		@Override
		public Class getClassType() {
			return boolean.class;
		}

		@Override
		public Object getValue(String arg) {
			return Boolean.parseBoolean(arg);
		}
	},
	
	OTHER_A("other_a") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	OTHER_M("other_m") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	OTHER_U("other_u") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	OWNER("owner") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	OWNER_A("owner_a") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	OWNER_M("owner_m") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	OWNER_U("owner_u") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	OPERATION("operation") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	PASSWORD("password") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	PERSISTENT("persistent") {
		@Override
		public Class getClassType() {
			return boolean.class;
		}

		@Override
		public Object getValue(String arg) {
			return Boolean.parseBoolean(arg);
		}
	},
	
	PUBLISH("publish") {
		@Override
		public Class getClassType() {
			return boolean.class;
		}

		@Override
		public Object getValue(String arg) {
			return Boolean.parseBoolean(arg);
		}
	},
	
	QUOTA("quota") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	QUOTA_TEMPLATE("quota_template") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	RECOVER("recover") {
		@Override
		public Class getClassType() {
			return boolean.class;
		}

		@Override
		public Object getValue(String arg) {
			return Boolean.parseBoolean(arg);
		}
	},
	
	RECURSIVE("recursive") {
		@Override
		public Class getClassType() {
			return boolean.class;
		}

		@Override
		public Object getValue(String arg) {
			return Boolean.parseBoolean(arg);
		}
	},
	
	RESOURCE("resource") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	RIGHTS("rights") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	RULE("rule") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	SNAP_ID("snapId") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	START_ID("startId") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	STATE("state") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	STATUS("status") {
		@Override
		public Class getClassType() {
			return Host.Status.class;
		}

		@Override
		public Object getValue(String arg) {
			return Host.Status.valueOf(arg);
		}
	},
	
	TARGET_DS("targetDS") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	TEMPLATE("template") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	TEMPLATE_ID("templateId") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	TOKEN("token") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	UID("uid") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	USER("user") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	USERNAME("username") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	VMM("vmm") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	VNET_ID("vnetId") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	},
	
	ZONE("zone") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			return arg;
		}
	},
	
	ZONE_ID("zoneId") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			return Integer.parseInt(arg);
		}
	};
	
	private String name;
	
	OneParameter(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public static OneParameter getValueOf(String arg) {
		for (OneParameter parameter : OneParameter.values()) {
			if (parameter.getName().equals(arg)) {
				return parameter;
			}
		}
		return null;
	}
	
	public abstract Class getClassType();
	
	public abstract Object getValue(String arg);
	
}

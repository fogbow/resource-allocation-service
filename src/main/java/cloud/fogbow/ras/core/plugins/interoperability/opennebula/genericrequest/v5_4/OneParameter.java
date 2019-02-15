package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import org.opennebula.client.Client;

public enum OneParameter {

	ACTION("action") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	
	APPEND("append") {
		@Override
		public Class getClassType() {
			return boolean.class;
		}

		@Override
		public Object getValue(String arg) {
			// TODO Auto-generated method stub
			return null;
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
	
	CAPACITY_TEMPLATE("capacityTemplate") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	
	CLIENT("client") {
		@Override
		public Class getClassType() {
			return Client.class;
		}

		@Override
		public Object getValue(String arg) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	
	DISK_ID("diskId") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	
	DISK_TEMPLATE("diskTemplate") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	
	DS_ID("dsId") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	
	ENFORCE("enforce") {
		@Override
		public Class getClassType() {
			return boolean.class;
		}

		@Override
		public Object getValue(String arg) {
			// TODO Auto-generated method stub
			return null;
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
	
	HARD("hard") {
		@Override
		public Class getClassType() {
			return boolean.class;
		}

		@Override
		public Object getValue(String arg) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	
	HOST_ID("hostId") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	
	ID("id") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	
	IMAGE_NAME("imageName") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	
	IMAGE_TYPE("imageType") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	
	LIVE("live") {
		@Override
		public Class getClassType() {
			return boolean.class;
		}

		@Override
		public Object getValue(String arg) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	
	NAME("name") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	
	NEW_SIZE("newSize") {
		@Override
		public Class getClassType() {
			return long.class;
		}

		@Override
		public Object getValue(String arg) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	
	NEW_TEMPLATE("new_template") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			// TODO Auto-generated method stub
			return null;
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
			// TODO Auto-generated method stub
			return null;
		}
	},
	
	OCTET("octet") {
		@Override
		public Class getClassType() {
			return String.class;
		}

		@Override
		public Object getValue(String arg) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	
	OPERATION("operation") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	
	SNAP_ID("snapId") {
		@Override
		public Class getClassType() {
			return int.class;
		}

		@Override
		public Object getValue(String arg) {
			// TODO Auto-generated method stub
			return null;
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

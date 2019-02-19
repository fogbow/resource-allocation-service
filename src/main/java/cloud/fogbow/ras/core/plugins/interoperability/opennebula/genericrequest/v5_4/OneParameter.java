package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

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
			return Boolean.parseBoolean(arg);
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

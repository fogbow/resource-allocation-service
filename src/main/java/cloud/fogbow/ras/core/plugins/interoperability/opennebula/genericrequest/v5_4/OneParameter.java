package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import org.opennebula.client.Client;
import org.w3c.dom.Node;

public enum OneParameter {

	CLIENT("client") {
		@Override
		public Class getClassType() {
			return Client.class;
		}
	},
	ID("id") {
		@Override
		public Class getClassType() {
			return int.class;
		}
	},
	RESOURSE("resourse") {
		@Override
		public Class getClassType() {
			return getStringClassType();
		}
	},
	RIGHTS("rights") {
		@Override
		public Class getClassType() {
			return getStringClassType();
		}
	},
	USER("user") {
		@Override
		public Class getClassType() {
			return getStringClassType();
		}
	},
	XML_ELEMENT("xmlElement") {
		@Override
		public Class getClassType() {
			return Node.class;
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
	
	private static Class getStringClassType() {
		return String.class;
	}
}

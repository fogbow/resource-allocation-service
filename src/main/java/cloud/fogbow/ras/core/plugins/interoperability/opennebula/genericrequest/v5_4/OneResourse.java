package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.Constructor;

import org.opennebula.client.Client;
import org.opennebula.client.image.Image;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.user.User;
import org.opennebula.client.vm.VirtualMachine;
import org.opennebula.client.vnet.VirtualNetwork;

public enum OneResourse {

	CLIENT("Client") {
		@Override
		public Class getClassType() {
			return Client.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {String.class, String.class};
			return OneGenericConstructor.generate(Client.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			String[] strings = {(String) objects[0], (String) objects[1]};
			return OneGenericInstance.instantiate(generateConstructor(), strings);
		}
	},
	
	IMAGE("Image"){
		@Override
		public Class getClassType() {
			return Image.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {int.class, Client.class};
			return OneGenericConstructor.generate(VirtualMachine.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			Object[] values = {(int) objects[0], (Client)objects[1]};
			return OneGenericInstance.instantiate(generateConstructor(), values);
		}
	},
	
	SECURITY_GROUP("SecurityGroup") {
		@Override
		public Class getClassType() {
			return SecurityGroup.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {int.class, Client.class};
			return OneGenericConstructor.generate(VirtualMachine.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			Object[] values = {(int) objects[0], (Client)objects[1]};
			return OneGenericInstance.instantiate(generateConstructor(), values);
		}
	},
	
	VIRTUAL_MACHINE("VirtualMachine") {
		@Override
		public Class getClassType() {
			return VirtualMachine.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {int.class, Client.class};
			return OneGenericConstructor.generate(VirtualMachine.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			Object[] values = {(int) objects[0], (Client)objects[1]};
			return OneGenericInstance.instantiate(generateConstructor(), values);
		}
	},
	
	VIRTUAL_NETWORK("VirtualNetwork") {
		@Override
		public Class getClassType() {
			return VirtualNetwork.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {int.class, Client.class};
			return OneGenericConstructor.generate(VirtualMachine.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			Object[] values = {(int) objects[0], (Client)objects[1]};
			return OneGenericInstance.instantiate(generateConstructor(), values);
		}
	},
	
	USER("User") {
		@Override
		public Class getClassType() {
			return User.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {int.class, Client.class};
			return OneGenericConstructor.generate(VirtualMachine.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			Object[] values = {(int) objects[0], (Client)objects[1]};
			return OneGenericInstance.instantiate(generateConstructor(), values);
		}
	};
	
	private String name;
	
	OneResourse(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public static OneResourse getValueOf(String arg) {
		for (OneResourse resourse : OneResourse.values()) {
			if (resourse.getName().equals(arg)) {
				return resourse;
			}
		}
		return null;
	}
	
	public abstract Class getClassType();
	
	public abstract Constructor generateConstructor();
	
	public abstract Object createInstance(Object... objects);
	
}

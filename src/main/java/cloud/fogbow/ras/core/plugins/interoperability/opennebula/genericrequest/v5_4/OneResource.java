package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opennebula.client.Client;
import org.opennebula.client.image.Image;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.user.User;
import org.opennebula.client.vm.VirtualMachine;
import org.opennebula.client.vnet.VirtualNetwork;

public enum OneResource {

	CLIENT("Client") {
		@Override
		public Class getClassType() {
			return Client.class;
		}

		@Override
		public Constructor generateConstructor() {
			List<Class> parameters = createClientParameters();
			return OneGenericConstructor.generate(Client.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			List<Object> args = addObjetcParameters(objects);
			return OneGenericInstance.instantiate(generateConstructor(), args);
		}
	},
	
	IMAGE("Image"){
		@Override
		public Class getClassType() {
			return Image.class;
		}

		@Override
		public Constructor generateConstructor() {
			List<Class> parameters = createDefaultParameters();
			return OneGenericConstructor.generate(VirtualMachine.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			List<Object> args = addObjetcParameters(objects);
			return OneGenericInstance.instantiate(generateConstructor(), args);
		}
	},
	
	SECURITY_GROUP("SecurityGroup") {
		@Override
		public Class getClassType() {
			return SecurityGroup.class;
		}

		@Override
		public Constructor generateConstructor() {
			List<Class> parameters = createDefaultParameters();
			return OneGenericConstructor.generate(VirtualMachine.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			List<Object> args = addObjetcParameters(objects);
			return OneGenericInstance.instantiate(generateConstructor(), args);
		}
	},
	
	VIRTUAL_MACHINE("VirtualMachine") {
		@Override
		public Class getClassType() {
			return VirtualMachine.class;
		}

		@Override
		public Constructor generateConstructor() {
			List<Class> parameters = createDefaultParameters();
			return OneGenericConstructor.generate(VirtualMachine.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			List<Object> args = addObjetcParameters(objects);
			return OneGenericInstance.instantiate(generateConstructor(), args);
		}
	},
	
	VIRTUAL_NETWORK("VirtualNetwork") {
		@Override
		public Class getClassType() {
			return VirtualNetwork.class;
		}

		@Override
		public Constructor generateConstructor() {
			List<Class> parameters = createDefaultParameters();
			return OneGenericConstructor.generate(VirtualMachine.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			List<Object> args = addObjetcParameters(objects);
			return OneGenericInstance.instantiate(generateConstructor(), args);
		}
	},
	
	USER("User") {
		@Override
		public Class getClassType() {
			return User.class;
		}

		@Override
		public Constructor generateConstructor() {
			List<Class> parameters = createDefaultParameters();
			return OneGenericConstructor.generate(VirtualMachine.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			List<Object> args = addObjetcParameters(objects);
			return OneGenericInstance.instantiate(generateConstructor(), args);
		}
	};
	
	private String name;
	
	OneResource(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public static OneResource getValueOf(String arg) {
		for (OneResource resourse : OneResource.values()) {
			if (resourse.getName().equals(arg)) {
				return resourse;
			}
		}
		return null;
	}
	
	public abstract Class getClassType();
	
	public abstract Constructor generateConstructor();
	
	public abstract Object createInstance(Object... objects);
	
	protected List<Object> addObjetcParameters(Object... objects) {
		List<Object> parameters = new ArrayList<>();
		for (Object object : objects) {
			parameters.add(object);
		}
		return parameters;
	}
	
	protected List<Class> createClientParameters() {
		List<Class> parameters = new ArrayList<>();
		parameters.add(String.class);
		parameters.add(String.class);
		return parameters;
	}
	
	protected List<Class> createDefaultParameters() {
		List<Class> parameters = new ArrayList<>();
		parameters.add(int.class);
		parameters.add(Client.class);
		return parameters;
	}
	
}

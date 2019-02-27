package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.opennebula.client.Client;
import org.opennebula.client.OneSystem;
import org.opennebula.client.acl.Acl;
import org.opennebula.client.acl.AclPool;
import org.opennebula.client.cluster.Cluster;
import org.opennebula.client.cluster.ClusterPool;
import org.opennebula.client.datastore.Datastore;
import org.opennebula.client.datastore.DatastorePool;
import org.opennebula.client.document.Document;
import org.opennebula.client.document.DocumentPool;
import org.opennebula.client.group.GroupPool;
import org.opennebula.client.host.Host;
import org.opennebula.client.host.HostPool;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.marketplace.MarketPlace;
import org.opennebula.client.marketplace.MarketPlacePool;
import org.opennebula.client.marketplaceapp.MarketPlaceApp;
import org.opennebula.client.marketplaceapp.MarketPlaceAppPool;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.secgroup.SecurityGroupPool;
import org.opennebula.client.template.Template;
import org.opennebula.client.template.TemplatePool;
import org.opennebula.client.user.User;
import org.opennebula.client.user.UserPool;
import org.opennebula.client.vdc.Vdc;
import org.opennebula.client.vdc.VdcPool;
import org.opennebula.client.vm.VirtualMachine;
import org.opennebula.client.vm.VirtualMachinePool;
import org.opennebula.client.vmgroup.VMGroup;
import org.opennebula.client.vmgroup.VMGroupPool;
import org.opennebula.client.vnet.VirtualNetwork;
import org.opennebula.client.vnet.VirtualNetworkPool;
import org.opennebula.client.vrouter.VirtualRouter;
import org.opennebula.client.vrouter.VirtualRouterPool;
import org.opennebula.client.zone.Zone;
import org.opennebula.client.zone.ZonePool;

public enum OneResource {

	ACL("Acl") {
		@Override
		public Class getClassType() {
			return Acl.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {};
			return OneGenericConstructor.generate(Acl.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	ACL_POOL("AclPool") {
		@Override
		public Class getClassType() {
			return AclPool.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {Client.class};
			return OneGenericConstructor.generate(AclPool.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
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
			return instantiateGenericInstance(objects);
		}
	},
	
	CLUSTER("Cluster") {
		@Override
		public Class getClassType() {
			return Cluster.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {int.class, Client.class};
			return OneGenericConstructor.generate(Cluster.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	CLUSTER_POOL("ClusterPool") {
		@Override
		public Class getClassType() {
			return ClusterPool.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {Client.class};
			return OneGenericConstructor.generate(ClusterPool.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	DATASTORE("Datastore") {
		@Override
		public Class getClassType() {
			return Datastore.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {int.class, Client.class};
			return OneGenericConstructor.generate(Datastore.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	DATASTORE_POOL("DatastorePool") {
		@Override
		public Class getClassType() {
			return DatastorePool.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {Client.class};
			return OneGenericConstructor.generate(DatastorePool.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	DOCUMENT("Document") {
		@Override
		public Class getClassType() {
			return Document.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {int.class, Client.class};
			return OneGenericConstructor.generate(Document.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	DOCUMENT_POOL("DocumentPool") {
		@Override
		public Class getClassType() {
			return DocumentPool.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {Client.class};
			return OneGenericConstructor.generate(DocumentPool.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	GROUP_POOL("GroupPool") {
		@Override
		public Class getClassType() {
			return GroupPool.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {Client.class};
			return OneGenericConstructor.generate(GroupPool.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	HOST("Host") {
		@Override
		public Class getClassType() {
			return Host.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {int.class, Client.class};
			return OneGenericConstructor.generate(Host.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	HOST_POOL("HostPool") {
		@Override
		public Class getClassType() {
			return HostPool.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {Client.class};
			return OneGenericConstructor.generate(HostPool.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
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
			return OneGenericConstructor.generate(Image.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	IMAGE_POOL("ImagePool") {
		@Override
		public Class getClassType() {
			return ImagePool.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {Client.class};
			return OneGenericConstructor.generate(ImagePool.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	MARKET_PLACE("MarketPlace") {
		@Override
		public Class getClassType() {
			return MarketPlace.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {int.class, Client.class};
			return OneGenericConstructor.generate(MarketPlace.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	MARKET_PLACE_APP("MarketPlaceApp") {
		@Override
		public Class getClassType() {
			return MarketPlaceApp.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {int.class, Client.class};
			return OneGenericConstructor.generate(MarketPlaceApp.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	MARKET_PLACE_APP_POOL("MarketPlaceAppPool") {
		@Override
		public Class getClassType() {
			return MarketPlaceAppPool.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {Client.class};
			return OneGenericConstructor.generate(MarketPlaceAppPool.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	MARKET_PLACE_POOL("MarketPlacePool") {
		@Override
		public Class getClassType() {
			return MarketPlacePool.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {Client.class};
			return OneGenericConstructor.generate(MarketPlacePool.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	ONE_SYSTEM("OneSystem") {
		@Override
		public Class getClassType() {
			return OneSystem.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {Client.class};
			return OneGenericConstructor.generate(OneSystem.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
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
			return OneGenericConstructor.generate(SecurityGroup.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	SECURITY_GROUP_POOP("SecurityGroupPool") {
		@Override
		public Class getClassType() {
			return SecurityGroupPool.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {Client.class};
			return OneGenericConstructor.generate(SecurityGroupPool.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	TEMPLATE("Template") {
		@Override
		public Class getClassType() {
			return Template.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {int.class, Client.class};
			return OneGenericConstructor.generate(MarketPlaceApp.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	TEMPLATE_POOL("Template") {
		@Override
		public Class getClassType() {
			return TemplatePool.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {Client.class};
			return OneGenericConstructor.generate(TemplatePool.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
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
			return OneGenericConstructor.generate(User.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	USER_POOL("UserPool") {
		@Override
		public Class getClassType() {
			return UserPool.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {Client.class};
			return OneGenericConstructor.generate(UserPool.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	VDC("Vdc") {
		@Override
		public Class getClassType() {
			return Vdc.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {int.class, Client.class};
			return OneGenericConstructor.generate(Vdc.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	VDC_POOL("VdcPool") {
		@Override
		public Class getClassType() {
			return VdcPool.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {Client.class};
			return OneGenericConstructor.generate(VdcPool.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
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
			return instantiateGenericInstance(objects);
		}
	},
	
	VIRTUAL_MACHINE_POOL("VirtualMachinePool") {
		@Override
		public Class getClassType() {
			return VirtualMachinePool.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {Client.class};
			return OneGenericConstructor.generate(VirtualMachinePool.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
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
			return OneGenericConstructor.generate(VirtualNetwork.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	VIRTUAL_NETWORK_POOL("VirtualNetworkPool") {
		@Override
		public Class getClassType() {
			return VirtualNetworkPool.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {Client.class};
			return OneGenericConstructor.generate(VirtualNetworkPool.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	VIRTUAL_ROUTER("VirtualRouter") {
		@Override
		public Class getClassType() {
			return VirtualRouter.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {int.class, Client.class};
			return OneGenericConstructor.generate(VirtualRouter.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	VIRTUAL_ROUTER_POOL("VirtualRouterPool") {
		@Override
		public Class getClassType() {
			return VirtualRouterPool.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {Client.class};
			return OneGenericConstructor.generate(VirtualRouterPool.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	VM_GROUP("VMGroup") {
		@Override
		public Class getClassType() {
			return VMGroup.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {int.class, Client.class};
			return OneGenericConstructor.generate(VMGroup.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	VM_GROUP_POOL("VMGroupPool") {
		@Override
		public Class getClassType() {
			return VMGroupPool.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {Client.class};
			return OneGenericConstructor.generate(VMGroupPool.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	ZONE("Zone") {
		@Override
		public Class getClassType() {
			return Zone.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {int.class, Client.class};
			return OneGenericConstructor.generate(Zone.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
		}
	},
	
	ZONE_POOL("ZonePool") {
		@Override
		public Class getClassType() {
			return ZonePool.class;
		}

		@Override
		public Constructor generateConstructor() {
			Class[] parameters = {Client.class};
			return OneGenericConstructor.generate(ZonePool.class, parameters);
		}

		@Override
		public Object createInstance(Object... objects) {
			return instantiateGenericInstance(objects);
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
	
	protected Object instantiateGenericInstance(Object... objects) {
		List<Object> args = addObjetcParameters(objects);
		return OneGenericInstance.instantiate(generateConstructor(), args);
	}
	
}

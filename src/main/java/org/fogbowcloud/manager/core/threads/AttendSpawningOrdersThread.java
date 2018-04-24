package org.fogbowcloud.manager.core.threads;

import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderRegistry;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeOrderInstance;
import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.plugins.ComputePlugin;
import org.fogbowcloud.manager.core.utils.SshClientPool;
import org.json.JSONObject;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;


public class AttendSpawningOrdersThread extends Thread {

	private static final Logger LOGGER = Logger.getLogger(AttendSpawningOrdersThread.class);
	
	// from the old class ManagerController in fogbow-manager
	private static final String DEFAULT_COMMON_SSH_USER = "fogbow";
	
	// from the old class ManagerController in fogbow-manager
	private static final long DEFAULT_INSTANCE_IP_MONITORING_PERIOD = 10000; // reference value is 10 seconds
	
	// from the old class Instance in fogbow-manager
	private static final String EXTRA_PORTS_ATT = "org.fogbowcloud.order.extra-ports";

	// from the old class Instance in fogbow-manager
	private static final String SSH_PUBLIC_ADDRESS_ATT = "org.fogbowcloud.order.ssh-public-address";
	
	// from the old class Instance in fogbow-manager
	private static final String SSH_SERVICE_NAME = "ssh";

	// from the old class Instance in fogbow-manager
	private static final String SSH_USERNAME_ATT = "org.fogbowcloud.order.ssh-username";

	// from the old class ConfigurationConstants in fogbow-manager
	private static final String SSH_COMMON_USER = "ssh_common_user";

	// from the old class ConfigurationConstants in fogbow-manager
	private static final String SSH_PRIVATE_KEY_PATH = "ssh_private_key";

	// from the old class ManagerController in fogbow-manager
	private static final int DEFAULT_MAX_IP_MONITORING_TRIES = 90; // reference value is 30 tries

	private ComputePlugin computePĺugin;
	private Long sleepTime;
	private Properties properties; // java.util.Properties ?
	private OrderRegistry orderRegistry;
	
	private SshClientPool sshClientPool = new SshClientPool();
	
	@Override
	public void run() {
		while(true) {
			try {
				Order order = this.orderRegistry.getNextOrderByState(OrderState.SPAWNING);
				if (order != null) {
					this.processSpawningOrder(order);
				} else {
					LOGGER.info("There is no Spawning Order to be processed, sleeping Attend Spawning Orders Thread...");
					Thread.sleep(this.sleepTime);
				}
			} catch (Exception e) {
				LOGGER.error("Error while trying to sleep Attend Order Thread", e);
			}
		}
	}
	
	private void processSpawningOrder(Order order) {
		synchronized (order) {
			OrderState orderState = order.getOrderState();
			if (orderState.equals(OrderState.SPAWNING)) {
				LOGGER.info("Trying to get an Instance for Order [" + order.getId() + "]");
				try {
					
					// get instance of the cloud...
					OrderInstance orderInstance = order.getOrderInstance();
					// check if type of the order is COMPUTE...
					if (order.getType().equals(OrderType.COMPUTE)) {
						// The attribute InstanceState is in ComputeOrderInstance, and not OrderInstance...
						ComputeOrderInstance computeOrderInstance = this.computePĺugin.getInstance(order.getLocalToken(), orderInstance.getId());
						
						// check if the instance is running...
						if (computeOrderInstance.getState().equals(InstanceState.ACTIVE)) {
							// try to communicate by SSH...							
							computeOrderInstance = this.setSshPublicAddress(order);
							this.waitForSSHConnectivity(computeOrderInstance);
							
							// update state after processing...						
							this.updateSpawningStateAfterProcessing(order);					

						} 
						// check if the instance is not running...
						if (computeOrderInstance.getState().equals(InstanceState.INACTIVE)) {
							// update state after processing...						
							this.updateSpawningStateAfterProcessing(order);
						}
					}					
					
				} catch (Exception e) {
					LOGGER.error("Error while trying to get an Instance for Order: " + System.lineSeparator() + order, e);
					// TODO: try to specify why it failed
					order.setOrderState(OrderState.FAILED, this.orderRegistry);
				}
			}
		}		
	}
	
	private void waitForSSHConnectivity(ComputeOrderInstance instance) {
		if (instance == null || instance.getTunnelingPorts() == null
				|| instance.getTunnelingPorts().get(SSH_PUBLIC_ADDRESS_ATT) == null) {
			return;
		}
		int retries = DEFAULT_MAX_IP_MONITORING_TRIES;
		while (retries-- > 0) {
			try {
				Command sshOutput = execOnInstance(instance.getTunnelingPorts().get(SSH_PUBLIC_ADDRESS_ATT),
						"echo HelloWorld");
				if (sshOutput.getExitStatus() == 0) {
					break;
				}
			} catch (Exception e) {
				LOGGER.debug("Check for SSH connectivity failed. " + retries + " retries left.", e);
			}
			try {
				Thread.sleep(DEFAULT_INSTANCE_IP_MONITORING_PERIOD);
			} catch (InterruptedException e) {
			}
		}
	}
	
	private Command execOnInstance(String sshPublicAddress, String cmd) throws Exception {
		SSHClient sshClient = sshClientPool.getClient(sshPublicAddress, getSSHCommonUser(),
				getManagerSSHPrivateKeyFilePath());
		Session session = sshClient.startSession();
		Command command = session.exec(cmd);
		command.join();
		return command;
	}

	private String getManagerSSHPrivateKeyFilePath() {
		String publicKeyFilePath = properties.getProperty(SSH_PRIVATE_KEY_PATH);
		if (publicKeyFilePath == null || publicKeyFilePath.isEmpty()) {
			return null;
		}
		return publicKeyFilePath;
	}

	private ComputeOrderInstance setSshPublicAddress(Order order) {
		try {
			ComputeOrderInstance instance = getInstanceSSHAddress(order);
			Map<String, String> attributes = instance.getTunnelingPorts();
			if (attributes != null) {
				String sshPublicAddress = attributes.get(SSH_PUBLIC_ADDRESS_ATT);
				if (sshPublicAddress != null) {
					return instance;
				}
			}
			Thread.sleep(DEFAULT_INSTANCE_IP_MONITORING_PERIOD);			
		} catch (Exception e) {
			LOGGER.warn("Exception while retrieving SSH public address", e);
		}
		return null;
	}

	private ComputeOrderInstance getInstanceSSHAddress(Order order) {
		OrderInstance orderInstance = new OrderInstance(order.getOrderInstance().getId());
		ComputeOrderInstance computeOrderInstance = (ComputeOrderInstance) orderInstance;
		Map<String, String> serviceAddresses = getExternalServiceAddresses(order.getId());
		if (serviceAddresses != null) {
			computeOrderInstance.addTunnelingPorts(SSH_PUBLIC_ADDRESS_ATT, serviceAddresses.get(SSH_SERVICE_NAME));
			computeOrderInstance.addTunnelingPorts(SSH_USERNAME_ATT, getSSHCommonUser());
			serviceAddresses.remove(SSH_SERVICE_NAME);
			computeOrderInstance.addTunnelingPorts(EXTRA_PORTS_ATT, new JSONObject(serviceAddresses).toString());
		}
		return computeOrderInstance;
	}

	private String getSSHCommonUser() {
		String sshCommonUser = properties.getProperty(SSH_COMMON_USER);
		return sshCommonUser == null ? DEFAULT_COMMON_SSH_USER : sshCommonUser;
	}

	private Map<String, String> getExternalServiceAddresses(String orderId) {

//		if (tokenId == null || tokenId.isEmpty()) {
//			return null;
//		}
//
//		String hostAddr = properties.getProperty(ConfigurationConstants.TOKEN_HOST_PRIVATE_ADDRESS_KEY);
//		if (hostAddr == null) {
//			return null;
//		}
//
//		HttpResponse response = null;
//		try {
//			String httpHostPort = properties.getProperty(ConfigurationConstants.TOKEN_HOST_HTTP_PORT_KEY);
//			HttpGet httpGet = new HttpGet("http://" + hostAddr + ":" + httpHostPort + "/token/" + tokenId + "/all");
//			response = reverseTunnelHttpClient.execute(httpGet);
//			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
//				JSONObject jsonPorts = new JSONObject(EntityUtils.toString(response.getEntity()));
//				if (jsonPorts.isNull(SSH_SERVICE_NAME)) {
//					return null;
//				}
//				Iterator<String> serviceIterator = jsonPorts.keys();
//				Map<String, String> servicePerAddress = new HashMap<String, String>();
//				String sshPublicHostIP = properties.getProperty(ConfigurationConstants.TOKEN_HOST_PUBLIC_ADDRESS_KEY);
//				while (serviceIterator.hasNext()) {
//					String service = (String) serviceIterator.next();
//					String port = jsonPorts.optString(service);
//					servicePerAddress.put(service, sshPublicHostIP + ":" + port);
//				}
//				return servicePerAddress;
//			}
//		} catch (Throwable e) {
//			LOGGER.warn("", e);
//		} finally {
//			if (response != null) {
//				try {
//					response.getEntity().getContent().close();
//				} catch (IOException e) {
//					// Best effort, may fail if the content was already closed.
//				}
//			}
//		}
		
		return null;		
	}	

	private void updateSpawningStateAfterProcessing(Order order) {
		// TODO Auto-generated method stub
		
		// if successfully do...
		/// remove Order from spawningOrders...
		/// set Order state to FULFILLED...
		/// insert Order in fulfilledOrders...	
		
		// if failed after a few attempts do...
		/// remove Order from spawningOrders
		/// set OrderState to FALLED...
		/// insert Order in failedOrders...
		
	}	

}

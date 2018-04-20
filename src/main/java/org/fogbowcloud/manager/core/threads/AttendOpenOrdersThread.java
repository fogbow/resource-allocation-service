package org.fogbowcloud.manager.core.models.threads;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderRegistry;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;

public class AttendOpenOrderThread extends Thread {

	private InstanceProvider localInstanceProvider;
	private InstanceProvider remoteInstanceProvider;

	private OrderRegistry orderRegistry;
	
	private String localMemberId;
	
	private static final Logger LOGGER = Logger.getLogger(AttendOpenOrderThread.class);

	public AttendOpenOrderThread(InstanceProvider localInstanceProvider, InstanceProvider remoteInstanceProvider,
			OrderRegistry orderRegistry, String localMemberId) {
		this.localInstanceProvider = localInstanceProvider;
		this.remoteInstanceProvider = remoteInstanceProvider;
		this.orderRegistry = orderRegistry;
		this.localMemberId = localMemberId;
	}

	@Override
	public void run() {
		Order order = this.orderRegistry.getNextOpenOrder();
		if (order != null) {
			LOGGER.info("Trying to get an Instance for Order [" + order.getId() + "]");

			try {
				InstanceProvider instanceProvider = null;
				if (order.isLocal(this.localMemberId)) {
					LOGGER.info("The open order [" + order.getId() + "] is local");
					
					instanceProvider = this.localInstanceProvider;
				} else if (order.isRemote(this.localMemberId)) {
					LOGGER.info("The open order [" + order.getId() + "] is remote for the member ["
							+ order.getProvidingMember() + "]");
					
					instanceProvider = this.remoteInstanceProvider;
				}
				
				order.processOpenOrder(instanceProvider);
				
				if (order.isLocal(this.localMemberId)) {
					OrderInstance orderInstance = order.getOrderInstance();
					String orderInstanceId = orderInstance.getId();
					if (!orderInstanceId.isEmpty()) {
						LOGGER.info("The open order [" + order.getId() + "] got an local instance with id ["
								+ orderInstanceId + "], setting your state to SPAWNING");
						
						order.setOrderState(OrderState.SPAWNING, this.orderRegistry);
					}
				} else if (order.isRemote(this.localMemberId)) {
					LOGGER.info("The open order [" + order.getId()
							+ "] was requested for remote member, setting your state to PENDING");
					
					order.setOrderState(OrderState.PENDING, this.orderRegistry);
				}
			} catch (Exception e) {
				LOGGER.error("Error while trying to get an Instance for Order: " + System.lineSeparator() + order, e);
				order.setOrderState(OrderState.FAILED, this.orderRegistry);
			}
		}
	}

}

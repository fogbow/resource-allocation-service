package org.fogbowcloud.manager.core.rest;

import java.util.List;

import org.fogbowcloud.manager.core.controllers.ApplicationController;
import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.token.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping(value = "compute")
public class ComputeOrdersController {

	private ApplicationController applicationController = ApplicationController.getInstance();
	private static final Logger LOGGER = LoggerFactory.getLogger(ComputeOrdersController.class);

	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<Order> createCompute(@RequestBody ComputeOrder computeOrder,
											   @RequestHeader(value = "accessId") String accessId) {
		try {
			Token federationToken = applicationController.authenticate(accessId);
			computeOrder.setFederationToken(federationToken);
			computeOrder.setOrderState(OrderState.OPEN);
			SharedOrderHolders.getInstance().getOpenOrdersList().addItem(computeOrder); // change to use OrderStateTransitioner
		} catch (Exception exception) { // change to catch failed authentication exception
			String message = "It was not possible to create new ComputeOrder. " + exception.getMessage();
			LOGGER.error(message);
			return new ResponseEntity<Order>(HttpStatus.UNAUTHORIZED);
		}
		return new ResponseEntity<Order>(HttpStatus.OK);
	}

	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<List<ComputeOrder>> getAllCompute() throws TokenCreationException, UnauthorizedException {
		return new ResponseEntity<List<ComputeOrder>>(HttpStatus.OK);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public ResponseEntity<ComputeOrder> getComputeById(@PathVariable String id) {
		return new ResponseEntity<ComputeOrder>(HttpStatus.OK);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public ResponseEntity<Boolean> deleteCompute(@PathVariable String id) {
		return new ResponseEntity<Boolean>(HttpStatus.OK);
	}
}

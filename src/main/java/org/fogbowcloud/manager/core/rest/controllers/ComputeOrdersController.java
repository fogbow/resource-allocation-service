package org.fogbowcloud.manager.core.rest.controllers;

import java.util.List;

import org.fogbowcloud.manager.core.controllers.ApplicationController;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping(value = "compute")
public class ComputeOrdersController {

	private ApplicationController applicationController = ApplicationController.getInstance();

	private final String ACCESS_ID_HEADER_KEY = "accessId";
	private final String LOCAL_TOKEN_ID_HEADER_KEY = "localTokenId";

	private final Logger LOGGER = LoggerFactory.getLogger(ComputeOrdersController.class);

	// ExceptionHandlerController handles the possible problems in request
	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<Order> createCompute(@RequestBody ComputeOrder computeOrder,
			@RequestHeader(ACCESS_ID_HEADER_KEY) String accessId,
			@RequestHeader(LOCAL_TOKEN_ID_HEADER_KEY) String localTokenId)
			throws UnauthorizedException, OrderManagementException {
		LOGGER.info("New compute order request received.");
		this.applicationController.newOrderRequest(computeOrder, accessId, localTokenId);
		return new ResponseEntity<Order>(HttpStatus.CREATED);
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
	public ResponseEntity<Boolean> deleteCompute(@PathVariable String id,
			@RequestHeader(value = "accessId") String accessId) throws UnauthorizedException, OrderManagementException {
		this.applicationController.deleteOrder(id, accessId, OrderType.COMPUTE);
		return new ResponseEntity<Boolean>(HttpStatus.OK);
	}

}

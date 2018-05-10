package org.fogbowcloud.manager.core.rest.controllers;

import java.util.List;

import org.fogbowcloud.manager.core.controllers.ApplicationController;
import org.fogbowcloud.manager.core.exceptions.OrdersServiceException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping(value = "compute")
public class ComputeOrdersController {

	//@Autowired
	//private OrdersService computeOrdersService;
	private ApplicationController applicationController = ApplicationController.getInstance();

	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<Order> createCompute(@RequestBody ComputeOrder computeOrder,
		@RequestHeader(value = "accessId") String accessId, @RequestHeader(value = "localTokenId") String localTokenId)
			throws UnauthorizedException, OrdersServiceException {
		applicationController.createOrder(computeOrder, accessId, localTokenId);
		return new ResponseEntity<Order>(HttpStatus.CREATED);
	}

	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<List<ComputeOrder>> getAllCompute() {
		return new ResponseEntity<List<ComputeOrder>>(HttpStatus.OK);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public ResponseEntity<ComputeOrder> getComputeById(@PathVariable Long id) {
		return new ResponseEntity<ComputeOrder>(HttpStatus.OK);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public ResponseEntity<Boolean> deleteCompute(@PathVariable Long id) {
		return new ResponseEntity<Boolean>(HttpStatus.OK);
	}

}
